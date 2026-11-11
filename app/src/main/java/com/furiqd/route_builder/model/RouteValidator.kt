package com.furiqd.route_builder.model

object RouteValidator {
    const val MAX_INSTRUCTION_LENGTH = 250
    private const val MAX_SAFE_INSTRUCTION_LENGTH = 200

    fun isStopValid(stop: RouteStopSelection): Boolean {
        val countValid = stop.packages > 0 && (stop.packagesMax == null || stop.packages <= stop.packagesMax)
        val instructionValid = stop.instructions.trim().isNotEmpty() && hasSafeInstructionLength(stop.instructions)
        return countValid && instructionValid
    }

    fun isStopValid(stop: RouteStopSelection, ignored: Any?): Boolean = isStopValid(stop)

    fun isInstructionWithinInputLimit(instruction: String): Boolean =
        instruction.length <= MAX_INSTRUCTION_LENGTH

    fun hasSafeInstructionLength(instruction: String): Boolean =
        instruction.length <= MAX_SAFE_INSTRUCTION_LENGTH

    fun isSubmitReady(stops: List<RouteStopSelection>): Boolean =
        stops.isNotEmpty() && stops.all { isStopValid(it) }

    fun requiresAttention(stops: List<RouteStopSelection>): List<String> =
        stops.filter { !isStopValid(it) }.map { it.stopName }
}
