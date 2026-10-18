package com.devatjunaid.balconysolar.sensors

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.devatjunaid.balconysolar.domain.Fmt
import com.devatjunaid.balconysolar.ui.graphics.CompassGraphic
import com.devatjunaid.balconysolar.ui.graphics.TiltGauge
import com.devatjunaid.balconysolar.ui.screens.MeasureKind
import com.devatjunaid.balconysolar.ui.theme.Palette
import com.devatjunaid.balconysolar.ui.theme.Type
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Live measurement with the phone's sensors.
 * Tilt: lay the phone flat on the panel glass; the angle between the screen and the horizontal is the tilt.
 * Direction: hold the phone flat with its top edge pointing the way the panel faces; the magnetic
 * heading is corrected to true north with the World Magnetic Model for your coordinates.
 */
@Composable
fun MeasureDialog(kind: MeasureKind, latitude: Double, longitude: Double, onDismiss: () -> Unit, onUse: (Double) -> Unit) {
    val context = LocalContext.current
    var value by remember { mutableStateOf<Double?>(null) }
    var accuracyLow by remember { mutableStateOf(false) }
    val available = remember(kind) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        when (kind) {
            MeasureKind.TILT -> sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
            MeasureKind.AZIMUTH -> sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
        }
    }

    DisposableEffect(kind) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val declination = GeomagneticField(latitude.toFloat(), longitude.toFloat(), 0f, System.currentTimeMillis()).declination
        var sx = 0.0
        var sy = 0.0
        var gx = 0.0; var gy = 0.0; var gz = 0.0
        var first = true
        val rotation = FloatArray(9)
        val orientation = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                when (e.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        // Low-pass filter isolates gravity.
                        val a = if (first) 1.0 else 0.12
                        first = false
                        gx += a * (e.values[0] - gx); gy += a * (e.values[1] - gy); gz += a * (e.values[2] - gz)
                        val g = sqrt(gx * gx + gy * gy + gz * gz)
                        if (g > 1e-3) value = Math.toDegrees(acos((kotlin.math.abs(gz) / g).coerceIn(0.0, 1.0)))
                    }
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotation, e.values)
                        SensorManager.getOrientation(rotation, orientation)
                        val heading = Math.toDegrees(orientation[0].toDouble()) + declination
                        // Circular smoothing so 359° → 1° does not jump through 180°.
                        val r = Math.toRadians(heading)
                        val a = if (first) 1.0 else 0.15
                        first = false
                        sx += a * (cos(r) - sx); sy += a * (sin(r) - sy)
                        value = (Math.toDegrees(atan2(sy, sx)) + 360) % 360
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                if (sensor.type == Sensor.TYPE_ROTATION_VECTOR || sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    accuracyLow = accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW
                }
            }
        }
        val sensor = when (kind) {
            MeasureKind.TILT -> sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            MeasureKind.AZIMUTH -> sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.SurfaceHigh,
        title = { Text(if (kind == MeasureKind.TILT) "Measure tilt" else "Measure direction", style = Type.title, color = Palette.Ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (!available) {
                    Text("This phone has no suitable sensor. Enter the value with the slider instead.", style = Type.body, color = Palette.Muted)
                } else {
                    val v = value ?: 0.0
                    if (kind == MeasureKind.TILT) TiltGauge(v, Modifier.fillMaxWidth().height(130.dp))
                    else CompassGraphic(v, Modifier.fillMaxWidth().height(150.dp), live = true)
                    Text(
                        if (value == null) "Reading sensor…" else if (kind == MeasureKind.TILT) Fmt.degrees(v) else "${Fmt.compassLong(v)} · ${Fmt.degrees(v)}",
                        style = Type.title, color = Palette.Amber,
                    )
                    Text(
                        if (kind == MeasureKind.TILT) "Lay the phone flat on the panel's glass, screen up, and wait for the number to settle."
                        else "Hold the phone flat, top edge pointing the way the panel faces. Keep it away from metal railings. True north is corrected automatically.",
                        style = Type.label, color = Palette.Muted,
                    )
                    if (accuracyLow && kind == MeasureKind.AZIMUTH) {
                        Text("Compass accuracy is low: move the phone in a figure-eight to calibrate.", style = Type.label, color = Palette.Coral)
                    }
                }
            }
        },
        confirmButton = {
            val v = value
            TextButton(enabled = available && v != null, onClick = { v?.let { onUse(if (kind == MeasureKind.TILT) it.roundToInt().toDouble().coerceIn(0.0, 90.0) else (it.roundToInt() % 360).toDouble()) } }) {
                Text("Use this value", color = if (available && v != null) Palette.Amber else Palette.Faint)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Palette.Muted) } },
    )
}
