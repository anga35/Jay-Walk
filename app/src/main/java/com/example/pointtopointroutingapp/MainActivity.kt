package com.example.pointtopointroutingapp

import android.annotation.SuppressLint
import android.app.Activity
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
import com.example.pointtopointroutingapp.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.example.pointtopointroutingapp.CustomMarker

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mMap: GoogleMap
    private var mapFragment: SupportMapFragment? = null
    var location: Location?=null
    lateinit var clusterManager: ClusterManager<CustomMarker>

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
                        setCameraView()
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
        }
        mMap.isMyLocationEnabled=true
    }


    private fun setCameraView(){


        val bottomBoundary=location?.longitude!!-0.1
        val leftBoundary=location?.latitude!!-0.1
        val topBoundary=location?.longitude!!+0.1
        val rightBoundary=location?.latitude!!+0.1
        val mapBoundary= LatLngBounds(LatLng(leftBoundary,bottomBoundary),LatLng(rightBoundary,topBoundary))

        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mapBoundary,0))
setupClusterer()
    }


    fun setupClusterer(){
        clusterManager=ClusterManager<CustomMarker>(this,mMap)
        clusterManager.renderer=CustomClusterRenderer(this,mMap,clusterManager)
        clusterManager.addItem(CustomMarker(
            LatLng(location!!.latitude,
            location!!.longitude),"TESTO","FF",R.drawable.my_pic
        ))
        clusterManager.cluster()

    }

}