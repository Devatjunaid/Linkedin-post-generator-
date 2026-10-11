package com.shelfguard.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val DASHBOARD = "dashboard"
    const val INVENTORY = "inventory?filter={filter}"
    const val ALERTS = "alerts"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
    const val PRODUCT = "product/{id}"
    const val EDITOR = "editor?id={id}&barcode={barcode}"

    fun inventory(filter: String = "all") = "inventory?filter=$filter"
    fun product(id: Long) = "product/$id"
    fun newProduct(barcode: String = "") = "editor?id=-1&barcode=${android.net.Uri.encode(barcode)}"
    fun editProduct(id: Long) = "editor?id=$id&barcode="
}

enum class TopLevel(
    val route: String,
    val navRoute: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    HOME(Routes.DASHBOARD, Routes.DASHBOARD, "Home", Icons.Outlined.Home, Icons.Filled.Home),
    STOCK(Routes.INVENTORY, Routes.inventory(), "Stock", Icons.Outlined.Inventory2, Icons.Filled.Inventory2),
    ALERTS(Routes.ALERTS, Routes.ALERTS, "Alerts", Icons.Outlined.Notifications, Icons.Filled.Notifications),
    REPORTS(Routes.REPORTS, Routes.REPORTS, "Reports", Icons.Outlined.BarChart, Icons.Filled.BarChart),
}
