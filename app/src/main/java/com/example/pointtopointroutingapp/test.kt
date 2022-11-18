package com.example.pointtopointroutingapp

import java.math.BigDecimal
import kotlin.math.roundToInt

fun main() {

    val a=BigDecimal("6.891173")
    val b=BigDecimal("6.890618")

    if(a<b){
        print("Hi")
    }

}

fun round(double: Double):Double{
    return (double*100.0)/100.0
}
