package com.pg.gajamap.ui.adapter

import android.util.Log
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pg.gajamap.databinding.ItemPhoneBinding
import com.pg.gajamap.ui.fragment.setting.ContactsData

class PhoneListAdapter(private val dataList : ArrayList<ContactsData>, private val listener: OnItemClickListener2
): RecyclerView.Adapter<PhoneListAdapter.ViewHolder>() {

    private val checkedPositions = SparseBooleanArray()
    inner class ViewHolder(private val binding: ItemPhoneBinding):
            RecyclerView.ViewHolder(binding.root){
                fun bind(data : ContactsData){
                    binding.itemPhoneTv.text = data.name
                    binding.itemPhoneTv.isChecked = checkedPositions[position]
                    binding.itemPhoneTv.setOnClickListener {
                        val isChecked = binding.itemPhoneTv.isChecked
                        checkedPositions.put(position, isChecked)
                        listener.onItemClick(position, isChecked)
                    }
                }
            }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPhoneBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataList[position])

        holder.itemView.setOnClickListener{
            itemClickListener.onClick(it, position)
        }
    }

    interface OnItemClickListener {
        fun onClick(v: View, position: Int)
    }

    interface OnItemClickListener2 {
        fun onItemClick(position: Int, isChecked: Boolean)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener){
        this.itemClickListener = onItemClickListener
    }

    private lateinit var itemClickListener : OnItemClickListener

    fun setAllItemsChecked(checked: Boolean) {
        for (i in 0 until dataList.size) {
            checkedPositions.put(i, checked)
        }
        notifyDataSetChanged()
    }

    fun isChecked(position: Int): Boolean {
        return checkedPositions.get(position, false)
    }
}