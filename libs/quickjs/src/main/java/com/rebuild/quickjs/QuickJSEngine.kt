package com.rebuild.quickjs

/**
 * QuickJS Android 封装（MIT 库的 Kotlin 包装层，非 C 源码——C 层走官方 quickjs-android 或 libquickjs）。
 * 纯自研：不复用某商业闭源影音App libquickjs-android-wrapper.so，而是接官方 QuickJS 绑定。
 */
interface QuickJSLibrary {
    fun newContext(): QuickJSContext
}

interface QuickJSContext {
    fun eval(code: String)
    fun invoke(fn: String, vararg args: String): String?
    /** 取 JS 全局某变量的字符串值（如 __result__） */
    fun capture(name: String): String?
    fun close()
}

class QuickJSEngine(private val lib: QuickJSLibrary) {

    /** 在 context 里执行 JS，并暴露 Kotlin 内置方法（get/post/json2object/request）为 JS 全局函数 */
    fun runSpider(jsSrc: String, api: QuickJSSpiderApi): String {
        val ctx = lib.newContext()
        try {
            // 暴露 4 个内置方法到 JS 全局（对应 Spider 协议第 10/11/12/13 接口）
            ctx.eval("global.get = function(u){ return __builtin_get(u); };")
            ctx.eval("global.post = function(u,b){ return __builtin_post(u,b); };")
            ctx.eval("global.json2object = function(s){ return __builtin_json2object(s); };")
            ctx.eval("global.request = function(h,u,o){ return __builtin_request(h,u,o); };")
            // 注册 4 个 native 回调 → Kotlin 侧
            api.bindGet(ctx)
            api.bindPost(ctx)
            api.bindJson(ctx)
            api.bindRequest(ctx)
            // 加载源 JS
            ctx.eval(jsSrc)
            // 调用源主入口
            return ctx.invoke("main") ?: "{}"
        } finally {
            ctx.close()
        }
    }

    /** 供 QuickJSSpider 调 13 接口中的某一个（已 init 后） */
    fun invoke(fn: String, vararg args: String): String? {
        val ctx = lib.newContext()
        try {
            // 这里简化：真实实现保留 ctx 状态（init 一次，多次 call）
            // 开发期 Mini 模式下返回空串属预期
            return ctx.invoke(fn, *args)
        } finally {
            ctx.close()
        }
    }

    /**
     * 执行"源 JS + 调某 13 接口 + 捕获 __result__"。
     * 这是 QuickJSSpider.call() 真正用到的入口。
     *  - Native 模式：C 层跑完，从 context 的 __result__ 取回
     *  - Mini 模式：MiniJsHost 把 `__result__ = "..."` 捕获后返回
     */
    fun runSpiderAndCapture(code: String): String? {
        val ctx = lib.newContext()
        try {
            // 暴露 4 内置（幂等，重声明无害）
            ctx.eval("global.get = global.get || function(u){ return ''; };")
            ctx.eval("global.post = global.post || function(u,b){ return ''; };")
            ctx.eval("global.json2object = global.json2object || function(s){ return s; };")
            ctx.eval("global.request = global.request || function(h,u,o){ return ''; };")
            // 跑：源 JS + 调 13 接口之一（code 形如 "<jsCode>\nhome()"）
            ctx.eval(code)
            // 捕获 __result__
            return ctx.capture("__result__")
        } finally {
            ctx.close()
        }
    }
}

/** Kotlin 侧实现的 4 个内置方法绑定到 JS 全局的 __builtin_* 钩子 */
interface QuickJSSpiderApi {
    fun bindGet(ctx: QuickJSContext)
    fun bindPost(ctx: QuickJSContext)
    fun bindJson(ctx: QuickJSContext)
    fun bindRequest(ctx: QuickJSContext)
}
