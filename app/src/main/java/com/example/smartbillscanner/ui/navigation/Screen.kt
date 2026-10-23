package com.example.smartbillscanner.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Scanner : Screen("scanner")
    object Verify : Screen("verify")
    object Detail : Screen("detail")
    object SplitPay : Screen("split")
}
