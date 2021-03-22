package com.udacity.project4.locationreminders.savereminder


import android.content.Context
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.O_MR1])
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    @get: Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get: Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val fakeDataSource = FakeDataSource()

    private lateinit var saveReminderViewModel: SaveReminderViewModel


    private val reminderDataItem1 = ReminderDataItem(
        title = "My Title 1",
        description = "My Description 1",
        location = "mylocation 1",
        latitude = 13.1,
        longitude = 14.1,
        id = "1"
    )
    private val reminderDataItem2 = ReminderDataItem(
        title = "My Title 2",
        description = "My Description 2",
        location = "mylocation 2",
        latitude = 10.1,
        longitude = 12.1,
        id = "2"
    )


    @Before
    fun setupViewModel() {
        stopKoin()

        saveReminderViewModel =
            SaveReminderViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
        /* Alternatively, We can mock the Application instance and create saveReminderViewModel instance as the following.
        val applicationMock = Mockito.mock(Application::class.java)
        saveReminderViewModel = SaveReminderViewModel(applicationMock, fakeDataSource)*/
    }

    @Test
    fun saveReminder_resultSuccess() = runBlockingTest {
        saveReminderViewModel.saveReminder(reminderDataItem1)

        val resultReminderDataItem1 =
            fakeDataSource.getReminder(reminderDataItem1.id) as Result.Success
        assertThat(resultReminderDataItem1.data.id, `is`(reminderDataItem1.id))
        assertThat(resultReminderDataItem1.data.title, `is`(reminderDataItem1.title))
        assertThat(resultReminderDataItem1.data.description, `is`(reminderDataItem1.description))

        saveReminderViewModel.saveReminder(reminderDataItem2)
        val resultReminderDataItem2 =
            fakeDataSource.getReminder(reminderDataItem2.id) as Result.Success
        assertThat(resultReminderDataItem2.data.id, `is`(reminderDataItem2.id))
        assertThat(resultReminderDataItem2.data.title, `is`(reminderDataItem2.title))
        assertThat(resultReminderDataItem1.data.description, `is`(reminderDataItem1.description))
    }

    @Test
    fun save_reminder_progress_bar() = runBlockingTest {
        mainCoroutineRule.pauseDispatcher()
        saveReminderViewModel.saveReminder(reminderDataItem1)
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(true))

        mainCoroutineRule.resumeDispatcher()
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(false))
        assertThat(
            saveReminderViewModel.navigationCommand.getOrAwaitValue(),
            `is`(NavigationCommand.Back)
        )
        assertThat(
            saveReminderViewModel.showToast.getOrAwaitValue(),
            `is`(
                ApplicationProvider.getApplicationContext<Context>()
                    .getString(R.string.reminder_saved)
            )
        )
    }

    @Test
    fun saveReminder__showLoading_toastMessage_navigateCommandBack() = runBlockingTest {
        saveReminderViewModel.saveReminder(reminderDataItem1)

        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(false))
        assertThat(
            saveReminderViewModel.showToast.getOrAwaitValue(),
            `is`(
                ApplicationProvider.getApplicationContext<Context>()
                    .getString(R.string.reminder_saved)
            )
        )
        assertThat(
            saveReminderViewModel.navigationCommand.getOrAwaitValue(),
            `is`(NavigationCommand.Back)
        )
    }

    @Test
    fun validateEnteredData() = runBlockingTest {
        // if the user fills all required fields, validateEnteredData returns true.
        assertThat(
            saveReminderViewModel.validateEnteredData(reminderDataItem1),
            `is`(true)
        )

        // if the user does not fill title and click save button, validateEnteredData returns false and snackBar is shown.
        val reminderDataItem1 = ReminderDataItem(
            title = null,
            description = "My Description 1",
            location = "mylocation 1",
            latitude = 13.1,
            longitude = 14.1,
            id = "1"
        )
        assertThat(
            saveReminderViewModel.validateEnteredData(reminderDataItem1),
            `is`(false)
        )
        assertThat(
            saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
            `is`(R.string.err_enter_title)
        )


        // if the user does not select location and click save button, validateEnteredData returns false and snackBar is shown.
        val reminderDataItem2 = ReminderDataItem(
            title = "mytitle",
            description = "My Description 1",
            location = null,
            latitude = 13.1,
            longitude = 14.1,
            id = "1"
        )
        assertThat(saveReminderViewModel.validateEnteredData(reminderDataItem2), `is`(false))
        assertThat(
            saveReminderViewModel.showSnackBarInt.getOrAwaitValue(),
            `is`(R.string.err_select_location)
        )
    }

    @Test
    fun validateAndSaveReminder_invalidReminder() = runBlockingTest {
        val reminder1 = ReminderDataItem("mytitle", "mydescription", "mylocation", 13.0, 15.0)
        assertThat(saveReminderViewModel.validateAndSaveReminder(reminder1), `is`(true))
        val resultReminderDataItem1 = fakeDataSource.getReminder(reminder1.id) as Result.Success
        assertThat(resultReminderDataItem1.data.id, `is`(reminder1.id))

        val reminder2 = ReminderDataItem(null, "mydescription", "mylocation", 5.0, 8.0)
        assertThat(saveReminderViewModel.validateAndSaveReminder(reminder2), `is`(false))
        val resultReminderDataItem2 = fakeDataSource.getReminder(reminder2.id) as Result.Error
        assertThat(resultReminderDataItem2.message, `is`("Reminder not found"))
        Truth.assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue())
            .isEqualTo(R.string.err_enter_title)
    }
}