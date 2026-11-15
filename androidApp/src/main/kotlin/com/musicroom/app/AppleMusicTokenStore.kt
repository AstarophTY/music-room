package com.musicroom.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class AppleMusicTokenStore(context: Context) {
    private val preferences = context.getSharedPreferences("apple_music_credentials", Context.MODE_PRIVATE)

    fun save(userToken: String, storefront: String?, appleStorefront: String?) {
        val payload = JSONObject().put("token", userToken).put("storefront", storefront)
            .put("appleStorefront", appleStorefront).toString().toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(payload)
        check(preferences.edit().putString("credentials", Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)).commit())
    }

    fun load(): JSONObject? {
        val encoded = preferences.getString("credentials", null) ?: return null
        return try {
            val data = Base64.decode(encoded, Base64.NO_WRAP)
            require(data.size > IV_BYTES)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, data.copyOfRange(0, IV_BYTES)))
            JSONObject(String(cipher.doFinal(data.copyOfRange(IV_BYTES, data.size)), Charsets.UTF_8))
        } catch (_: Exception) {
            clear()
            null
        }
    }

    fun clear() {
        preferences.edit().remove("credentials").commit()
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build())
        }.generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "music_room_apple_music"
        const val IV_BYTES = 12
    }
}
