package com.rebuild.spider

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.rebuild.quickjs.QuickJSEngine
import okhttp3.OkHttpClient

/**
 * 纯自研 QuickJS Spider 引擎。
 * - 用 QuickJS(MIT) 运行源 JS；
 * - 把 Spider 的 4 个内置方法（get/post/json2object/request）暴露为 JS 全局函数；
 * - 加载源 JS 后调用其 main()，拿到 13 接口中对应那一个的 JSON 返回。
 *
 * 不依赖某商业闭源影音App任何 native。QuickJS 仅做 JS 解释执行，
 * 网络/解析/序列化全部在 Kotlin 侧完成（可审计、可替换）。
 */
class QuickJSSpider(
    private val engine: QuickJSEngine,
    private val http: OkHttpClient = OkHttpClient(),
    private val gson: Gson = Gson(),
    /** 可选：Kotlin 侧实现的 4 内置方法（QuickJSSpiderApi） */
    val quickjsApi: QuickJSSpiderApi? = null
) : Spider {

    private var jsCode: String = ""

    fun init(src: String) { jsCode = src }

    /**
     * 调用 13 接口中的某一个。
     * 实现：在引擎里先 eval(jsCode) 加载源，再 call(fn, argsJson)。
     * Mini 模式下 get() 返回空串属预期；Native 模式走 C 层。
     */
    override fun home(): String = call("home")
    override fun homeVod(p: HomeVodParam): String =
        call("homeVod", gson.toJson(mapOf("types" to p.types, "page" to p.page, "filter" to p.filter.kw ?: "")))
    override fun category(type: String, page: Int, filter: Filter, ext: Map<String, String>): String =
        call("category", gson.toJson(mapOf("type" to type, "page" to page, "filter" to filter.kw ?: "", "extend" to ext)))
    override fun detail(id: String): String = call("detail", gson.toJson(mapOf("id" to id)))
    override fun search(kw: String, page: Int, filter: Filter): String =
        call("search", gson.toJson(mapOf("kw" to kw, "page" to page)))
    override fun play(id: String, flag: String, ep: String, alias: String): String =
        call("play", gson.toJson(mapOf("id" to id, "flag" to flag, "ep" to ep, "alias" to alias)))
    override fun proxy(url: String): ProxyResp {
        val raw = call("proxy", gson.toJson(mapOf("url" to url)))
        val o = com.google.gson.JsonParser.parseString(raw).asJsonObject
        return ProxyResp(o.get("url").asString, headersOf(o))
    }
    override fun classify(): String = call("classify")
    override fun test(): String = call("test")

    // ---- 内置方法（暴露给 JS，也可被 Kotlin 侧直接调用）----
    override fun get(url: String): String =
        http.newCall(okhttp3.Request.Builder().url(url).build()).execute().use { it.body?.string() ?: "" }
    override fun post(url: String, body: String): String =
        http.newCall(
            okhttp3.Request.Builder().url(url)
                .post(okhttp3.RequestBody.create(body, okhttp3.MediaType.parse("application/json")))
                .build()
        ).execute().use { it.body?.string() ?: "" }
    override fun json2Object(str: String): String = str   // QuickJS 侧原生支持
    override fun request(headers: String, url: String, options: String): String =
        get(url) // MVP 简化：GET；完整实现解析 options.method/headers

    // ---- mac_url 解析（# 分集 / $ 分"集名$URL" / 跳过空 $$ 占位）----
    override fun parseMacUrl(macUrl: String): List<Episode> {
        // 多线路以 , 或独立字段分隔；这里处理单线路，多线路在 play() 里拆
        return macUrl.split("#")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { ep ->
                val i = ep.indexOf('$')
                if (i < 0) null else Episode(name = ep.substring(0, i), url = ep.substring(i + 1))
            }
    }

    override fun destroy() { /* 释放 QuickJS context */ }

    // ---------- 内部 ----------
    private fun call(fn: String, vararg args: String): String {
        // 先加载源 JS（engine 内部会 eval jsCode 暴露 4 内置），再调对应 13 接口函数
        val withArgs = if (args.isNotEmpty()) args.joinToString(", ") else ""
        val code = "$jsCode\n$fn($withArgs)"
        // 把结果写进全局 __result__，engine 取回
        return engine.runSpiderAndCapture(code) ?: "{}"
    }
    private fun headersOf(o: JsonObject): Map<String, String> =
        o.getAsJsonObject("headers")?.entrySet()?.associate { it.key to it.value.asString } ?: emptyMap()
}
