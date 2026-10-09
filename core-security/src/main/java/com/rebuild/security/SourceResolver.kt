package com.rebuild.security

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * M3 · 源白名单 + 播放 URL 不下发明文（替代某商业闭源影音App写死的 <competitor-endpoint> 端点）。
 * 客户端只存 src_id + 类型；要播放时向服务端 /resolve 换取加密 + 时效 token 的 URL。
 */
class SourceResolver(
    private val client: OkHttpClient = OkHttpClient(),
    private val gson: Gson = Gson()
) {

    /** 源白名单条目（客户端本地只存这些，不存可直连源 URL） */
    data class WhiteSource(
        val srcId: String,
        val type: String,                 // CONFIG / JS / API
        val contentUrl: String            // 需签名后才下发的源内容 URL
    )

    /** /resolve 响应：加密 + 时效 token 的播放地址 */
    data class PlayUrl(val url: String, val expiresAt: Long, val token: String)

    /**
     * 解析播放地址（服务端按签名 + 白名单下发 AES-GCM 加密的 m3u8 + 5min token）。
     * 即使抓包/逆向客户端，也拿不到可直连源（只有加密 + 时效 token）。
     */
    fun resolve(srcId: String, ep: String, sign: (String) -> String): PlayUrl {
        val body = gson.toJson(mapOf("src_id" to srcId, "ep" to ep))
        val req = Request.Builder()
            .url("https://<own-service>/resolve")
            .header("X-SrcId", srcId)
            .header("X-Sign", sign(body))      // 调用方注入的签名（对应 6 设计 §3.3）
            .post(okhttp3.RequestBody.create(body, okhttp3.MediaType.parse("application/json")))
            .build()
        client.newCall(req).execute().use { resp ->
            val json = resp.body?.string() ?: ""
            val o = gson.fromJson(json, Map::class.java) as Map<String, Any>
            @Suppress("UNCHECKED_CAST")
            return PlayUrl(
                url = o["url"] as String,
                expiresAt = (o["expiresAt"] as Number).toLong(),
                token = o["token"] as String
            )
        }
    }
}
