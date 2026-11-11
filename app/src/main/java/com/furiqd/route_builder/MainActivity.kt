package com.furiqd.route_builder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.furiqd.route_builder.ui.RouteBuilderApp
import com.furiqd.route_builder.ui.theme.Route_builderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Route_builderTheme {
                RouteBuilderApp()
            }
        }
    }
}
