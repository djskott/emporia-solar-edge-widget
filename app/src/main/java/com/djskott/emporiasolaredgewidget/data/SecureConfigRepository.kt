package com.djskott.emporiasolaredgewidget.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.djskott.emporiasolaredgewidget.model.WidgetConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureConfigRepository(private val context: Context) {
    private val secretsDir: File by lazy {
        File(context.filesDir, "secrets").apply { mkdirs() }
    }

    private val configFile: File by lazy {
        File(secretsDir, CONFIG_FILE_NAME)
    }

    fun load(): WidgetConfig? {
        if (!configFile.exists()) {
            return null
        }

        val encryptedText = configFile.readText(StandardCharsets.UTF_8)
        if (encryptedText.isBlank()) {
            return null
        }

        val json = JSONObject(String(decrypt(encryptedText), StandardCharsets.UTF_8))
        val circuits = buildList {
            val array = json.optJSONArray(KEY_EMPORIA_CIRCUITS) ?: JSONArray()
            for (index in 0 until array.length()) {
                add(array.optString(index))
            }
        }.filter { it.isNotBlank() }

        return WidgetConfig(
            emporiaEmail = json.optString(KEY_EMPORIA_EMAIL),
            emporiaPassword = json.optString(KEY_EMPORIA_PASSWORD),
            solarEdgeApiKey = json.optString(KEY_SOLAREDGE_API_KEY),
            solarEdgeSiteId = json.optString(KEY_SOLAREDGE_SITE_ID).ifBlank { null },
            emporiaDeviceId = json.optString(KEY_EMPORIA_DEVICE_ID).ifBlank { null },
            emporiaCircuitIds = circuits,
        )
    }

    fun save(config: WidgetConfig) {
        val json = JSONObject().apply {
            put(KEY_EMPORIA_EMAIL, config.emporiaEmail)
            put(KEY_EMPORIA_PASSWORD, config.emporiaPassword)
            put(KEY_SOLAREDGE_API_KEY, config.solarEdgeApiKey)
            put(KEY_SOLAREDGE_SITE_ID, config.solarEdgeSiteId ?: "")
            put(KEY_EMPORIA_DEVICE_ID, config.emporiaDeviceId ?: "")
            put(KEY_EMPORIA_CIRCUITS, JSONArray(config.emporiaCircuitIds))
        }

        configFile.writeText(encrypt(json.toString().toByteArray(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)
    }

    fun clear() {
        configFile.delete()
    }

    private fun encrypt(plainBytes: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val encryptedBytes = cipher.doFinal(plainBytes)
        return JSONObject().apply {
            put(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            put(KEY_DATA, Base64.encodeToString(encryptedBytes, Base64.NO_WRAP))
        }.toString()
    }

    private fun decrypt(serializedCipherText: String): ByteArray {
        val payload = JSONObject(serializedCipherText)
        val iv = Base64.decode(payload.getString(KEY_IV), Base64.NO_WRAP)
        val encryptedBytes = Base64.decode(payload.getString(KEY_DATA), Base64.NO_WRAP)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(encryptedBytes)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existingKey = keyStore.getKey(KEY_ALIAS, null)
        if (existingKey is SecretKey) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )

        return keyGenerator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val KEY_ALIAS = "emporia_solar_edge_widget_config_key"
        private const val CONFIG_FILE_NAME = "widget-config.enc"
        private const val KEY_IV = "iv"
        private const val KEY_DATA = "data"
        private const val KEY_EMPORIA_EMAIL = "emporiaEmail"
        private const val KEY_EMPORIA_PASSWORD = "emporiaPassword"
        private const val KEY_SOLAREDGE_API_KEY = "solarEdgeApiKey"
        private const val KEY_SOLAREDGE_SITE_ID = "solarEdgeSiteId"
        private const val KEY_EMPORIA_DEVICE_ID = "emporiaDeviceId"
        private const val KEY_EMPORIA_CIRCUITS = "emporiaCircuitIds"
    }
}

