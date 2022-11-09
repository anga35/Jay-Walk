package com.example.pointtopointroutingapp

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.pointtopointroutingapp.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.example.pointtopointroutingapp.CustomMarker
import com.google.android.gms.maps.model.*
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

class MainActivity : AppCompatActivity(),
    GoogleMap.OnCameraMoveStartedListener,
    GoogleMap.OnCameraMoveListener,
    OnMapReadyCallback {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mMap: GoogleMap
    private var mapFragment: SupportMapFragment? = null
    var location: Location?=null
    lateinit var clusterManager: ClusterManager<CustomMarker>
    lateinit var geoApiContext:GeoApiContext
    var mPolyline: Polyline?=null
    private val startSettingsForResult=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){


        if(checkLocation()) displayMap()
        else toastMessage("Location still not enabled")

    }
    private var mFusedLocationClient: FusedLocationProviderClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val requestPermissionLauncher=registerForActivityResult(ActivityResultContracts.RequestPermission()){
            isGranted:Boolean->
            if(isGranted){
                Toast.makeText(this,"ACCESS GRANTED",Toast.LENGTH_SHORT).show()
                validateMapRequirement()
            }
            else{
                Toast.makeText(this,"ACCESS DENIED",Toast.LENGTH_SHORT).show()
            }
        }

        when{
            ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED->{
                validateMapRequirement()
                }
            shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION)->{

            }
            else->{
                Toast.makeText(this,"BLAH BLAH BLAH",Toast.LENGTH_SHORT).show()
                requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }



        }


    }

    fun toastMessage(message:String){

        Toast.makeText(this,message,Toast.LENGTH_SHORT).show()

    }

    fun validateMapRequirement(){
        if (checkLocation()) displayMap()
        else startSettingsForResult.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }


    @SuppressLint("MissingPermission")
    fun displayMap() {
        if (mapFragment == null) {

            mFusedLocationClient=LocationServices.getFusedLocationProviderClient(this)
            mFusedLocationClient!!.lastLocation.addOnCompleteListener {
                task->
                if(task.isSuccessful){
                    location=task.result
                    if(mMap!=null){
                        setCameraView(41.26636,-95.95708)
                        setupClusterer()
                    }
                    Log.d("Latitude", "displayMap: ${location!!.latitude}")
                    Log.d("Longitude","displayMap ${location!!.longitude}")


                }
            }
            val options = GoogleMapOptions()
            mapFragment = SupportMapFragment.newInstance(options)
            supportFragmentManager.beginTransaction().replace(R.id.map, mapFragment!!).commit()
            mapFragment!!.getMapAsync(this)




        }
    }


    fun checkLocation():Boolean{
        val locationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
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
        if(location!=null){
            setCameraView()
            setupClusterer()
        }
        mMap.isMyLocationEnabled=true
        mMap.setOnCameraMoveListener {
            val cameraPos=mMap.cameraPosition
            val latitude=cameraPos.target.latitude
            val longitude=cameraPos.target.longitude
            if((latitude<41.3 && latitude>41.1) && (longitude> -96.0 && longitude< -95.90)){
                clusterManager.cluster()
            }

        }
//        mMap.setOnInfoWindowClickListener {
//            calculateDirection(it)
//        }
        geoApiContext=GeoApiContext.Builder()
            .apiKey("AIzaSyBae1961sKyOmV6O0wbzHDI0r6_G_Pd4TI")
            .build()


    }


    private fun calculateDirection(marker: Marker){
        val TAG="calculateDirections"
        val destination=com.google.maps.model.LatLng(
            marker.position.latitude,
            marker.position.longitude)
        val directions=DirectionsApiRequest(geoApiContext)
        directions.alternatives(true)
        directions.origin(
           com.google.maps.model.LatLng(
               Constants.destinations[0].latitude,
               Constants.destinations[0].longitude
           )
        )
        directions.destination(destination).setCallback(
            object: PendingResult.Callback<DirectionsResult>{
                override fun onResult(result: DirectionsResult?) {
                    Log.d(TAG,result!!.routes[0].toString())
                    Log.d(TAG, result!!.routes[0].legs[0].duration.toString())
                    Log.d(TAG, result!!.routes[0].legs[0].distance.toString())


                    showPolylineToMap(result!!)
                }

                override fun onFailure(e: Throwable?) {
                    Log.d(TAG,"ERRORRRR")
                }

            }
        )

    }

    private fun setCameraView(latitude:Double=Constants.destinations[9].latitude,longitude:Double=Constants.destinations[9].longitude){


        val bottomBoundary=longitude!!-0.1
        val leftBoundary=latitude!!-0.1
        val topBoundary=longitude!!+0.1
        val rightBoundary=latitude!!+0.1
        val mapBoundary= LatLngBounds(LatLng(leftBoundary,bottomBoundary),LatLng(rightBoundary,topBoundary))

        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mapBoundary,0))
        mMap.moveCamera(CameraUpdateFactory.zoomTo(15f))

    }


    fun setupClusterer(){
        clusterManager=ClusterManager<CustomMarker>(this,mMap)
        clusterManager.renderer=CustomClusterRenderer(this,mMap,clusterManager)

        val collection=clusterManager.markerCollection

        collection.setOnInfoWindowClickListener {
            showAlertDialog(it)
        }

        var i =0
        for(destination in Constants.destinations){
            clusterManager.addItem(CustomMarker(
                LatLng(destination.latitude,
                    destination.longitude),"TESTO","FF",destination.markerRes
            )
            )

        }
        clusterManager.setAnimation(false)
        clusterManager.cluster()
//        setCameraView()

    }


    fun showAlertDialog(marker:Marker){


        val builder=AlertDialog.Builder(this)
            .setPositiveButton("Yes") { dialog, id ->
            calculateDirection(marker)

            }.setNegativeButton("No"){
                    dialog,id->
                dialog.dismiss()

            }.setMessage("Do you want to go to this location?")
            .setTitle("Direction")
        builder.create().show()


    }

    fun showPolylineToMap(directionsResult: DirectionsResult){
        for (route in directionsResult.routes){
            val decodedPath=PolylineEncoding.decode(route.overviewPolyline.encodedPath)
            val newDecodedPath= ArrayList<LatLng>()
            for(latLng in decodedPath){
                newDecodedPath.add(LatLng(latLng.lat,latLng.lng))
            }

            lifecycleScope.launch {
                mPolyline?.apply {
                    remove()

                }
                val polyline=mMap.addPolyline(PolylineOptions().addAll(newDecodedPath))
                mPolyline=polyline
                polyline.color=ContextCompat.getColor(this@MainActivity,R.color.darkGrey)
                polyline.isClickable=true
            }

        }

        lifecycleScope.launch{
            withContext(Dispatchers.Main){

            }
        }




    }


    override fun onCameraMoveStarted(p0: Int) {

    }

    override fun onCameraMove() {
    }


}