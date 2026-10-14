package com.harrycodess.fueltracker.ui.calculator

import kotlinx.coroutines.flow.MutableStateFlow

/** A model picked on the Cars screen to use in the calculator. */
data class CalculatorPrefill(val label: String, val kmPerL: Double)

/** Hands a picked model from the Cars screen to the calculator, which consumes and clears it. */
object CalculatorPrefillBus {
    val request = MutableStateFlow<CalculatorPrefill?>(null)
}
