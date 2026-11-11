package com.furiqd.route_builder

import com.furiqd.route_builder.model.Stop
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteModelsTest {
    @Test
    fun `initials use first and last name parts`() {
        assertEquals("JD", Stop("1", "Jane Middle Doe", "Address").initials)
    }

    @Test
    fun `single part name uses first two trimmed characters`() {
        assertEquals("CA", Stop("1", "  Cafe  ", "Address").initials)
    }
}
