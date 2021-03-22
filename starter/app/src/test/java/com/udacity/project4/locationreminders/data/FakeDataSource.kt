package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(private val reminders: MutableList<ReminderDTO> = mutableListOf()) :
    ReminderDataSource {

    private var shouldReturnError = false

    override suspend fun saveReminder(reminderDto: ReminderDTO) {
        reminders.add(reminderDto)
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return if (shouldReturnError) {
            Result.Error("Error occurred in FakeDataSource")
        } else {
            Result.Success(reminders)
        }
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        val reminderDto = reminders.find { it.id == id }
        return when {
            shouldReturnError -> Result.Error("Error occurred while getting reminder")
            reminderDto == null -> Result.Error("Error - No reminder found with this id")
            else -> Result.Success(reminderDto)
        }
    }

    override suspend fun deleteAllReminders() {
        reminders.clear()
    }

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

}