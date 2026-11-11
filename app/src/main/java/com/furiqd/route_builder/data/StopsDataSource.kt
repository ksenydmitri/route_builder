package com.furiqd.route_builder.data

import com.furiqd.route_builder.model.RouteStopSelection
import com.furiqd.route_builder.model.Stop

interface StopsDataSource {
    fun loadStops(query: String = ""): List<Stop>
    fun createRoute(selectedStops: List<RouteStopSelection>): String
}
