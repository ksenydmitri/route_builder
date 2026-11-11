package com.furiqd.route_builder

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.furiqd.route_builder.data.StopsRepository
import com.furiqd.route_builder.data.NetworkAvailability
import com.furiqd.route_builder.model.RouteMode
import com.furiqd.route_builder.ui.InstructionsScreen
import com.furiqd.route_builder.ui.RouteViewModel
import com.furiqd.route_builder.ui.StopsScreen
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import java.io.Closeable
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.json.JSONArray
import org.json.JSONObject

class RouteBuilderUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun stopGroupsSearchResultsAndSelectionSummaryAreVisible() {
        val viewModel = createViewModel()
        composeRule.setContent { StopsScreen(viewModel) }
        viewModel.loadStops()

        composeRule.onNodeWithText("A").assertIsDisplayed()
        composeRule.onNodeWithText("B").assertIsDisplayed()
        composeRule.onNodeWithTag("stop_group_#").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Alpha Depot").performClick()
        composeRule.onNodeWithText("Continue (1)").assertIsDisplayed()
        composeRule.onAllNodesWithTag("selected_stop_chip").fetchSemanticsNodes().also {
            assert(it.size == 1)
        }

        composeRule.onNodeWithTag("search_input_field").performTextInput("no match")
        composeRule.onNodeWithText("No stops found").assertIsDisplayed()
    }

    @Test
    fun parcelCountIsEditedInlineAndSaved() {
        val viewModel = createViewModel()
        composeRule.setContent { StopsScreen(viewModel) }
        viewModel.loadStops()

        composeRule.onNodeWithText("Alpha Depot").performClick()
        composeRule.onNodeWithTag("parcel_count_stp_a").performClick()
        composeRule.onNodeWithTag("parcel_plus_stp_a").performClick()
        composeRule.onNodeWithText("Save").performClick()

        composeRule.onNodeWithText("2×").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("Parcel count").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun emptyInstructionUsesRequiredCopyAndSuccessIsInline() {
        val viewModel = createViewModel()
        selectOneStop(viewModel)
        composeRule.setContent { InstructionsScreen(viewModel) }

        composeRule.onNodeWithText("Dispatch Route").performClick()
        composeRule.onNodeWithText("Delivery instruction is required for this stop.").assertIsDisplayed()
        viewModel.setInstruction("stp_a", "Leave at the front desk")
        composeRule.onNodeWithText("Dispatch Route").performClick()

        composeRule.onNodeWithText("Route Dispatched Successfully.").assertIsDisplayed()
        composeRule.onNodeWithText("Route ID: rt_test").assertIsDisplayed()
    }

    @Test
    fun failedSubmissionShowsDuplicateRiskWarningBeforeRetry() {
        val viewModel = createViewModel(routeStatus = 500)
        selectOneStop(viewModel)
        viewModel.setInstruction("stp_a", "Leave at the front desk")
        composeRule.setContent { InstructionsScreen(viewModel) }

        composeRule.onNodeWithText("Dispatch Route").performClick()

        composeRule.onNodeWithText(
            "Route submission failed or timed out. Retrying may create a duplicate route. Do you want to proceed?"
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Retry route").assertIsDisplayed()
    }

    @Test
    fun offlineStatePreservesDraftAndDisablesSubmission() {
        val network = TestNetworkAvailability()
        val viewModel = createViewModel(networkAvailability = network)
        selectOneStop(viewModel)
        viewModel.setInstruction("stp_a", "Leave at the front desk")
        composeRule.setContent { InstructionsScreen(viewModel) }

        network.setAvailable(false)

        composeRule.onNodeWithText(
            "No internet connection. Route draft preserved locally."
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Leave at the front desk").assertIsDisplayed()
        composeRule.onNodeWithText("Dispatch Route").assertIsNotEnabled()
    }

    private fun selectOneStop(viewModel: RouteViewModel) {
        viewModel.loadStops()
        viewModel.changeRouteMode(RouteMode.ONE_STOP)
        viewModel.toggleStop(viewModel.availableStops.first())
        viewModel.openInstructions()
    }

    private fun createViewModel(
        routeStatus: Int = 201,
        networkAvailability: NetworkAvailability = TestNetworkAvailability(),
    ): RouteViewModel {
        val stops = listOf(
            StopFixture("stp_a", "Alpha Depot", "1 A Road", 4),
            StopFixture("stp_b", "Beta Store", "2 B Road", 3),
            StopFixture("stp_other", "24/7 Pharmacy", "3 Market Road", 8),
        )
        val repository = StopsRepository("http://mock") { url ->
            if (url.path.endsWith("/v1/routes")) {
                TestConnection(url, """{"id":"rt_test"}""", routeStatus)
            } else {
                val query = url.query.orEmpty()
                    .substringAfter("q=", "")
                    .substringBefore('&')
                    .let { URLDecoder.decode(it, "UTF-8") }
                val matched = if (query.isBlank()) stops else stops.filter {
                    it.name.contains(query, ignoreCase = true) ||
                        it.address.contains(query, ignoreCase = true)
                }
                val items = JSONArray()
                matched.forEach { stop ->
                    items.put(
                        JSONObject()
                            .put("id", stop.id)
                            .put("name", stop.name)
                            .put("address", stop.address)
                            .put("packages_max", stop.max)
                    )
                }
                TestConnection(url, JSONObject().put("items", items).put("has_more", false).toString())
            }
        }
        return RouteViewModel(
            repository = repository,
            networkAvailability = networkAvailability,
            testScope = CoroutineScope(Dispatchers.Unconfined),
        )
    }

    private data class StopFixture(val id: String, val name: String, val address: String, val max: Int)

    private class TestNetworkAvailability : NetworkAvailability {
        private var listener: ((Boolean) -> Unit)? = null
        private var available = true

        override fun isAvailable(): Boolean = available

        override fun observe(listener: (Boolean) -> Unit): Closeable {
            this.listener = listener
            listener(available)
            return Closeable { this.listener = null }
        }

        fun setAvailable(value: Boolean) {
            available = value
            listener?.invoke(value)
        }
    }

    private class TestConnection(
        url: URL,
        private val response: String,
        private val status: Int = 200,
    ) : HttpURLConnection(url) {
        private val request = ByteArrayOutputStream()

        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy() = false
        override fun getResponseCode(): Int = status
        override fun getInputStream(): InputStream = response.byteInputStream()
        override fun getErrorStream(): InputStream = response.byteInputStream()
        override fun getOutputStream() = request
    }
}
