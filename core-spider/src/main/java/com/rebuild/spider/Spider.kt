package com.rebuild.spider

import com.rebuild.quickjs.QuickJSEngine

/**
 * 纯自研 TVBox Spider 协议（13 接口）。
 * 协议字段与 TVBox/某商业闭源影音App/OK影视 四壳通用源标准对齐（见 2-TVBox源协议规范.md），
 * 但代码完全自研，不复制某商业闭源影音App任何 .so / 资源 / 端点。
 *
 * 返回值统一为 JSON 字符串（QuickJS 侧约定），壳侧用 Gson 解析成 data class。
 */
interface Spider {

    /** 1. 首页分类导航 → {"class":[{"type","name","style"?}...]} */
    fun home(): String

    /** 2. 首页推荐 → {"list":[vod],"page":n,"pages":n,"total":n} */
    fun homeVod(p: HomeVodParam): String

    /** 3. 分类列表 → 同 homeVod */
    fun category(type: String, page: Int, filter: Filter, ext: Map<String, String>): String

    /** 4. 详情+分集 → {"vod":{...},"player":"..."} */
    fun detail(id: String): String

    /** 5. 搜索 → 同 category */
    fun search(kw: String, page: Int, filter: Filter): String

    /** 6. 播放地址（逐集）→ 可播 URL / m3u8 */
    fun play(id: String, flag: String, ep: String, alias: String): String

    /** 7. 流代理/鉴权头 */
    fun proxy(url: String): ProxyResp

    /** 8. 筛选字典 */
    fun classify(): String

    /** 9. 自检 */
    fun test(): String

    /** 10. 内置 HTTP GET（暴露给 JS） */
    fun get(url: String): String

    /** 11. 内置 HTTP POST（暴露给 JS） */
    fun post(url: String, body: String): String

    /** 12. 内置工具 JSON→object */
    fun json2Object(str: String): String

    /** 13. 底层通用请求（暴露给 JS，headers/url/options 全参） */
    fun request(headers: String, url: String, options: String): String

    /** 解析播放地址里的 mac_url（# 分集、$ 分"集名$URL"、$$ 跳过占位） */
    fun parseMacUrl(macUrl: String): List<Episode>

    fun destroy()
}

data class HomeVodParam(val types: List<String> = emptyList(), val filter: Filter = Filter(), val page: Int = 1)
data class Filter(val kw: String? = null, val ext: Map<String, String> = emptyMap())
data class ProxyResp(val url: String, val headers: Map<String, String> = emptyMap())
data class Episode(val name: String, val url: String)
