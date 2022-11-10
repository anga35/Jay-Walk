package com.example.pointtopointroutingapp.models

import com.google.android.gms.maps.model.Polyline
import com.google.maps.model.DirectionsLeg

class PolyLineData(var polyline: Polyline,var leg: DirectionsLeg){

    override fun toString(): String {
        return "PolylineData{polyline=${polyline},leg=${leg}}"
    }


}
