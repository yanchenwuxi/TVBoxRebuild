package com.rebuild.source

import com.google.gson.Gson

/**
 * M1 · 源管理数据模型。
 * 与某商业闭源影音App脱壳 dex 的 SourceManager 行为对齐（多仓/导入导出/健康检测），代码自研。
 */

/** 单个视频源（TVBox 通用源 JSON 配置的一个条目） */
data class VideoSource(
    val name: String,
    /** 源类型：CONFIG(无JS, 走 RuleBasedParser) / JS(QuickJS spider) / API(内置) */
    val type: SourceType,
    /** 源配置 JSON 文本（CONFIG 型）或 JS 代码（JS 型） */
    val content: String,
    /** 是否启用 */
    var enabled: Boolean = true,
    /** 健康状态（最近一次检测） */
    var health: Health = Health.UNKNOWN,
    /** 上次健康检测时间 */
    var lastCheckedMs: Long = 0L
)

enum class SourceType { CONFIG, JS, API }

enum class Health(val label: String) {
    OK("可用"), SLOW("慢"), ERR("错误"), UNKNOWN("未知");
    override fun toString() = label
}

/** 一个"仓"（仓库）= 一组源 */
data class SourceRepo(
    val name: String,
    val url: String,
    val sources: List<VideoSource> = emptyList()
)

data class SourceConfig(
    /** 已选用的源 */
    val selected: List<String> = emptyList(),
    /** 多仓列表 */
    val repos: List<SourceRepo> = emptyList()
)

/**
 * 源管理器：导入/导出（JSON），多仓聚合，健康检测（轻量 GET 首页类）。
 */
class SourceManager(private val gson: Gson = Gson()) {

    /** 从单个 JSON 字符串导入（TVBox 通用源格式：{"name":...,"type":...,"content":...}） */
    fun importSource(json: String): VideoSource {
        val o = gson.fromJson(json, com.google.gson.JsonObject::class.java)
        return VideoSource(
            name = o.get("name")?.asString ?: "未命名",
            type = when (o.get("type")?.asString?.uppercase()) {
                "JS" -> SourceType.JS
                "API" -> SourceType.API
                else -> SourceType.CONFIG
            },
            content = o.get("content")?.asString ?: ""
        )
    }

    /** 导入一个仓（JSON 数组或对象列表） */
    fun importRepo(name: String, url: String, json: String): SourceRepo {
        val arr = gson.fromJson(json, Array<com.google.gson.JsonObject>::class.java) ?: emptyArray()
        return SourceRepo(name, url, arr.map { importSource(it.toString()) })
    }

    fun exportSources(sources: List<VideoSource>): String =
        gson.toJson(sources.map {
            mapOf("name" to it.name, "type" to it.type.name, "content" to it.content,
                  "enabled" to it.enabled, "health" to it.health.name)
        })

    /** 轻量健康检测：CONFIG 型取 home，JS 型走 QuickJS（由调用方传入执行器） */
    fun probeHealth(
        source: VideoSource,
        runner: (VideoSource) -> String,   // 返回执行结果 JSON（如 home()）
        timeoutMs: Int = 5000
    ): Health {
        source.lastCheckedMs = System.currentTimeMillis()
        return try {
            val out = runner(source)
            source.health = if (out.isNotBlank() && out.contains("\"class\"")) Health.OK else Health.SLOW
        } catch (e: Exception) {
            source.health = Health.ERR
        }
        source.health
    }

    /** 聚合多仓 → 去重（按 name）后的可用源列表 */
    fun allEnabled(repos: List<SourceRepo>): List<VideoSource> =
        repos.flatMap { it.sources }.filter { it.enabled }.distinctBy { it.name }
}
