package com.rebuild.parse

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.rebuild.home.SourceCategory
import com.rebuild.home.VodSummary

/**
 * 纯自研 JSON→UI 模型映射（Spider 13 接口返回的 JSON → Compose 用的 data class）。
 * 字段对齐 TVBox 通用源 VOD 约定（见 2-TVBox源协议规范.md），代码自研。
 */
object VodJson {
    private val gson = Gson()

    /** home() → {"class":[{type,name,style}]} → 分类 Tab */
    fun parseClass(homeJson: String): List<SourceCategory> {
        val root = JsonParser.parseString(homeJson).asJsonObject
        val arr = root.getAsJsonArray("class") ?: root.getAsJsonArray("classes") ?: return emptyList()
        return arr.mapNotNull { it.asJsonObject?.let { o ->
            SourceCategory(
                type = o.get("type")?.asInt ?: 0,
                name = o.get("name")?.asString ?: "",
                style = o.get("style")?.asInt ?: 0
            )
        } }
    }

    /** category()/search() → {"list":[vod]} 或 {"list":[...],"pages":n} → (片单, 总页数) */
    fun parseList(json: String): Pair<List<VodSummary>, Int> {
        val root = JsonParser.parseString(json).asJsonObject
        val arr = root.getAsJsonArray("list") ?: return (emptyList(), 1)
        val vods = arr.mapNotNull { it.asJsonObject?.toVod() }
        val pages = root.get("pages")?.asInt ?: 1
        return vods to pages
    }

    private fun JsonObject.toVod(): VodSummary? {
        val id = this.get("vod_id")?.asString ?: return null
        return VodSummary(
            vodId = id,
            name = this.get("vod_name")?.asString ?: "",
            year = this.get("vod_year")?.asString,
            area = this.get("vod_area")?.asString,
            cls = this.get("vod_cls")?.asString,
            remarks = this.get("vod_remarks")?.asString,
            pic = this.get("vod_pic")?.asString
        )
    }

    /** detail() → {"vod":{...}} 的 vod_play_url 拆成分集（复用 Spider.parseMacUrl 逻辑） */
    fun parseEpisodes(macUrl: String): List<Episode> {
        return macUrl.split("#")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { ep ->
                val i = ep.indexOf('$')
                if (i < 0) null else Episode(name = ep.substring(0, i), url = ep.substring(i + 1))
            }
    }

    data class Episode(val name: String, val url: String)
}
