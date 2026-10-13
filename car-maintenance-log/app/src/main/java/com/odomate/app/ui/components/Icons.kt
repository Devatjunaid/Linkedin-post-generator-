package com.odomate.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DiscFull
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.LocalCarWash
import androidx.compose.material.icons.outlined.OilBarrel
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.TireRepair
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector

fun serviceIcon(key: String): ImageVector = when (key) {
    "oil" -> Icons.Outlined.OilBarrel
    "air_filter" -> Icons.Outlined.FilterAlt
    "cabin_filter" -> Icons.Outlined.Air
    "spark" -> Icons.Outlined.ElectricBolt
    "tire" -> Icons.Outlined.TireRepair
    "alignment" -> Icons.Outlined.Straighten
    "brakes" -> Icons.Outlined.DiscFull
    "drop" -> Icons.Outlined.WaterDrop
    "coolant" -> Icons.Outlined.Thermostat
    "transmission" -> Icons.Outlined.Settings
    "battery" -> Icons.Outlined.BatteryChargingFull
    "wiper" -> Icons.Outlined.CleaningServices
    "insurance" -> Icons.Outlined.VerifiedUser
    "document" -> Icons.Outlined.Description
    "inspection" -> Icons.Outlined.TaskAlt
    "wash" -> Icons.Outlined.LocalCarWash
    else -> Icons.Outlined.Build
}
