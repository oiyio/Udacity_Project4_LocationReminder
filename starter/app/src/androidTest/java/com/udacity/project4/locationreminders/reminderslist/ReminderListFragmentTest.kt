package com.udacity.project4.locationreminders.reminderslist

import android.content.Context
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito
import org.mockito.Mockito.verify

/*
*  🔥 🦍 🦕  HEY YOU!!! TURN OFF THE ANIMATION SETTINGS IN YOUR DEVICE. I forgot it and spend many hours to find the reason.
* */
@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest {


    private lateinit var repository: ReminderDataSource

    @get: Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val testModules = module {
        // single<Application> { getApplicationContext() }   // if you want Application instance, getApplicationContext() will give you it.
        // single<Context> { getApplicationContext() }      //  if you want Application instance, getApplicationContext() will give you it.

        viewModel {
            RemindersListViewModel(
                    get(), // Application instance
                    get()    // if you want instance of ReminderDataSource interface, i will give you instance of RemindersLocalRepository.
            )
        }

        viewModel {
            SaveReminderViewModel(
                    get(), // Application instance
                    get()  // if you want instance of ReminderDataSource interface, i will give you instance of RemindersLocalRepository.
            )
        }

        single { LocalDB.createRemindersDao(get()) }  // if you want instance of RemindersDao, i will give you it by calling LocalDB.createRemindersDao( context )
        /*alternatively you write an in-memory roomdb as the following
        * single {
            Room.inMemoryDatabaseBuilder(
                    getApplicationContext(),
                    RemindersDatabase::class.java
            ).allowMainThreadQueries().build().reminderDao()
        }
        * */

        single<ReminderDataSource> { RemindersLocalRepository(get()) }   // if you want instance of ReminderDataSource interface, i will give you instance of RemindersLocalRepository.
        // single{ RemindersLocalRepository(get()) as ReminderDataSource } // you can do the above line with this line instead. They are the same.
    }

    @Before
    fun setup() {
        stopKoin()

        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(listOf(testModules))
        }

        repository = GlobalContext.get().koin.get()

        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }


    //At first launch of ReminderListFragment, there is no reminder. Thus "No Data" is shown.
    @Test
    fun at_first_launch_there_is_no_reminder_thus_no_Data_text_is_shown_in_textView() {
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        onView(ViewMatchers.withText(getApplicationContext<Context>().getString(R.string.no_data))).check(
                ViewAssertions.matches(
                        ViewMatchers.isDisplayed()
                )
        )
    }

    //When we click fab button, open ReminderListFragment.
    @Test
    fun when_fab_is_clicked_navigate_to_ReminderListFragment2() {
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = Mockito.mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        onView(withId(R.id.addReminderFAB)).perform(click())
        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }


    @Test
    fun add_two_reminders_and_navigate_to_SaveReminderFragment() {
        val reminderDto1 = ReminderDTO(
                title = "My Title 1",
                description = "My Description 1",
                location = "mylocation 1",
                latitude = 13.1,
                longitude = 14.1,
                id = "1"
        )
        val reminderDto2 = ReminderDTO(
                title = "My Title 2",
                description = "My Description 2",
                location = "mylocation 2",
                latitude = 10.1,
                longitude = 12.1,
                id = "2"
        )

        runBlocking {
            repository.saveReminder(reminderDto1)
            repository.saveReminder(reminderDto2)
        }

        // if we click addReminderFAB, navigationComponent works and ReminderListFragment is opened
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = Mockito.mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
        onView(withId(R.id.addReminderFAB)).perform(click())

        // verify ReminderListFragment is opened
        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
        onView(ViewMatchers.withText(reminderDto1.title)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        onView(ViewMatchers.withText(reminderDto1.description)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        onView(ViewMatchers.withText(reminderDto1.location)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        onView(ViewMatchers.withText(reminderDto2.title)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        onView(ViewMatchers.withText(reminderDto2.description)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        onView(ViewMatchers.withText(reminderDto2.location)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
}