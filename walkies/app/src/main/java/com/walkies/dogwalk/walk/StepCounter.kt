package com.walkies.dogwalk.walk

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Reads the hardware step counter (total steps since boot). Because the total keeps counting
 * while the app is in the background, a walk only needs the reading at its start and its end, so
 * no background location or foreground service is needed.
 */
class StepCounter(context: Context) {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val sensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    val isAvailable: Boolean get() = sensor != null

    fun totalSteps(): Flow<Long> {
        val manager = sensorManager ?: return emptyFlow()
        val stepSensor = sensor ?: return emptyFlow()
        return callbackFlow {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    trySend(event.values[0].toLong())
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            manager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
            awaitClose { manager.unregisterListener(listener) }
        }
    }

    companion object {
        /** Average adult stride; the distance shown is labelled as an estimate. */
        const val METERS_PER_STEP = 0.75
    }
}
