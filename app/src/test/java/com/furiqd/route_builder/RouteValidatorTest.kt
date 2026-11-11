package com.furiqd.route_builder

import com.furiqd.route_builder.model.RouteMode
import com.furiqd.route_builder.model.RouteStopSelection
import com.furiqd.route_builder.model.RouteValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteValidatorTest {
    @Test
    fun `non empty instruction passes validation`() {
        val stop = RouteStopSelection(
            stopId = "stp_1",
            packages = 2,
            instructions = "Deliver to the front desk",
            stopName = "North Cafe",
            stopAddress = "12 North Rd"
        )

        assertTrue(RouteValidator.isStopValid(stop, null))
    }

    @Test
    fun `blank instruction fails validation`() {
        val stop = RouteStopSelection(
            stopId = "stp_1",
            packages = 2,
            instructions = "   ",
            stopName = "North Cafe",
            stopAddress = "12 North Rd"
        )

        assertFalse(RouteValidator.isStopValid(stop, null))
    }

    @Test
    fun `instruction input accepts 250 characters but service validation stops at 200`() {
        val serviceLimit = "x".repeat(200)
        val designLimit = "x".repeat(250)

        assertTrue(RouteValidator.isInstructionWithinInputLimit(designLimit))
        assertFalse(RouteValidator.isInstructionWithinInputLimit("$designLimit x"))
        assertTrue(RouteValidator.hasSafeInstructionLength(serviceLimit))
        assertFalse(RouteValidator.hasSafeInstructionLength(serviceLimit + "x"))
    }

    @Test
    fun `mode selection enforces one stop only`() {
        assertTrue(RouteMode.ONE_STOP.allowsSelection(1))
        assertFalse(RouteMode.ONE_STOP.allowsSelection(2))
        assertTrue(RouteMode.MULTI_STOP.allowsSelection(2))
    }
}
