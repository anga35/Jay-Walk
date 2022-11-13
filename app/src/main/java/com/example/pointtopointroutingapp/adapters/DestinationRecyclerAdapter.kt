package com.example.pointtopointroutingapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pointtopointroutingapp.databinding.ItemDestinationBinding
import com.example.pointtopointroutingapp.models.Destination

class DestinationRecyclerAdapter(val destinationList:List<Destination>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var destinationOnClickListener:DestinationOnClickListener?=null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ItemDestinationBinding.inflate(
            LayoutInflater.from(parent.context), parent,
            false
        )
        return DestinationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if(holder is DestinationViewHolder){
            with(holder){
                with(destinationList[position]){
                    binding.imvDestination.setImageResource(markerRes)
                    binding.tvDestination.text=name
                    binding.imvDestinationGoto.setOnClickListener {
                        destinationOnClickListener?.onClick(latitude, longitude)
                    }


                }
            }


        }
    }

    override fun getItemCount(): Int {
        return destinationList.size
    }


    class DestinationViewHolder(val binding: ItemDestinationBinding) :
        RecyclerView.ViewHolder(binding.root)


    interface DestinationOnClickListener{

        fun onClick(latitude:Double,longitude:Double)

    }
}