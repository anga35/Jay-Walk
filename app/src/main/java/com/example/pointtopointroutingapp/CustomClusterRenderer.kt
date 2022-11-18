package com.example.pointtopointroutingapp

import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.ClusterRenderer
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.ui.IconGenerator

class CustomClusterRenderer(context: Context,map:GoogleMap,clusterManager: ClusterManager<CustomMarker>):
    DefaultClusterRenderer<CustomMarker>(context,map,clusterManager){
    lateinit var iconGenerator: IconGenerator
    lateinit var imageView: ImageView
    init {
        iconGenerator=IconGenerator(context.applicationContext)
        imageView=ImageView(context.applicationContext)
        val markerSize=context.resources.getDimension(R.dimen.marker_dimen).toInt()
        val markerPadding=context.resources.getDimension(R.dimen.marker_padding).toInt()
        imageView.layoutParams=ViewGroup.LayoutParams(markerSize,markerSize)
        imageView.setPadding(markerPadding,markerPadding,markerPadding,markerPadding)
        iconGenerator.setContentView(imageView)
    }

    override fun onBeforeClusterItemRendered(item: CustomMarker, markerOptions: MarkerOptions) {


        imageView.setImageResource(item.getIcon())
        val icon=iconGenerator.makeIcon()
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon))
            .title(item.title)


    }

    override fun setOnClusterItemClickListener(listener: ClusterManager.OnClusterItemClickListener<CustomMarker>?) {
        super.setOnClusterItemClickListener(listener)
    }



}