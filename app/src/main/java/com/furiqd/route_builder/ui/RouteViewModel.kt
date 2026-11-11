package com.furiqd.route_builder.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.furiqd.route_builder.data.AlwaysAvailableNetwork
import com.furiqd.route_builder.data.NetworkAvailability
import com.furiqd.route_builder.data.StopsRepository
import com.furiqd.route_builder.data.StopsDataSource
import com.furiqd.route_builder.model.RouteMode
import com.furiqd.route_builder.model.RouteStopSelection
import com.furiqd.route_builder.model.RouteValidator
import com.furiqd.route_builder.model.Stop
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Closeable

class RouteViewModel(
    private val repository: StopsDataSource = StopsRepository(),
    private val networkAvailability: NetworkAvailability = AlwaysAvailableNetwork,
    private val testScope: CoroutineScope? = null,
) : ViewModel() {
    private val operationScope: CoroutineScope
        get() = testScope ?: viewModelScope
    private var stopsLoadJob: Job? = null

    var routeMode by mutableStateOf(RouteMode.MULTI_STOP)
        private set
    var availableStops by mutableStateOf<List<Stop>>(emptyList())
        private set
    var selectedStops by mutableStateOf<List<RouteStopSelection>>(emptyList())
        private set
    var currentQuery by mutableStateOf("")
        private set
    var isLoading by mutableStateOf(false)
        private set
    var submitting by mutableStateOf(false)
        private set
    var lastError by mutableStateOf<String?>(null)
        private set
    var confirmationRouteId by mutableStateOf<String?>(null)
        private set
    var currentScreen by mutableStateOf(RouteScreen.STOPS)
        private set
    var retryWarning by mutableStateOf(false)
        private set
    var showValidationErrors by mutableStateOf(false)
        private set
    var isOffline by mutableStateOf(!networkAvailability.isAvailable())
        private set

    private val networkListener: Closeable = networkAvailability.observe { available ->
        isOffline = !available
        if (available && lastError == OFFLINE_MESSAGE) {
            lastError = null
        } else if (!available) {
            lastError = OFFLINE_MESSAGE
        }
    }

    fun loadStops(query: String = currentQuery) {
        val queryChanged = query != currentQuery
        currentQuery = query
        if (queryChanged) {
            availableStops = emptyList()
        }
        if (!networkAvailability.isAvailable()) {
            isOffline = true
            lastError = OFFLINE_MESSAGE
            isLoading = false
            return
        }
        isOffline = false
        isLoading = true
        lastError = null
        stopsLoadJob?.cancel()
        stopsLoadJob = operationScope.launch {
            try {
                val stops = if (testScope != null) {
                    repository.loadStops(query)
                } else {
                    withContext(Dispatchers.IO) { repository.loadStops(query) }
                }
                if (query == currentQuery) {
                    availableStops = stops
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (query == currentQuery) {
                    lastError = error.message ?: "Unable to load stops"
                }
            } finally {
                if (query == currentQuery) {
                    isLoading = false
                }
            }
        }
    }

    fun changeRouteMode(mode: RouteMode) {
        if (mode == RouteMode.ONE_STOP && selectedStops.size > 1) {
            lastError = "Reduce selections to one stop before switching to One stop mode."
            return
        }
        routeMode = mode
    }

    fun toggleStop(stop: Stop) {
        val existing = selectedStops.find { it.stopId == stop.id }
        if (existing != null) {
            selectedStops = selectedStops.filter { it.stopId != stop.id }
            return
        }

        if (routeMode == RouteMode.ONE_STOP) {
            selectedStops = listOf(
                RouteStopSelection(
                    stopId = stop.id,
                    stopName = stop.name,
                    stopAddress = stop.address,
                    stopPhone = stop.phone,
                    packages = 1,
                    packagesMax = stop.packagesMax,
                    instructions = "",
                )
            )
            return
        }

        val nextSelection = RouteStopSelection(
            stopId = stop.id,
            stopName = stop.name,
            stopAddress = stop.address,
            stopPhone = stop.phone,
            packages = 1,
            packagesMax = stop.packagesMax,
            instructions = "",
        )
        selectedStops = selectedStops + nextSelection
    }

    fun removeStop(stopId: String) {
        selectedStops = selectedStops.filter { it.stopId != stopId }
        if (selectedStops.isEmpty()) {
            currentScreen = RouteScreen.STOPS
            confirmationRouteId = null
            retryWarning = false
        }
    }

    fun setCount(stopId: String, count: Int) {
        selectedStops = selectedStops.map {
            if (it.stopId == stopId) {
                val maxAllowed = it.packagesMax
                val safeValue = if (maxAllowed != null) count.coerceIn(1, maxAllowed) else count.coerceAtLeast(1)
                it.copy(packages = safeValue)
            } else it
        }
    }

    fun setInstruction(stopId: String, instruction: String) {
        if (!RouteValidator.isInstructionWithinInputLimit(instruction)) return
        selectedStops = selectedStops.map {
            if (it.stopId == stopId) it.copy(instructions = instruction) else it
        }
    }

    fun clearInstruction(stopId: String) {
        setInstruction(stopId, "")
    }

    fun openInstructions() {
        if (selectedStops.isNotEmpty()) {
            currentScreen = RouteScreen.INSTRUCTIONS
        }
    }

    fun returnToStops() {
        currentScreen = RouteScreen.STOPS
    }

    fun submitRoute(forceRetry: Boolean = false): Boolean {
        val prepared = selectedStops
        if (prepared.isEmpty()) return false
        if (!networkAvailability.isAvailable()) {
            isOffline = true
            lastError = OFFLINE_MESSAGE
            return false
        }
        if (!prepared.all { RouteValidator.isStopValid(it) }) {
            showValidationErrors = true
            lastError = null
            retryWarning = false
            return false
        }
        if (submitting) return false
        if (!forceRetry && confirmationRouteId != null) return false

        submitting = true
        showValidationErrors = false
        lastError = null
        retryWarning = false
        var success = false
        operationScope.launch {
            try {
                val routeId = if (testScope != null) {
                    repository.createRoute(prepared)
                } else {
                    withContext(Dispatchers.IO) { repository.createRoute(prepared) }
                }
                if (routeId.isBlank()) {
                    confirmationRouteId = null
                    lastError = "The route outcome could not be confirmed because no route ID was returned."
                    retryWarning = true
                } else {
                    confirmationRouteId = routeId
                    success = true
                }
            } catch (error: Exception) {
                lastError = error.message ?: "Unable to submit route"
                retryWarning = true
            } finally {
                submitting = false
                if (success) {
                    currentScreen = RouteScreen.INSTRUCTIONS
                }
            }
        }
        return success
    }

    fun confirmSuccess() {
        returnToStops()
    }

    fun onConfirmed() {
        confirmSuccess()
    }

    fun isStopSelected(stopId: String): Boolean = selectedStops.any { it.stopId == stopId }

    fun selectedCountLabel(): String = "${selectedStops.size} selected"
    fun canContinue(): Boolean = selectedStops.isNotEmpty()

    override fun onCleared() {
        networkListener.close()
        stopsLoadJob?.cancel()
        super.onCleared()
    }

    companion object {
        const val OFFLINE_MESSAGE = "No internet connection. Route draft preserved locally."
    }
}

enum class RouteScreen {
    STOPS,
    INSTRUCTIONS
}
