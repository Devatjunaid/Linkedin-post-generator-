package com.superdevlabs.petfood.portioncalc.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.superdevlabs.petfood.portioncalc.MainActivity
import com.superdevlabs.petfood.portioncalc.PetFoodApp
import com.superdevlabs.petfood.portioncalc.R
import com.superdevlabs.petfood.portioncalc.data.PetEntity
import com.superdevlabs.petfood.portioncalc.domain.Format
import com.superdevlabs.petfood.portioncalc.domain.NutritionCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Meal-time reminders. Uses inexact, battery-friendly alarms (no exact-alarm permission),
 * so a reminder may arrive a few minutes late, which is fine for feeding.
 */
class ReminderScheduler(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.reminder_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Re-arms every meal of this pet, or cancels them when reminders are off. */
    fun sync(pet: PetEntity) {
        for (meal in 0 until MAX_MEALS) cancel(pet.id, meal)
        if (!pet.remindersEnabled) return
        pet.mealTimeMinutes().forEachIndexed { meal, minutes -> schedule(pet.id, meal, minutes) }
    }

    fun cancelAll(petId: Long) {
        for (meal in 0 until MAX_MEALS) cancel(petId, meal)
    }

    fun schedule(petId: Long, meal: Int, minutesOfDay: Int) {
        val now = LocalDateTime.now()
        var at = LocalDate.now().atTime(LocalTime.of(minutesOfDay / 60, minutesOfDay % 60))
        if (!at.isAfter(now)) at = at.plusDays(1)
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pendingIntent(petId, meal, create = true)!!)
    }

    private fun cancel(petId: Long, meal: Int) {
        pendingIntent(petId, meal, create = false)?.let {
            alarms.cancel(it)
            it.cancel()
        }
    }

    private fun pendingIntent(petId: Long, meal: Int, create: Boolean): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_MEAL)
            .putExtra(EXTRA_PET, petId)
            .putExtra(EXTRA_MEAL, meal)
        val flags = PendingIntent.FLAG_IMMUTABLE or
            if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
        return PendingIntent.getBroadcast(context, requestCode(petId, meal), intent, flags)
    }

    companion object {
        const val CHANNEL_ID = "feeding_reminders"
        const val ACTION_MEAL = "com.superdevlabs.petfood.portioncalc.MEAL_REMINDER"
        const val EXTRA_PET = "pet_id"
        const val EXTRA_MEAL = "meal_index"
        const val MAX_MEALS = 4

        fun requestCode(petId: Long, meal: Int) = (petId * MAX_MEALS + meal).toInt()

        fun mealName(meal: Int, meals: Int): String = when (meals) {
            1 -> "Daily meal"
            2 -> if (meal == 0) "Breakfast" else "Dinner"
            3 -> listOf("Breakfast", "Lunch", "Dinner")[meal]
            else -> "Meal ${meal + 1}"
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val petId = intent.getLongExtra(ReminderScheduler.EXTRA_PET, -1)
        val meal = intent.getIntExtra(ReminderScheduler.EXTRA_MEAL, -1)
        if (petId < 0 || meal < 0) return
        val container = (context.applicationContext as PetFoodApp).container
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val pet = container.pets.getPet(petId) ?: return@launch
                if (!pet.remindersEnabled || meal >= pet.mealsPerDay) return@launch
                // Re-arm for tomorrow before anything else so the chain never breaks.
                container.reminders.schedule(pet.id, meal, pet.mealTimeMinutes()[meal])
                val today = LocalDate.now().toEpochDay()
                if (container.pets.isMealFed(pet.id, today, meal)) return@launch
                notify(context, pet, meal, portionText(container, pet))
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun portionText(container: com.superdevlabs.petfood.portioncalc.AppContainer, pet: PetEntity): String {
        val energy = NutritionCalculator.energy(pet.toProfile())
        val primary = pet.primaryFoodId?.let { container.pets.getFood(it) }?.toInfo()
        val secondary = pet.secondaryFoodId?.let { container.pets.getFood(it) }?.toInfo()
        val portions = NutritionCalculator.portions(energy.foodKcal, primary, secondary, pet.secondarySharePercent)
        if (portions.isEmpty()) return "About ${energy.foodKcal / pet.mealsPerDay} kcal this meal."
        return portions.joinToString(" + ") { p ->
            val grams = p.gramsPerDay / pet.mealsPerDay
            val cups = p.cupsPerDay?.let { " (${Format.cups(it / pet.mealsPerDay)})" } ?: ""
            "${Format.grams(grams)}$cups ${p.food.name.substringBefore(" (").substringBefore(" ·")}"
        }
    }

    private fun notify(context: Context, pet: PetEntity, meal: Int, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val open = PendingIntent.getActivity(
            context,
            ReminderScheduler.requestCode(pet.id, meal),
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${ReminderScheduler.mealName(meal, pet.mealsPerDay)} for ${pet.name}")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ReminderScheduler.requestCode(pet.id, meal), notification)
    }
}

/** Alarms are cleared on reboot and on app update, so they are re-armed here. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val container = (context.applicationContext as PetFoodApp).container
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                container.pets.allPets().forEach { container.reminders.sync(it) }
            } finally {
                pending.finish()
            }
        }
    }
}
