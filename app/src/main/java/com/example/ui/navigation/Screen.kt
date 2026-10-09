package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Scan : Screen("scan?mode={mode}") {
        const val defaultRoute = "scan?mode=single"
        fun createRoute(mode: String = "single") = "scan?mode=$mode"
    }
    object Create : Screen("create?category={category}") {
        const val defaultRoute = "create?category=URL"
        fun createRoute(category: String = "URL") = "create?category=$category"
    }
    object History : Screen("history?filter={filter}") {
        const val defaultRoute = "history?filter=all"
        fun createRoute(filter: String = "all") = "history?filter=$filter"
    }
    object Settings : Screen("settings")
    object ResultDetail : Screen("result/{recordId}") {
        fun createRoute(recordId: Long) = "result/$recordId"
    }
}
