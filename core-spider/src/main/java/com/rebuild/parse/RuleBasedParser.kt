package com.rebuild.parse

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * 纯自研内置解析器（无 JS 源用）：JSONPath(子集) + 正则 + 简单 CSS-like 选择器。
 * 覆盖 80% "配置型" 站点；复杂站点才挂 QuickJS spider（见 2-协议规范 第 4.4 节）。
 *
 * 设计：声明式 Rule = { 字段: 提取器 }，提取器三选一：
 *   - JsonPath("$.data.list")
 *   - Regex("(?s)<li><a[^>]+href=\"([^\"]+)\"[^>]+title=\"([^\"]+)\"")
 *   - CssSelector("#data_list li a[data-src]")
 */
sealed class Rule {
    data class JsonPath(val path: String) : Rule()
    data class Regex(val group: Int, val pattern: String, val flags: String = "") : Rule()
    data class CssSelector(val selector: String) : Rule()
}

class RuleBasedParser(private val http: (url: String) -> String, private val gson: com.google.gson.Gson = com.google.gson.Gson()) {

    fun parseHome(rules: Map<String, List<Rule>>): JsonObject =
        rules.entries.associate { (k, v) -> k to v.firstOrNull()?.let { render(it) }?.toString() ?: "" }
            .let { JsonObject().apply { it.forEach { (k, v) -> add(k, v) } } }

    fun parseList(rules: Map<String, Rule>, body: String): List<JsonObject> {
        val list = when (val r = rules.values.first()) {
            is Rule.JsonPath -> jsonPath(body, r.path)
            is Rule.Regex -> regexAll(body, r.pattern, r.flags)
            is Rule.CssSelector -> cssSelect(body, r.selector)
        }
        // 后续按其余字段规则二次提取
        return list.map { normalize(it) }
    }

    private fun normalize(el: JsonElement): JsonObject = when (el) {
        is JsonObject -> el
        else -> JsonObject().apply { el.asString.let { add("raw", it) } }
    }

    private fun jsonPath(body: String, path: String): List<JsonElement> {
        // MVP：支持 "$.a.b[0].c" 极简 JSONPath
        val root = JsonParser.parseString(body)
        val parts = path.removePrefix("$").split(".")
        var cur: JsonElement = root
        for (p in parts) {
            if (cur !is JsonObject) return emptyList()
            cur = cur.getAsJsonObject(p) ?: return emptyList()
        }
        return if (cur is JsonArray) cur.asArray.toList() else listOf(cur)
    }

    private fun regexAll(body: String, pattern: String, flags: String): List<JsonElement> {
        val re = (if (flags.isEmpty()) "(${pattern})" else "(${pattern})")
            .let { java.util.regex.Pattern.compile(it, java.util.regex.Pattern.DOTALL) }
        val m = re.matcher(body)
        val out = mutableListOf<JsonElement>()
        while (m.find()) {
            for (g in 1..m.groupCount()) out += com.google.gson.JsonPrimitive(m.group(g))
        }
        return out
    }

    private fun cssSelect(body: String, selector: String): List<JsonElement> {
        // MVP：占位。完整实现需 DOM 解析（Jsoup 依赖）。
        // 这里先收集所有 <a> 元素文本/链接，供上层选择器命中。
        val jsoup = org.jsoup.Jsoup.parse(body)
        val els = jsoup.select(selector)
        return els.map { com.google.gson.JsonPrimitive(it.text()) }
    }
}
