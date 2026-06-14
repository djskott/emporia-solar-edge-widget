package com.djskott.emporiasolaredgewidget.network

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class SolarEdgeClient(private val httpJsonClient: HttpJsonClient = HttpJsonClient()) {
    fun discoverSiteId(apiKey: String): String {
        val response = httpJsonClient.getObject(
            url = "$SOLAREDGE_API_ORIGIN/sites/list?api_key=${apiKey.urlEncoded()}",
        )

        val sites = response.optJSONObject("sites")
            ?: throw IllegalStateException("SolarEdge did not return a site list.")
        val siteNode = sites.opt("site")
            ?: throw IllegalStateException("SolarEdge did not return any sites.")

        val siteArray = when (siteNode) {
            is JSONArray -> siteNode
            is JSONObject -> JSONArray().put(siteNode)
            else -> JSONArray()
        }

        if (siteArray.length() == 0) {
            throw IllegalStateException("SolarEdge did not return any sites.")
        }

        var firstSiteId: String? = null
        for (index in 0 until siteArray.length()) {
            val site = siteArray.getJSONObject(index)
            val siteId = site.opt("id")?.toString()
            if (siteId != null && firstSiteId == null) {
                firstSiteId = siteId
            }
            if (site.optString("status").equals("Active", ignoreCase = true) && siteId != null) {
                return siteId
            }
        }

        return firstSiteId ?: throw IllegalStateException("SolarEdge site id was missing.")
    }

    fun getCurrentProductionKw(apiKey: String, siteId: String): Double {
        val response = httpJsonClient.getObject(
            url = "$SOLAREDGE_API_ORIGIN/site/$siteId/currentPowerFlow?api_key=${apiKey.urlEncoded()}",
        )

        val flow = response.optJSONObject("siteCurrentPowerFlow")
            ?: throw IllegalStateException("SolarEdge did not return current power flow.")
        val unit = flow.optString("unit", "kW")
        val pv = flow.optJSONObject("PV")
        val currentPower = pv?.optDouble("currentPower", 0.0) ?: 0.0
        return when (unit) {
            "W" -> currentPower / 1000.0
            else -> currentPower
        }
    }

    private fun String.urlEncoded(): String {
        return URLEncoder.encode(this, StandardCharsets.UTF_8.toString())
    }

    companion object {
        private const val SOLAREDGE_API_ORIGIN = "https://monitoringapi.solaredge.com"
    }
}

