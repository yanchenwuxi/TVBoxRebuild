package com.rebuild.net

import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps

/**
 * 纯自研 DoH 客户端（防 DNS 污染），替代某商业闭源影音App okhttp3.dnsoverhttps 的自研行为。
 * 用 OkHttp 官方 DoH（MIT），但接入/配置完全自研，不复用某商业闭源影音App端点。
 */
class DoHClient(private val client: OkHttpClient = OkHttpClient()) {

    fun safeDns(dohUrl: String = "https://doh.pub/dns-query"): okhttp3.Dns {
        val client = client.newBuilder()
            .sslSocketFactory(null, null) // 走 DoH 的 TLS
            .build()
        return DnsOverHttps(client, okhttp3.HttpUrl.parse(dohUrl)!!)
    }
}
