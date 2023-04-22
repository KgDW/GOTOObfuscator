package org.gotoobfuscator.utils

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec

object EncodeUtils {
    fun desEncode(data : ByteArray,key : ByteArray): ByteArray {
        val sr = SecureRandom()

        val dks = DESKeySpec(key)

        val keyFactory = SecretKeyFactory.getInstance("DES")
        val secureKey = keyFactory.generateSecret(dks)

        val cipher = Cipher.getInstance("DES")

        cipher.init(Cipher.ENCRYPT_MODE, secureKey, sr)
        return cipher.doFinal(data)
    }
}