package com.superdevlabs.coldplunge.domain

/**
 * Conservative, general safety guidance shown next to the controls. This is not medical advice;
 * the app never promises health outcomes.
 */
object Safety {

    fun coldNotes(tempC: Float, durationSec: Int): List<String> = buildList {
        if (durationSec > 10 * 60) {
            add("Over 10 minutes greatly raises hypothermia risk. Most routines use 1–5 minutes.")
        } else if (tempC <= 5f && durationSec > 5 * 60) {
            add("At 5 °C or colder, keep each plunge under 5 minutes.")
        }
        if (tempC < 1f) add("Near-freezing water: shorten the plunge and make sure someone is with you.")
        if (durationSec <= 60 && tempC >= 10f) add("Good starting point. Add time gradually over several sessions.")
    }

    fun heatNotes(tempC: Float, durationSec: Int): List<String> = buildList {
        if (tempC >= 95f && durationSec > 15 * 60) {
            add("Above 95 °C, sessions over 15 minutes are hard on the body. Step out if you feel dizzy.")
        }
        if (durationSec > 25 * 60) add("Long sessions increase dehydration — drink water before and after.")
    }

    val essentials = listOf(
        "Never cold plunge alone, and never hold your breath under cold water.",
        "The first 30–60 seconds trigger a gasp reflex. Enter slowly and breathe out long and slow.",
        "Get out right away if you feel dizzy, confused, numb, or start shivering hard.",
        "Skip alcohol before and during sessions, and rehydrate afterwards.",
        "Talk to a doctor first if you are pregnant or have a heart condition, high blood pressure, or Raynaud’s.",
    )
}
