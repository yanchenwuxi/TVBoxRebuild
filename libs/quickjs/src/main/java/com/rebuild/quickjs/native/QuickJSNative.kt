package com.rebuild.quickjs.native

import java.nio.ByteBuffer

/**
 * 真实 QuickJS C 层 JNI 绑定（Bellard quickjs-ng v0.17.0，MIT）。
 * 对应 C 侧 quickjs_rebuild.c 的 4 个 JNI 导出。
 *
 * 接法：
 *  1) AGP/NDK 编出 libquickjs_rebuild.so（见 fetch-and-build.sh / build-native.sh / CMakeLists）
 *  2) Android 进程启动时 loadLibraryOnce() 加载
 *  3) Kotlin 侧 newContext()/eval()/call() 走 native，替换 M0/M1 的 Mini 替身
 *
 * 合规：不复用某商业闭源影音App libquickjs-android-wrapper.so；用 Bellard quickjs-ng（MIT）。
 */
object QuickJSNative {

    @Volatile private var loaded = false
    @Volatile private var loadFailed = false

    /** 加载一次（App 启动 / 首个 Spider 调用时触发） */
    fun loadLibraryOnce() {
        if (loaded || loadFailed) return
        synchronized(this) {
            if (loaded || loadFailed) return
            try {
                System.loadLibrary("quickjs_rebuild")
                loaded = true
            } catch (t: Throwable) {
                loadFailed = true
                // 开发期未集成 C 层 → 上层回退 Mini；线上必加载
                System.err.println("[QuickJSNative] libquickjs_rebuild 未加载，回退 Mini：$t")
            }
        }
    }

    val isNativeReady: Boolean get() = loaded && !loadFailed

    // ---- C 层导出（符号名对应 quickjs_rebuild.c 的 Java_com_rebuild_...）----

    /** 新建 JS context，返回 C 侧句柄（int） */
    @JvmStatic external fun nativeNewContext(): Int

    /** 释放 context */
    @JvmStatic external fun nativeFreeContext(handle: Int)

    /** 在 context 里 eval 一段 JS。返回 0=成功，非 0=异常。 */
    @JvmStatic external fun nativeEval(handle: Int, code: String, filename: String): Int

    /**
     * 调用 JS 全局函数。argsJson 是 JSON 数组字符串。
     * 返回：C 侧把 JS 返回的字符串写回 outBuf（UTF-8），返回值=字节数（-1=非字符串/undefined）。
     */
    @JvmStatic external fun nativeCall(
        handle: Int, fn: String, argsJson: String,
        outBuf: ByteBuffer, outCap: Int
    ): Int
}
