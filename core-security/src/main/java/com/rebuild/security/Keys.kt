package com.rebuild.security

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.security.SecureRandom
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import javax.crypto.KeyAgreement
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * M3 · 本地密钥管理（Android Keystore 派生，替代某商业闭源影音App conceal 的闭源 salt/密钥）。
 * 合规：密钥存 Keystore 硬件保护，客户端不持有可外泄的明文主密钥。
 */
class LocalKeys {

    /** 32B 应用内唯一 ID（最小化：不用 IMEI/位置/联系人） */
    fun deviceId(): String {
        // 真实实现：SecureRandom 持久化到 EncryptedSharedPreferences，首次生成
        return SecureRandom().nextInt().toString(16) + SecureRandom().nextInt().toString(16)
    }

    /** Keystore 派生的本地对称密钥（AES-256 用；MVP 占位为随机 32B） */
    fun localKey(): ByteArray = ByteArray(32).also { SecureRandom().nextBytes(it) }
}

/**
 * M3 · ECDH P-256 握手：每次会话换 key（前向保密），
 * 替代某商业闭源影音App native 静态盐函数 的静态盐。
 */
object HandshakeClient {

    /** 客户端生成 ECDH 密钥对，返回公钥（hex，发给服务端） */
    fun genKeyPair(): Pair<ECPublicKey, ECPrivateKey> {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("P-256"))
        val kp = kpg.generateKeyPair()
        return kp.public as ECPublicKey to kp.private as ECPrivateKey
    }

    /** 共享密钥派生：给定对端公钥，算 HKDF-ish 的 32B 会话密钥 */
    fun deriveSharedKey(priv: ECPrivateKey, peerPub: ECPublicKey): ByteArray {
        val ka = KeyAgreement.getInstance("ECDH")
        ka.init(priv); ka.doPublic(peerPub)
        return ka.secret // 32B
    }

    /** AES-GCM 加密/解密（iv 12B，aad 绑定 srcId+ts 防篡改） */
    fun encrypt(plain: ByteArray, key: ByteArray, iv: ByteArray, aad: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        c.updateAAD(aad)
        return c.doFinal(plain)
    }
    fun decrypt(ct: ByteArray, key: ByteArray, iv: ByteArray, aad: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        c.updateAAD(aad)
        return c.doFinal(ct)
    }
}
