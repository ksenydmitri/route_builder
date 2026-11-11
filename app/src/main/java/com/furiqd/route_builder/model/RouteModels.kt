package com.furiqd.route_builder.model

data class Stop(
    val id: String,
    val name: String,
    val address: String,
    val phone: String? = null,
    val packagesMax: Int? = null,
    val zone: String? = null,
    val lastDeliveryAt: String? = null,
) {
    val initials: String
        get() = buildInitials(name)

    private fun buildInitials(value: String): String {
        val trimmed = value.trim()
        val parts = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "?"
            parts.size == 1 -> trimmed.take(2).uppercase()
            else -> listOf(parts.first(), parts.last()).joinToString("") { it.first().uppercase() }
        }
    }
}

enum class RouteMode(val displayName: String) {
    ONE_STOP("One stop"),
    MULTI_STOP("Multi-stop");

    fun allowsSelection(selectedCount: Int): Boolean =
        this == MULTI_STOP || selectedCount <= 1
}

data class RouteStopSelection(
    val stopId: String,
    val stopName: String,
    val stopAddress: String,
    val stopPhone: String? = null,
    val packages: Int = 1,
    val instructions: String = "",
    val packagesMax: Int? = null,
) {
    val isValid: Boolean
        get() = RouteValidator.isStopValid(this)

    fun withPackages(value: Int): RouteStopSelection = copy(packages = value.coerceAtLeast(1))
    fun withInstructions(value: String): RouteStopSelection = copy(instructions = value)
}
