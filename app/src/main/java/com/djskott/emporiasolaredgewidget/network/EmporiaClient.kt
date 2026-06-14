package com.djskott.emporiasolaredgewidget.network

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class EmporiaClient(private val httpJsonClient: HttpJsonClient = HttpJsonClient()) {
    fun authenticate(email: String, password: String): EmporiaSession {
        val payload = JSONObject().apply {
            put("AuthParameters", JSONObject().apply {
                put("USERNAME", email)
                put("PASSWORD", password)
            })
            put("AuthFlow", "USER_PASSWORD_AUTH")
            put("ClientId", COGNITO_CLIENT_ID)
        }

        val response = httpJsonClient.postObject(
            url = COGNITO_URL,
            headers = mapOf(
                "Content-Type" to "application/x-amz-json-1.1",
                "X-Amz-Target" to "AWSCognitoIdentityProviderService.InitiateAuth",
            ),
            body = payload,
        )

        val authResult = response.getJSONObject("AuthenticationResult")
        return EmporiaSession(
            idToken = authResult.getString("IdToken"),
        )
    }

    fun discoverMainSelection(session: EmporiaSession): EmporiaSelection {
        val channels = httpJsonClient.getArray(
            url = "$EMPORIA_API_ORIGIN/v1/customers/devices/channels",
            headers = mapOf("Authorization" to session.idToken),
        )

        for (index in 0 until channels.length()) {
            val device = channels.getJSONObject(index)
            val channelIds = collectChannelIds(device.optJSONArray("channels") ?: JSONArray())
            val mainsLegs = listOf("Mains_A", "Mains_B").filter { channelIds.contains(it) }
            if (mainsLegs.size == 2) {
                return EmporiaSelection(
                    deviceId = device.getString("device_id"),
                    circuitIds = mainsLegs,
                )
            }
            if (channelIds.contains("Mains")) {
                return EmporiaSelection(
                    deviceId = device.getString("device_id"),
                    circuitIds = listOf("Mains"),
                )
            }
        }

        throw IllegalStateException("No Emporia energy monitor with mains channels was found.")
    }

    fun getCurrentHomeLoadKw(
        session: EmporiaSession,
        selection: EmporiaSelection,
    ): Double {
        val end = Instant.now()
        val start = end.minusSeconds(15 * 60L)
        val url = buildString {
            append("$EMPORIA_API_ORIGIN/v1/devices/energy-monitors/circuits/usages/power")
            append("?device_ids=")
            append(selection.deviceId.urlEncoded())
            append("&circuit_ids=")
            append(selection.circuitIds.joinToString(",").urlEncoded())
            append("&start=")
            append(TIMESTAMP_FORMATTER.format(start).urlEncoded())
            append("&end=")
            append(TIMESTAMP_FORMATTER.format(end).urlEncoded())
            append("&power_resolution=MINUTES")
        }

        val response = httpJsonClient.getObject(
            url = url,
            headers = mapOf("Authorization" to session.idToken),
        )

        val errors = response.optJSONArray("error") ?: JSONArray()
        if ((response.optJSONArray("success")?.length() ?: 0) == 0 && errors.length() > 0) {
            val firstError = errors.getJSONObject(0)
            throw IllegalStateException(firstError.optString("message", "Emporia power query failed."))
        }

        val successItems = response.getJSONArray("success")
        if (successItems.length() == 0) {
            throw IllegalStateException("Emporia returned no power samples.")
        }

        val devicePower = successItems.getJSONObject(0).getJSONArray("circuit_power")
        var totalKw = 0.0
        for (index in 0 until devicePower.length()) {
            val powerEntries = devicePower.getJSONObject(index).optJSONArray("power") ?: JSONArray()
            if (powerEntries.length() == 0) {
                continue
            }
            val latest = powerEntries.getJSONObject(powerEntries.length() - 1)
            totalKw += latest.optDouble("average_power_kw", 0.0)
        }

        return totalKw
    }

    private fun collectChannelIds(channels: JSONArray): Set<String> {
        val ids = linkedSetOf<String>()
        for (index in 0 until channels.length()) {
            ids += channels.getJSONObject(index).optString("channel_id")
        }
        return ids
    }

    private fun String.urlEncoded(): String {
        return URLEncoder.encode(this, StandardCharsets.UTF_8.toString())
    }

    companion object {
        private const val COGNITO_URL = "https://cognito-idp.us-east-2.amazonaws.com/"
        private const val COGNITO_CLIENT_ID = "4qte47jbstod8apnfic0bunmrq"
        private const val EMPORIA_API_ORIGIN = "https://c-api.emporiaenergy.com"
        private val TIMESTAMP_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC)
    }
}

data class EmporiaSession(
    val idToken: String,
)

data class EmporiaSelection(
    val deviceId: String,
    val circuitIds: List<String>,
)

