package com.example.pointtopointroutingapp

fun main() {
val arr= listOf<Int>(1,2,3,4,5,6,7,8,9,10)


    val filteredArr=arr.filter {i->
        i%2!=0
    }
    println(arr)
    print(filteredArr)
}


