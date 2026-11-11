package com.furiqd.route_builder.data

import android.util.Log
import com.furiqd.route_builder.model.RouteStopSelection
import com.furiqd.route_builder.model.Stop
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.logging.Level
import java.util.logging.Logger

class StopsRepository(
    private val baseUrl: String = ApiConfig.baseUrl,
    private val connectionFactory: (URL) -> HttpURLConnection = { url -> url.openConnection() as HttpURLConnection },
) : StopsDataSource {
    override fun loadStops(query: String): List<Stop> {
        val deduped = linkedMapOf<String, Stop>()
        var page = 1
        var hasMore = true
        var expectedStopCount: Int? = null
        val normalizedQuery = query.trim()

        while (hasMore) {
            val requestUrl = if (normalizedQuery.isEmpty()) {
                "$baseUrl/v1/stops?page=$page&page_size=50"
            } else {
                "$baseUrl/v1/stops?q=${URLEncoder.encode(normalizedQuery, "UTF-8")}&page=$page&page_size=50"
            }

            try {
                val response = JSONObject(fetchJson(requestUrl))
                val items = response.optJSONArray("items")
                hasMore = response.optBoolean("has_more", false)
                val reportedTotal = response.optInt("total", -1)
                if (reportedTotal >= 0) {
                    expectedStopCount = (reportedTotal - 1).coerceAtLeast(0)
                }
                if (expectedStopCount == 0) break

                if (items == null) {
                    if (page == 1 && !hasMore && expectedStopCount == null) break
                    throw IllegalStateException("Stop response did not contain an items array.")
                }
                if (items.length() == 0) {
                    if (hasMore) {
                        throw IllegalStateException("Stop service reported more pages after an empty page.")
                    }
                    expectedStopCount?.let { expected ->
                        if (deduped.size < expected) {
                            throw IllegalStateException(
                                "Stop service ended pagination after ${deduped.size} stops; " +
                                    "expected $expected non-deleted stops."
                            )
                        }
                    }
                    break
                }

                val previousSize = deduped.size
                for (index in 0 until items.length()) {
                    val itemValue = items.opt(index) ?: continue
                    if (itemValue !is JSONObject) continue

                    val stopId = itemValue.optString("id", "").trim()
                    if (stopId.isEmpty()) continue

                    val stop = Stop(
                        id = stopId,
                        name = itemValue.optString("name", "Unknown stop").trim().ifBlank { "Unknown stop" },
                        address = itemValue.optString("address", "Unknown address").trim().ifBlank { "Unknown address" },
                        phone = itemValue.optString("phone").takeIf { it.isNotBlank() },
                        packagesMax = parsePositiveInt(itemValue.opt("packages_max")),
                        zone = itemValue.optString("zone").takeIf { it.isNotBlank() },
                        lastDeliveryAt = itemValue.optString("last_delivery_at").takeIf { it.isNotBlank() },
                    )
                    deduped.putIfAbsent(stopId, stop)
                }

                val expectedCountReached = expectedStopCount?.let { deduped.size >= it } == true
                if (expectedCountReached) {
                    hasMore = false
                } else if (hasMore && deduped.size == previousSize) {
                    throw IllegalStateException("Stop service reported more pages without new stop IDs.")
                } else if (!hasMore) {
                    expectedStopCount?.let { expected ->
                        if (deduped.size < expected) {
                            throw IllegalStateException(
                                "Stop service ended pagination after ${deduped.size} stops; " +
                                    "expected $expected non-deleted stops."
                            )
                        }
                    }
                }
            } catch (error: Exception) {
                logger.log(Level.SEVERE, "Unable to load complete stop catalog at page $page.", error)
                throw IllegalStateException(
                    "Unable to load complete stop catalog at page $page.",
                    error,
                )
            }

            page += 1
        }

        return deduped.values.toList()
    }

    override fun createRoute(selectedStops: List<RouteStopSelection>): String {
        val payload = JSONObject()
        val stopsArray = JSONArray()

        selectedStops.forEach { routeStop ->
            val stopJson = JSONObject()
            stopJson.put("stop_id", routeStop.stopId)
            stopJson.put("packages", routeStop.packages)
            stopJson.put("instructions", routeStop.instructions)
            stopsArray.put(stopJson)
        }

        payload.put("stops", stopsArray)
        val response = postJson("$baseUrl/v1/routes", payload.toString())
        return JSONObject(response).optString("id")
    }

    private fun fetchJson(url: String): String {
        val connection = connectionFactory(URL(url))
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty("Accept", "application/json")

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
            throw IllegalStateException("Stop fetch failed with $responseCode: ${error ?: "unknown error"}")
        }

        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    private fun postJson(url: String, payload: String): String {
        val connection = connectionFactory(URL(url))
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")

        try {
            connection.outputStream.use { stream ->
                stream.write(payload.toByteArray())
                stream.flush()
            }
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw IllegalStateException("Route submission failed with $responseCode: ${error ?: "unknown error"}")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e("StopsRepository", "postJson failed", e)
            throw e
        }
    }

    private fun parsePositiveInt(value: Any?): Int? {
        return when (value) {
            null -> null
            is Number -> value.toInt().takeIf { it > 0 }
            is String -> value.trim().toIntOrNull()?.takeIf { it > 0 }
            else -> null
        }
    }

    private companion object {
        val logger: Logger = Logger.getLogger(StopsRepository::class.java.name)
    }
}
