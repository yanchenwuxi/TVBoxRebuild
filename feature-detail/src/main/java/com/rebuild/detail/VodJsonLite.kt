package com.rebuild.detail

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/** 详情 VO（从 Spider.detail JSON 映射） */
data class DetailVod(
    val name: String, val content: String?, val actor: String?, val score: String?,
    val pic: String?, val episodes: List<EpisodeLite>
)

/**
 * 纯自研 detail JSON 解析（Spider.detail → DetailVod）。
 * 字段对齐 TVBox 通用源：{"vod":{vod_name,vod_content,vod_actor,vod_score,vod_pic,vod_play_url}}
 */
object VodJsonLite {
    fun parseDetail(json: String): DetailVod {
        val root = JsonParser.parseString(json).asJsonObject
        val vod = root.getAsJsonObject("vod") ?: root
        val name = vod.get("vod_name")?.asString ?: ""
        val mac = vod.get("vod_play_url")?.asString ?: ""
        return DetailVod(
            name = name,
            content = vod.get("vod_content")?.asString,
            actor = vod.get("vod_actor")?.asString,
            score = vod.get("vod_score")?.asString,
            pic = vod.get("vod_pic")?.asString,
            episodes = parseEpisodes(mac)
        )
    }

    /** mac_url：# 分集 / $ 分"集名$URL" / 跳空占位（与 core-spider parseMacUrl 同逻辑） */
    private fun parseEpisodes(macUrl: String): List<EpisodeLite> =
        macUrl.split("#")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { ep ->
                val i = ep.indexOf('$')
                if (i < 0) null else EpisodeLite(name = ep.substring(0, i), url = ep.substring(i + 1))
            }
}
