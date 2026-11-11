package com.furiqd.route_builder

import com.furiqd.route_builder.data.StopsRepository
import com.furiqd.route_builder.model.RouteStopSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class StopsRepositoryTest {
    @Test
    fun `loads every page and deduplicates stops by stable id`() {
        val responses = ArrayDeque(
            listOf(
                """{"items":[{"id":"a","name":"Alpha","address":"A"},{"id":"b","name":"Beta","address":"B"}],"has_more":true}""",
                """{"items":[{"id":"b","name":"Beta duplicate","address":"B"},{"id":"c","name":"Cafe","address":"C"}],"has_more":false}""",
            )
        )
        val requestedUrls = mutableListOf<String>()
        val repository = StopsRepository("http://localhost:4000") { url ->
            requestedUrls += url.toString()
            FakeConnection(url, responses.removeFirst())
        }

        val stops = repository.loadStops("North Rd")

        assertEquals(listOf("a", "b", "c"), stops.map { it.id })
        assertEquals(2, requestedUrls.size)
        assertTrue(requestedUrls.all { it.contains("q=North+Rd") })
        assertTrue(requestedUrls[1].contains("page=2"))
    }

    @Test
    fun `uses reported visible count when mock has more flag remains true`() {
        val items = (0 until 24).joinToString(",") { index ->
            """{"id":"stop-$index","name":"Stop $index","address":"Street $index"}"""
        }
        var requestCount = 0
        val repository = StopsRepository("http://localhost:4000") { url ->
            requestCount += 1
            FakeConnection(
                url,
                """{"items":[$items],"total":25,"has_more":true}""",
            )
        }

        val stops = repository.loadStops()

        assertEquals(24, stops.size)
        assertEquals(1, requestCount)
    }

    @Test(expected = IllegalStateException::class)
    fun `reported visible count prevents accepting a prematurely ended catalog`() {
        val repository = StopsRepository("http://localhost:4000") { url ->
            FakeConnection(
                url,
                """{"items":[{"id":"a","name":"Alpha","address":"A"}],"total":3,"has_more":false}""",
            )
        }

        repository.loadStops()
    }

    @Test
    fun `skips malformed list entries and treats absent items as empty`() {
        val repository = StopsRepository("http://localhost:4000") { url ->
            FakeConnection(url, """{"items":[null,{"id":"ok","name":"Valid","address":"Here"},{"name":"Missing id"}],"has_more":false}""")
        }

        assertEquals(listOf("ok"), repository.loadStops().map { it.id })
    }

    @Test(expected = IllegalStateException::class)
    fun `empty page with more results does not return a partial catalog`() {
        val responses = ArrayDeque(
            listOf(
                """{"items":[{"id":"a","name":"Alpha","address":"A"}],"has_more":true}""",
                """{"items":[],"has_more":true}""",
            )
        )
        val repository = StopsRepository("http://localhost:4000") { url ->
            FakeConnection(url, responses.removeFirst())
        }

        repository.loadStops()
    }

    @Test(expected = IllegalStateException::class)
    fun `pagination error does not return a partial catalog`() {
        val responses = ArrayDeque(
            listOf(
                FakeConnection.Response(
                    """{"items":[{"id":"a","name":"Alpha","address":"A"}],"has_more":true}"""
                ),
                FakeConnection.Response("""{"error":"internal_error"}""", responseCode = 500),
            )
        )
        val repository = StopsRepository("http://localhost:4000") { url ->
            val response = responses.removeFirst()
            FakeConnection(url, response.body, response.responseCode)
        }

        repository.loadStops()
    }

    @Test(expected = IllegalStateException::class)
    fun `repeated page without new IDs is reported as incomplete`() {
        val responses = ArrayDeque(
            listOf(
                """{"items":[{"id":"a","name":"Alpha","address":"A"}],"has_more":true}""",
                """{"items":[{"id":"a","name":"Alpha","address":"A"}],"has_more":true}""",
            )
        )
        val repository = StopsRepository("http://localhost:4000") { url ->
            FakeConnection(url, responses.removeFirst())
        }

        repository.loadStops()
    }

    @Test(expected = IllegalStateException::class)
    fun `first page error remains visible to caller`() {
        val repository = StopsRepository("http://localhost:4000") { url ->
            FakeConnection(url, """{"error":"internal_error"}""", httpStatus = 500)
        }

        repository.loadStops()
    }

    @Test
    fun `route payload preserves original instruction text`() {
        lateinit var connection: FakeConnection
        val repository = StopsRepository("http://localhost:4000") { url ->
            FakeConnection(url, """{"id":"route-1"}""").also { connection = it }
        }

        val routeId = repository.createRoute(
            listOf(
                RouteStopSelection(
                    stopId = "stop-1",
                    stopName = "Cafe",
                    stopAddress = "Main St",
                    instructions = "  Leave at the desk  ",
                )
            )
        )

        assertEquals("route-1", routeId)
        assertTrue(connection.requestBody.contains("  Leave at the desk  "))
    }

    private class FakeConnection(
        url: URL,
        private val response: String,
        private val httpStatus: Int = 200,
    ) : HttpURLConnection(url) {
        data class Response(val body: String, val responseCode: Int = 200)

        private val request = ByteArrayOutputStream()
        val requestBody: String
            get() = request.toString(Charsets.UTF_8.name())

        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy() = false
        override fun getResponseCode(): Int = httpStatus
        override fun getInputStream(): InputStream = response.byteInputStream()
        override fun getErrorStream(): InputStream = response.byteInputStream()
        override fun getOutputStream() = request
    }
}
