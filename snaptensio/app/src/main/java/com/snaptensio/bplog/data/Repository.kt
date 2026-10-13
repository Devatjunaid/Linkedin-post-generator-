package com.snaptensio.bplog.data

import kotlinx.coroutines.flow.Flow

class Repository(private val db: AppDatabase) {
    private val profiles = db.profileDao()
    private val readings = db.readingDao()
    private val meds = db.medicationDao()
    private val reminders = db.reminderDao()

    fun observeProfiles(): Flow<List<Profile>> = profiles.observeAll()
    suspend fun allProfiles() = profiles.getAll()
    suspend fun addProfile(p: Profile) = profiles.insert(p)
    suspend fun updateProfile(p: Profile) = profiles.update(p)
    suspend fun deleteProfile(p: Profile) = profiles.delete(p)

    fun observeReadings(profileId: Long): Flow<List<Reading>> = readings.observeForProfile(profileId)
    suspend fun readingsFor(profileId: Long) = readings.getForProfile(profileId)
    suspend fun reading(id: Long) = readings.get(id)
    suspend fun saveReading(r: Reading): Long = if (r.id == 0L) readings.insert(r) else { readings.update(r); r.id }
    suspend fun insertReadings(list: List<Reading>) = readings.insertAll(list)
    suspend fun deleteReading(id: Long) = readings.delete(id)

    fun observeMedications(profileId: Long): Flow<List<Medication>> = meds.observeForProfile(profileId)
    suspend fun activeMedications() = meds.getAllActive()
    suspend fun medication(id: Long) = meds.get(id)
    suspend fun saveMedication(m: Medication): Long = if (m.id == 0L) meds.insert(m) else { meds.update(m); m.id }
    suspend fun deleteMedication(m: Medication) = meds.delete(m)
    fun observeDoseLogs(profileId: Long, fromDateKey: Int) = meds.observeDoseLogs(profileId, fromDateKey)
    suspend fun markTaken(log: DoseLog) = meds.insertDoseLog(log)
    suspend fun unmarkTaken(medicationId: Long, dateKey: Int, time: String) = meds.deleteDoseLog(medicationId, dateKey, time)

    fun observeReminders(): Flow<List<Reminder>> = reminders.observeAll()
    suspend fun allReminders() = reminders.getAll()
    suspend fun reminder(id: Long) = reminders.get(id)
    suspend fun saveReminder(r: Reminder): Long = if (r.id == 0L) reminders.insert(r) else { reminders.update(r); r.id }
    suspend fun deleteReminder(r: Reminder) = reminders.delete(r)
}
