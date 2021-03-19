package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.google.common.truth.Truth
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    private lateinit var database: RemindersDatabase
    private lateinit var dao: RemindersDao

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

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
    fun initDatabase() {
        database = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        dao = database.reminderDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insert_into_db() = runBlockingTest {
        dao.saveReminder(reminderDto1)

        Truth.assertThat(dao.getReminders()).hasSize(1)
        Truth.assertThat(dao.getReminders()).contains(reminderDto1)

        // THEN - The loaded data contains the expected values.
        val reminderTakenFromDb = dao.getReminderById(reminderDto1.id)
        assertThat(reminderTakenFromDb as ReminderDTO, notNullValue())


        assertThat(reminderTakenFromDb.id, `is`(reminderDto1.id))
        assertThat(reminderTakenFromDb.title, `is`(reminderDto1.title))
        assertThat(reminderTakenFromDb.description, `is`(reminderDto1.description))
        assertThat(reminderTakenFromDb.location, `is`(reminderDto1.location))
        assertThat(reminderTakenFromDb.latitude, `is`(reminderDto1.latitude))
        assertThat(reminderTakenFromDb.longitude, `is`(reminderDto1.longitude))
    }


    @Test
    fun insert_2_reminder_into_db() = runBlockingTest {
        // GIVEN - Insert two reminders
        val dao = database.reminderDao()

        dao.saveReminder(reminderDto1)
        dao.saveReminder(reminderDto2)

        // WHEN - Get the reminders from the database.
        val remindersList = dao.getReminders()

        // THEN - The reminders' table is not empty.
        assertThat(remindersList.isNotEmpty(), `is`(true))
        Truth.assertThat(dao.getReminders()).hasSize(2)
        Truth.assertThat(remindersList).contains(reminderDto1)
        Truth.assertThat(remindersList).contains(reminderDto2)
    }

    @Test
    fun get_reminder_from_db() = runBlockingTest {
        val reminderDto = ReminderDTO("MyTitle", "MyDescription", "mylocation", 13.1, 13.1)
        dao.saveReminder(reminderDto)

        val reminderTakenFromDb = dao.getReminderById(reminderDto.id)

        assertThat(reminderTakenFromDb as ReminderDTO, notNullValue())  // 🔥 if you comment this line, the below you must handle reminderTakenFromDb?.title etc. This line saves me from that boilerplate
        Truth.assertThat(reminderTakenFromDb).isNotNull()
        Truth.assertThat(reminderTakenFromDb.title).isEqualTo(reminderDto.title)
        Truth.assertThat(reminderTakenFromDb.description).isEqualTo(reminderDto.description)
        Truth.assertThat(reminderTakenFromDb.location).isEqualTo(reminderDto.location)
        Truth.assertThat(reminderTakenFromDb.latitude).isEqualTo(reminderDto.latitude)
        Truth.assertThat(reminderTakenFromDb.longitude).isEqualTo(reminderDto.longitude)
    }

    @Test
    fun delete_all_reminders() = runBlockingTest {
        // GIVEN - Insert two reminders
        dao.saveReminder(reminderDto1)
        dao.saveReminder(reminderDto2)

        // WHEN - Delete all reminders from the database.
        dao.deleteAllReminders()
        val remindersList = dao.getReminders()

        // THEN - The reminders' table is empty.
        assertThat(remindersList.isEmpty(), `is`(true))
        Truth.assertThat(dao.getReminders()).hasSize(0)
    }

}