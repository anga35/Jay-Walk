package com.example.pointtopointroutingapp

import android.graphics.drawable.Icon
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

class CustomMarker(
    private var position: LatLng,
    private var title: String,
    private var snippet:String,
    private var icon:Int
) : ClusterItem {


    override fun getPosition(): LatLng {
        return position
    }

    override fun getTitle(): String? {
        return title
    }

    override fun getSnippet(): String? {
        return snippet
    }

    fun getIcon():Int{
        return icon
    }

    fun setSnippet(snippet: String){
        this.snippet=snippet
    }


}