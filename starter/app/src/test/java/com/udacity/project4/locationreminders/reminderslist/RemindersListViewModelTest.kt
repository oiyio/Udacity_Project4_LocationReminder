package com.udacity.project4.locationreminders.reminderslist

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class RemindersListViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get: Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val fakeDataSource = FakeDataSource()

    private lateinit var viewModel: RemindersListViewModel


    @Before
    fun setupViewModel() {
        stopKoin()

        viewModel =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)

        runBlockingTest {
            fakeDataSource.deleteAllReminders()
        }
    }

    /*
    * Test case : When we load reminders, showLoading is initially true, then becomes false.
    * */
    @Test
    fun loadReminders_showLoading() = runBlockingTest {
        val reminder1 = ReminderDTO("mytitle1", "mydescription1", "mylocation1", 5.0, 6.0)
        fakeDataSource.saveReminder(reminder1)

        mainCoroutineRule.pauseDispatcher() // We pause dispatcher. In this way, we can verify initial values.

        viewModel.loadReminders()

        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))
        mainCoroutineRule.resumeDispatcher()
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))

        assertThat(viewModel.showNoData.getOrAwaitValue(), `is`(false))
    }

    /*
    * Test case : save two reminders and check that.
    * */
    @Test
    fun loadReminders_saveReminder() = runBlockingTest {
        val reminder1 = ReminderDTO("mytitle1", "mydescription1", "mylocation1", 5.0, 6.0)
        val reminder2 = ReminderDTO("mytitle2", "mydescription2", "mylocation3", 7.0, 8.0)

        fakeDataSource.saveReminder(reminder1)
        fakeDataSource.saveReminder(reminder2)

        viewModel.loadReminders()

        val remindersList = viewModel.remindersList.getOrAwaitValue()
        assertThat(remindersList.size, `is`(2))
        assertThat(viewModel.remindersList.getOrAwaitValue(), `is`(remindersList))
        assertThat(remindersList[0].id, `is`(reminder1.id))
        assertThat(remindersList[0].title, `is`(reminder1.title))
        assertThat(remindersList[0].description, `is`(reminder1.description))
        assertThat(remindersList[1].id, `is`(reminder2.id))
        assertThat(remindersList[1].title, `is`(reminder2.title))
        assertThat(remindersList[1].description, `is`(reminder2.description))

        assertThat(viewModel.remindersList.getOrAwaitValue().isEmpty(), `is`(false))
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))
        assertThat(viewModel.showNoData.getOrAwaitValue(), `is`(false))
    }

    /*
    * Test case : Error occurs while loading reminders.
    * */
    @Test
    fun loadReminders_loadingError() = runBlockingTest {
        fakeDataSource.setReturnError(true)

        viewModel.loadReminders()

        assertThat(
            viewModel.showSnackBar.getOrAwaitValue(),
            `is`("Error occurred in FakeDataSource")
        )
        assertThat(viewModel.showNoData.getOrAwaitValue(), `is`(true))
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))
    }
}