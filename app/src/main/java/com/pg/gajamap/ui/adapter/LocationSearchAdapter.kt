package com.pg.gajamap.ui.adapter

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pg.gajamap.R
import com.pg.gajamap.data.response.LocationSearchData
import com.pg.gajamap.ui.view.AddDirectActivity


class LocationSearchAdapter(val context: Context, val itemList: ArrayList<LocationSearchData>): RecyclerView.Adapter<LocationSearchAdapter.ViewHolder>() {
    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_location_search, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = itemList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.name.text = itemList[position].name
        if(itemList[position].road == "") {
            holder.road.text = itemList[position].address
        } else {
            holder.road.text = itemList[position].road
        }

        // 아이템의 배경 설정
        if(position == selectedPosition){
            holder.itemView.setBackgroundResource(R.color.inform)
            holder.itemView.findViewById<Button>(R.id.btn_plus).visibility = View.VISIBLE
        }else{
            holder.itemView.setBackgroundResource(R.color.white)
            holder.itemView.findViewById<Button>(R.id.btn_plus).visibility = View.GONE
        }
        // 아이템 클릭 이벤트
        holder.itemView.setOnClickListener {
            // 이전에 선택된 아이템의 배경을 변경
            Log.d("locationSearchList3","ok")
            val previousSelectedPosition = selectedPosition
            selectedPosition = holder.position
            notifyItemChanged(previousSelectedPosition)
            // 현재 클릭된 아이템의 배경을 변경
            notifyItemChanged(selectedPosition)

            itemClickListener.onClick(it, position, holder.road.text.toString())
        }

        holder.button.setOnClickListener {

            val intent = Intent(context, AddDirectActivity::class.java)
            intent.putExtra("latitude", itemList[position].y)
            intent.putExtra("longitude", itemList[position].x)
            intent.putExtra("address", holder.road.text)
            context.startActivity(intent)
        }
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tv_name)
        val road: TextView = itemView.findViewById(R.id.tv_address)
        val button: Button = itemView.findViewById(R.id.btn_plus)
    }

    interface OnItemClickListener {
        fun onClick(v: View, position: Int, text: String)
    }

    fun setItemClickListener(onItemClickListener: OnItemClickListener) {
        this.itemClickListener = onItemClickListener
    }

    private lateinit var itemClickListener : OnItemClickListener
}