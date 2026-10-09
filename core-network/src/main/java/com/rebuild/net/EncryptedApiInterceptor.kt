package com.rebuild.net

import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.Response
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 纯自研服务端加密拦截器（替代某商业闭源影音App conceal/nc/salt 的闭源 native 实现）。
 *
 * 请求：HMAC-SHA256(method + path + ts + nonce + sha256(body)) 放 X-Sign
 *       + X-Ts / X-Nonce / X-Kid / X-DevId
 * 响应：带 X-Enc=1 时，按 AES-GCM( key=sessionKey, iv=X-Iv, aad=srcId:ts ) 解密 body
 *
 * 不复用某商业闭源影音App任何 native/salt。密钥由服务端握手下发（见 core-security/Keys.kt ECDH）。
 *
 * ⚠ 安全红线：会话密钥【绝不可硬编码进仓库】。
 *   - 线上：由 ECDH 握手的 sessionKey 提供
 *   - 开发：从环境变量 REBUILD_DEV_KEY 读（.env 不入库，见 .gitignore）
 *   - 默认：空 key（仅用于本地 smoke，不代表可用密钥）
 */
class EncryptedApiInterceptor(
    /**
     * 当前会话密钥（字节）。
     * 默认从环境变量读，开发不硬编码；线上由 HandshakeClient 的 ECDH 会话密钥注入。
     */
    private val sessionKey: () -> ByteArray = {
        System.getenv("REBUILD_DEV_KEY")?.toByteArray() ?: ByteArray(0)
    },
    /** 设备唯一 ID（最小化，来自 LocalKeys.deviceId） */
    private val deviceId: () -> String = { System.getenv("REBUILD_DEV_ID") ?: "" },
    /** 当前源 ID（用于 aad 绑定） */
    private val srcId: () -> String = { "" }
) : Interceptor {

    private val HMAC = "HmacSHA256"
    private val GCM_IV_LEN = 12
    private val GCM_TAG_BITS = 128

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val ts = System.currentTimeMillis()
        val nonce = hex(SecureRandom().generateSeed(8))
        val bodyBytes = req.body?.bytes() ?: ByteArray(0)

        val signInput = listOf(
            req.method, req.url.encodedPath,
            ts.toString(), nonce,
            sha256Hex(bodyBytes)
        ).joinToString(" ")

        val sign = hmacSha256Hex(sessionKey(), signInput)

        val signed = req.newBuilder()
            .header("X-Ts", ts.toString())
            .header("X-Nonce", nonce)
            .header("X-Kid", "current")
            .header("X-Sign", sign)
            .header("X-DevId", deviceId())
            .header("X-SrcId", srcId())
            .build()

        val resp = chain.proceed(signed)

        return if (resp.header("X-Enc") == "1") decrypt(resp) else resp
    }

    /** 响应体 AES-GCM 解密（iv=X-Iv, aad=X-SrcId:X-Ts，key=sessionKey） */
    private fun decrypt(resp: Response): Response {
        val bodyStream = resp.body
        val ivHex = resp.header("X-Iv") ?: ""
        val aad = "${srcId()}:${resp.request.header("X-Ts")}".toByteArray()
        val iv = hexToBytes(ivHex)
        val ct = bodyStream?.bytes() ?: ByteArray(0)
        val plain = decryptAesGcm(ct, sessionKey(), iv, aad)
        val newBody = RequestBody.create(MediaType.parse("application/json"), plain)
        return resp.newBuilder().body(newBody).build()
    }

    // ---- 工具 ----
    private fun hmacSha256Hex(key: ByteArray, msg: String): String {
        val m = Mac.getInstance(HMAC).apply { init(SecretKeySpec(key, HMAC)) }
        return m.doFinal(msg.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun decryptAesGcm(
        ct: ByteArray, key: ByteArray, iv: ByteArray, aad: ByteArray
    ): ByteArray {
        val c = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        c.init(javax.crypto.Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"),
               GCMParameterSpec(GCM_TAG_BITS, iv))
        if (aad.isNotEmpty()) c.updateAAD(aad)
        return c.doFinal(ct)
    }

    private fun sha256Hex(b: ByteArray): String {
        val d = java.security.MessageDigest.getInstance("SHA-256").digest(b)
        return d.joinToString("") { "%02x".format(it) }
    }

    private fun hex(bytes: ByteArray) =
        bytes.joinToString("") { "%02x".format(it) }

    private fun hexToBytes(s: String): ByteArray {
        val out = ByteArray(s.length / 2)
        for (i in 0 until out.size) out[i] = s.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        return out
    }
}
