package com.example.pointtopointroutingapp

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.location.LocationRequest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pointtopointroutingapp.databinding.ActivityMainBinding
import com.google.android.gms.maps.*
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.example.pointtopointroutingapp.CustomMarker
import com.example.pointtopointroutingapp.adapters.DestinationRecyclerAdapter
import com.example.pointtopointroutingapp.models.Destination
import com.example.pointtopointroutingapp.models.PolyLineData
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.maps.DirectionsApiRequest
import com.google.maps.GeoApiContext
import com.google.maps.PendingResult
import com.google.maps.android.collections.MarkerManager
import com.google.maps.internal.PolylineEncoding
import com.google.maps.model.DirectionsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class MainActivity : AppCompatActivity(),
    GoogleMap.OnCameraMoveStartedListener,
    GoogleMap.OnCameraMoveListener,
    OnMapReadyCallback {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mMap: GoogleMap
    private var mapFragment: SupportMapFragment? = null
    var location: Location? = null
    lateinit var clusterManager: ClusterManager<CustomMarker>
    lateinit var geoApiContext: GeoApiContext
    var mPolyData = ArrayList<PolyLineData>()
    var mPolyline: Polyline? = null
    var selectedMarker: Marker? = null

    var previousPinPoint: Marker? = null
    private val startSettingsForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {


            if (checkLocation()) displayMap()
            else toastMessage("Location still not enabled")

        }
    private var mFusedLocationClient: FusedLocationProviderClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRefreshMap.setOnClickListener {
            resetMap()
        }

        BottomSheetBehavior.from(binding.sheet).apply {
            peekHeight = 200
            this.state = BottomSheetBehavior.STATE_COLLAPSED
        }


        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    Toast.makeText(this, "ACCESS GRANTED", Toast.LENGTH_SHORT).show()
                    validateMapRequirement()
                } else {
                    Toast.makeText(this, "ACCESS DENIED", Toast.LENGTH_SHORT).show()
                }
            }

        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                validateMapRequirement()
            }
            shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION) -> {

            }
            else -> {
                Toast.makeText(this, "BLAH BLAH BLAH", Toast.LENGTH_SHORT).show()
                requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }


        }


    }

    fun toastMessage(message: String) {

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    }

    fun validateMapRequirement() {
        if (checkLocation()) displayMap()
        else startSettingsForResult.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }


    @SuppressLint("MissingPermission")
    fun displayMap() {
        if (mapFragment == null) {

            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

            val locationCallback=object:LocationCallback(){
                override fun onLocationResult(locationResult: LocationResult) {
                    val mLocation=locationResult.lastLocation
                    location=mLocation
                    Log.d("LASTLOCATION", "displayMap: ${mLocation!!.latitude}")
//                    Toast.makeText(this@MainActivity,"lat ${mLocation.latitude}",Toast.LENGTH_SHORT).show()

                    selectedMarker?.apply {

                        val desLat=position.latitude
                        val desLon=position.longitude
                        val bigLat=BigDecimal("$desLat")
                        val bigOriginLat=BigDecimal("${mLocation.latitude}")
                        val diffLat=bigLat.subtract(bigOriginLat).abs()

                        val bigLon=BigDecimal("$desLon")
                        val bigOriginLon=BigDecimal("${mLocation.longitude}")
                        val diffLon=bigLon.subtract(bigOriginLon).abs()

//                        Toast.makeText(this@MainActivity,"lat ${diffLat} lon ${diffLon}",Toast.LENGTH_SHORT).show()
                        if(diffLat<=BigDecimal("0.0000600") && diffLon<=(BigDecimal("0.0000600"))){
                            Toast.makeText(this@MainActivity,"You are at your destination",Toast.LENGTH_SHORT).show()
                            resetMap()
                        }
                    }



                }
            }
            val locationRequest=com.google.android.gms.location.LocationRequest.
            Builder(Priority.PRIORITY_HIGH_ACCURACY,3000).build()

            mFusedLocationClient!!.requestLocationUpdates(locationRequest,locationCallback,
                Looper.getMainLooper())

            mFusedLocationClient!!.lastLocation.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    location = task.result
                    if (mMap != null) {
                        setCameraView()
                        setupClusterer()
                    }
                    Log.d("Latitude", "displayMap: ${location!!.latitude-6.890618}")
                    Log.d("Longitude", "displayMap ${location!!.longitude-3.722744}")


                }
            }
            val options = GoogleMapOptions()
            mapFragment = SupportMapFragment.newInstance(options)
            supportFragmentManager.beginTransaction().replace(R.id.map, mapFragment!!).commit()
            mapFragment!!.getMapAsync(this)


        }
    }


    fun checkLocation(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(
            MarkerOptions().position(sydney)
                .title("Marker in Sydney")
        )
        if (location != null) {
            setCameraView()
            setupClusterer()
        }
        mMap.isMyLocationEnabled = true
        mMap.setOnCameraMoveListener {
            val cameraPos = mMap.cameraPosition
            val latitude = cameraPos.target.latitude
            val longitude = cameraPos.target.longitude
            if ((latitude < 41.3 && latitude > 41.1) && (longitude > -96.0 && longitude < -95.90)) {
                clusterManager.cluster()
            }

        }

        mMap.setOnPolylineClickListener {

            mPolyline?.color = ContextCompat.getColor(this, R.color.darkGrey)
            mPolyline?.zIndex = 0F
            it.color = ContextCompat.getColor(this, R.color.mBlue)
            mPolyline = it
            it.zIndex = 1F
            val polyData = mPolyData.find { it.polyline == mPolyline }


            selectedMarker!!.isVisible = false
            previousPinPoint?.remove()
            previousPinPoint = mMap.addMarker(
                MarkerOptions().position(selectedMarker!!.position)
                    .snippet(polyData!!.leg.duration.toString())
                    .title(selectedMarker!!.title)
            )


        }
//        mMap.setOnInfoWindowClickListener {
//            calculateDirection(it)
//        }
        geoApiContext = GeoApiContext.Builder()
            .apiKey("AIzaSyBae1961sKyOmV6O0wbzHDI0r6_G_Pd4TI")
            .build()

        val destinationAdapter = DestinationRecyclerAdapter(Constants.destinations)
        destinationAdapter.destinationOnClickListener =
            object : DestinationRecyclerAdapter.DestinationOnClickListener {
                override fun onClick(latitude: Double, longitude: Double) {
                    setCameraView(latitude, longitude,true)
                    BottomSheetBehavior.from(binding.sheet).apply {
                        peekHeight = 200
                        this.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                }

            }
        binding.rvDestinations.layoutManager = LinearLayoutManager(this)
        binding.rvDestinations.adapter = destinationAdapter


    }


    private fun calculateDirection(marker: Marker) {
        val TAG = "calculateDirections"
        val destination = com.google.maps.model.LatLng(
            marker.position.latitude,
            marker.position.longitude
        )
        val directions = DirectionsApiRequest(geoApiContext)
        directions.alternatives(true)
        directions.origin(
            com.google.maps.model.LatLng(
                location!!.latitude,
                location!!.longitude
            )
        )
        directions.destination(destination).setCallback(
            object : PendingResult.Callback<DirectionsResult> {
                override fun onResult(result: DirectionsResult?) {
                    Log.d(TAG, result!!.routes[0].toString())
                    Log.d(TAG, result!!.routes[0].legs[0].duration.toString())
                    Log.d(TAG, result!!.routes[0].legs[0].distance.toString())


                    showPolylineToMap(result!!)
                }

                override fun onFailure(e: Throwable?) {
                    Log.d(TAG, "ERRORRRR")
                }

            }
        )

    }

    private fun setCameraView(
        latitude: Double = 41.26443,
        longitude: Double = -95.94438,
//        latitude: Double = 6.894475,
//        longitude: Double = 3.724666,
        zoomIn:Boolean=false
    ) {


        val bottomBoundary = longitude!! - 0.1
        val leftBoundary = latitude!! - 0.1
        val topBoundary = longitude!! + 0.1
        val rightBoundary = latitude!! + 0.1
        val mapBoundary =
            LatLngBounds(LatLng(leftBoundary, bottomBoundary), LatLng(rightBoundary, topBoundary))

        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mapBoundary, 0))
        if(!zoomIn) mMap.moveCamera(CameraUpdateFactory.zoomTo(15f))
        else {
            mMap.animateCamera(CameraUpdateFactory.zoomTo(19f))
            clusterManager?.cluster()
        }

    }


    fun resetMap() {

//        mMap?.apply {
//            clear()
//        }
//
//        clusterManager?.apply {
//            clearItems()
//        }
        for(polyData in mPolyData.map { it.polyline }){
            polyData.remove()
        }


        mPolyData.clear()
        mPolyline = null
        previousPinPoint?.remove()
        selectedMarker?.isVisible=true
        selectedMarker = null
//        previousPinPoint = null
//        setupClusterer()

    }


    fun setupClusterer() {
        clusterManager = ClusterManager<CustomMarker>(this, mMap)
        clusterManager.renderer = CustomClusterRenderer(this, mMap, clusterManager)

        val collection = clusterManager.markerCollection

        collection.setOnInfoWindowClickListener {


            showAlertDialog(it)
        }


        var i = 0

        for (destination in Constants.destinations) {
            clusterManager.addItem(
                CustomMarker(
                    LatLng(
                        destination.latitude,
                        destination.longitude
                    ), destination.name, destination.desc, destination.markerRes
                )
            )

        }
        clusterManager.setAnimation(false)
        clusterManager.cluster()
//        setCameraView()

    }


    fun showAlertDialog(marker: Marker) {


        val builder = AlertDialog.Builder(this)
            .setPositiveButton("Yes") { dialog, id ->
                previousPinPoint?.remove()

                selectedMarker?.isVisible = true
                selectedMarker = marker
                calculateDirection(marker)

            }.setNegativeButton("No") { dialog, id ->
                dialog.dismiss()

            }.setMessage("Do you want to go to this location?")
            .setTitle("Direction")
        builder.create().show()


    }

    fun showPolylineToMap(directionsResult: DirectionsResult) {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                if (mPolyData.size > 0) {
                    for (data in mPolyData) {
                        data.polyline.remove()
                    }
                    mPolyData.clear()
                }

            }


            for ((i, route) in (directionsResult.routes).withIndex()) {
                val decodedPath = PolylineEncoding.decode(route.overviewPolyline.encodedPath)
                val newDecodedPath = ArrayList<LatLng>()
                for (latLng in decodedPath) {
                    newDecodedPath.add(LatLng(latLng.lat, latLng.lng))
                }


                withContext(Dispatchers.Main) {
                    val polyline = mMap.addPolyline(PolylineOptions().addAll(newDecodedPath))
                    val polyLineData = PolyLineData(polyline, route.legs[0])
                    mPolyData.add(polyLineData)
                    if (i == 0) {
                        mPolyline = polyline
                        polyline.color = ContextCompat.getColor(this@MainActivity, R.color.mBlue)
                        polyline.zIndex = 1F
                        polyline.isClickable = true
                        selectedMarker!!.isVisible = false

                        goToRoute(polyLineData)

                    } else {
                        polyline.color = ContextCompat.getColor(this@MainActivity, R.color.darkGrey)
                        polyline.zIndex = 0F
                        polyline.isClickable = true
                    }

                }

            }

        }


    }

    fun goToRoute(polyLineData: PolyLineData) {
        previousPinPoint?.remove()


        selectedMarker!!.isVisible = false
        previousPinPoint = mMap.addMarker(
            MarkerOptions().position(selectedMarker!!.position)
                .snippet(polyLineData!!.leg.duration.toString())
                .title(selectedMarker!!.title)
        )

    }


    override fun onCameraMoveStarted(p0: Int) {

    }

    override fun onCameraMove() {
    }


}