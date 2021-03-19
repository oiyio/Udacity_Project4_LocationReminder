package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    private lateinit var database: RemindersDatabase
    private lateinit var repository: RemindersLocalRepository

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private val reminderDto1 = ReminderDTO(
            title = "My Title 1",
            description = "My Description 1",
            location = "mylocation 1",
            latitude = 13.1,
            longitude = 14.1,
            id = "1"
    )
    private val reminderDto2 = ReminderDTO(
            title = "My Title 2",
            description = "My Description 2",
            location = "mylocation 2",
            latitude = 10.1,
            longitude = 12.1,
            id = "2"
    )

    @Before
    fun init_database() {
        database = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        repository = RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @After
    fun close_database() {
        database.close()
    }

    @Test
    fun save__and_get_reminder() = runBlocking {
        repository.saveReminder(reminderDto1)

        // with Truth, repository.getReminders()
        val reminderList = repository.getReminders()

        Truth.assertThat(reminderList).isInstanceOf(Result.Success::class.java)
        reminderList as Result.Success // cast result to Result.Success

        Truth.assertThat(reminderList.data).isNotEmpty()
        Truth.assertThat(reminderList.data).hasSize(1)


        // with Hamcrest, repository.getReminder(id)
        val reminderTakenFromRepo = repository.getReminder(reminderDto1.id)

        assertThat(reminderTakenFromRepo, not(nullValue()))
        Truth.assertThat(reminderList).isInstanceOf(Result.Success::class.java)
        reminderTakenFromRepo as Result.Success // cast result to Result.Success

        assertThat(reminderTakenFromRepo.data.id, `is`(reminderDto1.id))
        assertThat(reminderTakenFromRepo.data.title, `is`(reminderDto1.title))
        assertThat(reminderTakenFromRepo.data.description, `is`(reminderDto1.description))
        assertThat(reminderTakenFromRepo.data.latitude, `is`(reminderDto1.latitude))
        assertThat(reminderTakenFromRepo.data.longitude, `is`(reminderDto1.longitude))
        assertThat(reminderTakenFromRepo.data.location, `is`(reminderDto1.location))
    }

    @Test
    fun save_2_reminders_and_get() = runBlocking {
        repository.saveReminder(reminderDto1)
        repository.saveReminder(reminderDto2)

        val remindersListFromRepo = repository.getReminders() as Result.Success
        assertThat(remindersListFromRepo.data.size, `is`(2))
        assertThat(remindersListFromRepo.data[0].id, `is`(reminderDto1.id))
        assertThat(remindersListFromRepo.data[1].id, `is`(reminderDto2.id))
    }
}