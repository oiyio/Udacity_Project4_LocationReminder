package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.savereminder.selectreminderlocation.REQUEST_TURN_DEVICE_LOCATION_ON
import com.udacity.project4.locationreminders.savereminder.selectreminderlocation.foregroundAndBackgroundLocationPermissionGranted
import com.udacity.project4.locationreminders.savereminder.selectreminderlocation.requestForegroundAndBackgroundLocationPermissions
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient
    /*private lateinit var viewModel: GeofenceViewModel*/

    private val TAG = this.javaClass.name

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        geofencingClient = LocationServices.getGeofencingClient(requireContext())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this

        binding.selectLocation.setOnClickListener {
            //Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                    NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {

            val reminderData = _viewModel.getReminderDataItem()
            if (_viewModel.validateAndSaveReminder(reminderData)) {
                checkPermissions(reminderData)
            }
        }
    }


    private fun checkPermissions(reminderDataItem: ReminderDataItem) {
        if (foregroundAndBackgroundLocationPermissionGranted(requireContext())) {
            checkDeviceLocationSettings(activity = requireActivity(),
                    lambda = {
                        addGeofence(reminderDataItem)
                    })
        } else {
            requestForegroundAndBackgroundLocationPermissions(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    /*
     * Adds a Geofence for the current clue if needed, and removes any existing Geofence. This
     * method should be called after the user has granted the location permission.  If there are
     * no more geofences, we remove the geofence and let the viewmodel know that the ending hint
     * is now "active."
     */
    @SuppressLint("MissingPermission")
    private fun addGeofence(reminderDataItem: ReminderDataItem) {
        // Build the Geofence Object
        val geofence = Geofence.Builder()
                // Set the request ID, string to identify the geofence.
                .setRequestId(reminderDataItem.id)
                // Set the circular region of this geofence.
                .setCircularRegion(reminderDataItem.latitude!!,
                        reminderDataItem.longitude!!,
                        GeofencingConstants.GEOFENCE_RADIUS_IN_METERS
                )
                // Set the expiration duration of the geofence. This geofence gets
                // automatically removed after this period of time.
                .setExpirationDuration(GeofencingConstants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                // Set the transition types of interest. Alerts are only generated for these
                // transition. We track entry and exit transitions in this sample.
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()

        // Build the geofence request
        val geofencingRequest = GeofencingRequest.Builder()
                // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
                // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
                // is already inside that geofence.
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)

                // Add the geofences to be monitored by geofencing service.
                .addGeofence(geofence)
                .build()

        // First, remove any existing geofences that use our pending intent
        geofencingClient.removeGeofences(geofencePendingIntent)
                ?.run {
                    // Regardless of success/failure of the removal, add the new geofence
                    addOnCompleteListener {
                        // Add the new geofence request with the new geofence
                        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
                            addOnSuccessListener {
                                // Geofences added.
                                Log.e(TAG, "Add Geofence ${geofence.requestId}")
                                // Tell the viewmodel that we've reached the end of the game and
                                // activated the last "geofence" --- by removing the Geofence.
                                // viewModel.geofenceActivated()
                            }
                            addOnFailureListener {
                                // Failed to add geofences.
                                if ((it.message != null)) {
                                    Log.w(TAG, it.message!!)
                                }
                            }
                        }
                    }
                }
    }

    /**
     * Removes geofences. This method should be called after the user has granted the location
     * permission.
     */
    /*private fun removeGeofences() {
        if (!foregroundAndBackgroundLocationPermissionApproved()) {
            return
        }
        geofencingClient.removeGeofences(geofencePendingIntent)?.run {
            addOnSuccessListener {
                // Geofences removed
                Log.d(TAG, getString(R.string.geofences_removed))
                Toast.makeText(applicationContext, R.string.geofences_removed, Toast.LENGTH_SHORT)
                        .show()
            }
            addOnFailureListener {
                // Failed to remove geofences
                Log.d(TAG, getString(R.string.geofences_not_removed))
            }
        }
    }*/


    companion object {
        internal const val ACTION_GEOFENCE_EVENT =
                "SaveReminderFragment.action.ACTION_GEOFENCE_EVENT"
    }


}

internal object GeofencingConstants {
    val GEOFENCE_RADIUS_IN_METERS = 100f

    /**
     * Used to set an expiration time for a geofence. After this amount of time, Location services
     * stops tracking the geofence. For this sample, geofences expire after one hour.
     */
    val GEOFENCE_EXPIRATION_IN_MILLISECONDS: Long = TimeUnit.HOURS.toMillis(1)
}