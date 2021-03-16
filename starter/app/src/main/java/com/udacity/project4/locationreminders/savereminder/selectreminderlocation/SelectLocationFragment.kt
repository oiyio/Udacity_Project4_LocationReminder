package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()

    private val fusedLocationProviderClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireContext())
    }

    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var googleMap: GoogleMap

    private val TAG = SelectLocationFragment::class.java.simpleName

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        val mapFragment = childFragmentManager.findFragmentById(R.id.fragmentMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        checkPermissions()

//        TODO: add style to the map
//        TODO: put a marker to location that the user selected


        binding.buttonSave.setOnClickListener {
            onLocationSelected()
        }

        return binding.root
    }

    private fun onLocationSelected() {
        //        TODO: When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        fun setMapType(mapType: Int): Boolean {
            googleMap.mapType = mapType
            return true
        }

        return when (item.itemId) {
            R.id.normal_map -> setMapType(GoogleMap.MAP_TYPE_NORMAL)
            R.id.hybrid_map -> setMapType(GoogleMap.MAP_TYPE_HYBRID)
            R.id.terrain_map -> setMapType(GoogleMap.MAP_TYPE_TERRAIN)
            R.id.satellite_map -> setMapType(GoogleMap.MAP_TYPE_SATELLITE)

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onMapReady(p0: GoogleMap) {
        googleMap = p0

        setMapStyle(googleMap)
        moveCameraToCurrentLocation()

    }

    @SuppressLint("MissingPermission")
    private fun moveCameraToCurrentLocation() {
        if (foregroundAndBackgroundLocationPermissionGranted(requireContext())) {
            googleMap.isMyLocationEnabled = true  // shows my location button at top right

            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
                }
            }
        }
    }

    /*
     *  Uses the Location Client to check the current state of location settings, and gives the user
     *  the opportunity to turn on location services within our app.
     */
    private fun checkDeviceLocationSettings(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

        // add a failure listener
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                // Location settings are not satisfied, but this can be fixed by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult().
                    exception.startResolutionForResult(requireActivity(), REQUEST_TURN_DEVICE_LOCATION_ON)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(requireActivity().findViewById(android.R.id.content), R.string.location_required_error, Snackbar.LENGTH_INDEFINITE)
                        .setAction(android.R.string.ok) {
                            checkDeviceLocationSettings()
                        }.show()
            }
        }

        // add a complete listener
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                moveCameraToCurrentLocation()
            }
        }
    }

    /*
    * called only from from onCreateView()
    * */
    @SuppressLint("MissingPermission")
    private fun checkPermissions() {
        if (foregroundAndBackgroundLocationPermissionGranted(requireContext())) {
            checkDeviceLocationSettings()
        } else {
            requestForegroundAndBackgroundLocationPermissions(this)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            if (foregroundAndBackgroundLocationPermissionGranted(requireContext())) {
                checkDeviceLocationSettings(false)
            } else {
                requestForegroundAndBackgroundLocationPermissions(this)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        fun showSnackbarWithSettingsAction() {
            Snackbar.make(requireActivity().findViewById(android.R.id.content), R.string.permission_denied_explanation, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.settings) {
                        // Launches App settings screen.
                        startActivity(
                                Intent().apply {
                                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                    }.show()
        }

        fun isPermissionDenied(_requestCode: Int): Boolean {
            return grantResults.isEmpty() || grantResults[0] == PackageManager.PERMISSION_DENIED ||
                    (requestCode == _requestCode && grantResults[0] == PackageManager.PERMISSION_DENIED)
        }

        if (isPermissionDenied(REQUEST_FOREGROUND_ONLY_PERMISSION_REQUEST_CODE)) {
            showSnackbarWithSettingsAction()
        } else if (isPermissionDenied(REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE)) {
            showSnackbarWithSettingsAction()
        } else {
            checkDeviceLocationSettings()
        }
    }

    // style your map.
    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            requireContext(),
                            R.raw.map_style
                    )
            )

            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }
}