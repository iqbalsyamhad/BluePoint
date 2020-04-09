package com.syamhad.bluepoint.func

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Color.GRAY
import android.graphics.Color.WHITE
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.syamhad.bluepoint.R
import com.syamhad.bluepoint.ui.MapsActivity
import kotlinx.android.synthetic.main.pin_list.view.*
import kotlin.math.round

class PinAdapter(pinList: ArrayList<PinModel>, mOnclick: PinInterface) : RecyclerView.Adapter<PinAdapter.MyViewHolder>() {
    private val mOnItemClickListener = mOnclick
    private val pinList: List<PinModel>
    private lateinit var context: Context

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.pin_list, parent, false)
        context = parent.context
        return MyViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val pin: PinModel = pinList[position]
        holder.itemView.name.setText(pin.name)
        holder.itemView.address.setText(pin.address)
        holder.itemView.latlng.setText("${round(pin.distance).toInt()} M.")
        if(pin.distance > MapsActivity.radius){
            holder.itemView.cardView.setCardBackgroundColor(GRAY)
            holder.itemView.setOnClickListener { v ->
                Toast.makeText(context, "Out of radius", Toast.LENGTH_SHORT).show()
            }
        }
        else{
            holder.itemView.cardView.setCardBackgroundColor(WHITE)
            holder.itemView.setOnClickListener { v ->
                mOnItemClickListener.onItemClick(v, position)
            }
        }
    }

    override fun getItemCount(): Int {
        return pinList.size
    }

    init {
        this.pinList = pinList
    }
}