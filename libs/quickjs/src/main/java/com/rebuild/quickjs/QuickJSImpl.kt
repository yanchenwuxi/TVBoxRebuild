package com.rebuild.quickjs

import com.rebuild.quickjs.native.QuickJSNative
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap

/**
 * QuickJS 绑定实现（把抽象接口落到真实可运行的 JS 运行时）。
 *
 * 两种后端（同一 QuickJSContextImpl，运行时自动选择）：
 *  - NATIVE：libquickjs_rebuild.so 已加载 → 走 QuickJSNative（真实 JS 执行）
 *  - MINI  ：未集成 C 层 → 内置极简解释（开发期替身，仅跑内联数据源）
 *
 * 合规：不复用某商业闭源影音App libquickjs-android-wrapper.so；接 Bellard quickjs-ng（MIT）。
 */

/** 真实 QuickJS 绑定（加载 C 层后自动走 native，否则 Mini 替身） */
class RealQuickJSLibrary : QuickJSLibrary {
    override fun newContext(): QuickJSContext {
        QuickJSNative.loadLibraryOnce()   // 触发 System.loadLibrary("quickjs_rebuild")
        return QuickJSContextImpl()
    }
}

class QuickJSContextImpl(
    /** Kotlin 侧注入的 4 个内置方法（Native 模式下由 C 侧调回） */
    val builtin: QuickJSSpiderApi? = null
) : QuickJSContext {

    /** NATIVE 后端句柄；-1 = Mini 模式 */
    private var nativeHandle: Int = -1

    /** MINI 后端（开发期替身） */
    private val vars = LinkedHashMap<String, String>()

    private val directBuffer: ByteBuffer =
        ByteBuffer.allocateDirect(1 shl 20).order(ByteOrder.nativeOrder())

    init {
        if (QuickJSNative.isNativeReady) {
            try {
                nativeHandle = QuickJSNative.nativeNewContext()
                builtin?.let { QuickJSCallback.register(it, nativeHandle) }
            } catch (t: Throwable) {
                nativeHandle = -1
            }
        }
    }

    private val isNative: Boolean get() = nativeHandle >= 0

    override fun eval(code: String) {
        if (isNative) {
            QuickJSNative.nativeEval(nativeHandle, code, "spider.js")
        } else {
            // Mini：识别 `__result__ = "...";` 形式
            Regex("__result__\\s*=\\s*\"([^\"]*)\"").find(code)?.let { vars["__result__"] = it.groupValues[1] }
        }
    }

    override fun invoke(fn: String, vararg args: String): String? {
        return if (isNative) {
            directBuffer.clear()
            val n = QuickJSNative.nativeCall(nativeHandle, fn, argsJson(args), directBuffer, directBuffer.capacity())
            if (n > 0) directBuffer.asCharBuffer().limit(n / 2).toString() else null
        } else {
            vars[fn] ?: vars["__result__"] ?: "{}"
        }
    }

    /** 取 JS 全局某变量（Mini 模式从 vars；Native 模式 C 层 JS_GetProperty + JS_ToString） */
    override fun capture(name: String): String? {
        return if (isNative) {
            // Native：通过 eval 一段 "typeof x==='string'?x:''" 取回（占位；真实走 C 层 API）
            directBuffer.clear()
            val probe = "(${name}!==undefined && typeof ${name}==='string')?${name}:'__undefined__'"
            QuickJSNative.nativeEval(nativeHandle, probe, "<capture>")
            // MVP 简化：Native capture 回退到 Mini 变量表（接 C 层后补全）
            vars[name]
        } else {
            vars[name]
        }
    }

    override fun close() {
        if (isNative) QuickJSNative.nativeFreeContext(nativeHandle)
        nativeHandle = -1
        vars.clear()
    }

    private fun argsJson(args: Array<String>): String =
        args.joinToString(", ", "[", "]") { "\"$it\"" }
}

/** 内置方法回调：记录 Kotlin 4 方法 ↔ C 句柄的绑定（C 侧查表回调） */
object QuickJSCallback {
    private val table = ConcurrentHashMap<Int, QuickJSSpiderApi>()
    fun register(api: QuickJSSpiderApi, handle: Int) { table[handle] = api }
    fun get(handle: Int): QuickJSSpiderApi? = table[handle]
    fun unregister(handle: Int) { table.remove(handle) }
}

/** 内置方法默认实现（MVP：网络走 OkHttp，json2object 透传） */
class DefaultQuickJSSpiderApi(private val http: okhttp3.OkHttpClient = okhttp3.OkHttpClient())
    : QuickJSSpiderApi {

    override fun bindGet(ctx: QuickJSContext) { /* Native 模式由 C 侧注册，无需操作 */ }
    override fun bindPost(ctx: QuickJSContext) { /* 同上 */ }
    override fun bindJson(ctx: QuickJSContext) { /* 同上 */ }
    override fun bindRequest(ctx: QuickJSContext) { /* 同上 */ }

    /** Kotlin 侧直接可用的 4 方法（供 QuickJSSpider.get/post/... 调用） */
    fun get(url: String): String =
        http.newCall(okhttp3.Request.Builder().url(url).build()).execute()
            .use { it.body?.string() ?: "" }
    fun post(url: String, body: String): String =
        http.newCall(okhttp3.Request.Builder().url(url)
            .post(okhttp3.RequestBody.create(body, okhttp3.MediaType.parse("application/json")))
            .build()).execute().use { it.body?.string() ?: "" }
    fun json2object(s: String): String = s
    fun request(headers: String, url: String, options: String): String = get(url)
}
