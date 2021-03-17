package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
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
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
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
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.locationreminders.savereminder.checkDeviceLocationSettings
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

        binding.buttonSave.setOnClickListener {
            _viewModel.navigationCommand.value = NavigationCommand.Back
        }

        return binding.root
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
        setLongClick(googleMap)
        setPoiClick(googleMap)
        moveCameraToCurrentLocation()
    }

    private fun setLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            map.clear()

            map.addMarker(latLng, resources.getString(R.string.selected_location)).showInfoWindow()
            setSelectedLocation(latLng, resources.getString(R.string.selected_location))
        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            map.clear()

            map.addMarker(poi.latLng, poi.name).showInfoWindow()
            setSelectedLocation(poi.latLng, poi.name, poi)
        }
    }

    private fun setSelectedLocation(latLng: LatLng, name: String, poi: PointOfInterest? = null) {
        _viewModel.latitude.value = latLng.latitude
        _viewModel.longitude.value = latLng.longitude

        _viewModel.selectedPOI.value = poi
        _viewModel.reminderSelectedLocationStr.value = name
    }

    private fun GoogleMap.addMarker(latLng: LatLng, title: String): Marker {
        return addMarker(
                MarkerOptions()
                        .position(latLng)
                        .title(title)
        )
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
    * called only from from onCreateView()
    * */
    private fun checkPermissions() {
        if (foregroundAndBackgroundLocationPermissionGranted(requireContext())) {
            checkDeviceLocationSettings(
                    activity = requireActivity(),
                    lambda = {
                        moveCameraToCurrentLocation()
                    }
            )
        } else {
            requestForegroundLocationPermission(this)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            if (foregroundAndBackgroundLocationPermissionGranted(requireContext())) {
                checkDeviceLocationSettings(
                        activity = requireActivity(),
                        resolve = false,
                        lambda = {
                            moveCameraToCurrentLocation()
                        }
                )
            } else {
                requestForegroundLocationPermission(this)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if(isPermissionGranted(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)){
            if(isPermissionGranted(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION)){
                checkDeviceLocationSettings(
                        activity = requireActivity(),
                        resolve = false,
                        lambda = {
                            moveCameraToCurrentLocation()
                        }
                )
            }
            else{
                requestBackgroundLocationPermission(this)
            }
        }
        else{
            showSnackbarWithSettingsAction(requireActivity())
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