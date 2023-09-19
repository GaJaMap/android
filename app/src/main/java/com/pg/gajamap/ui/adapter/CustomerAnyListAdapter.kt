package com.pg.gajamap.ui.adapter

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.pg.gajamap.R
import com.pg.gajamap.base.GajaMapApplication
import com.pg.gajamap.base.UserData
import com.pg.gajamap.data.model.Client
import com.pg.gajamap.databinding.ItemAnyListBinding

class CustomerAnyListAdapter(private val dataList: List<Client>): RecyclerView.Adapter<CustomerAnyListAdapter.ViewHolder>() {

    private var selectPos :Boolean = false
    private var pos : Int = -1
    private val selectedPositions = mutableSetOf<Int>()

    // 원래 배경 리소스 ID (클릭 전의 배경)
    private var originalBackgroundResource: Int = 0

    private var itemBackground: Drawable? = null

    inner class ViewHolder(private val binding: ItemAnyListBinding):
            RecyclerView.ViewHolder(binding.root) {
        fun bind(data: Client, background: Drawable?) {
            val address = data.address.mainAddress
            val imageUrl = UserData.imageUrlPrefix
            val file = imageUrl + data.image.filePath
            Glide.with(binding.itemProfileImg.context)
                .load(file)
                .fitCenter()
                .apply(RequestOptions().override(500, 500))
                .error(R.drawable.profile_img_origin)
                .into(binding.itemProfileImg)
            binding.itemProfileAddressDetail.text = address
            binding.itemProfileName.text = data.clientName
            binding.itemProfilePhoneDetail.text = data.phoneNumber
            itemView.background = background

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAnyListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
       holder.bind(dataList[position], itemBackground)

        holder.itemView.setOnClickListener {
            if (selectedPositions.contains(position)) {
                // 이미 선택된 상태일 경우 선택 해제
                itemClickListener.onClick(it, position)
                holder.itemView.setBackgroundResource(R.drawable.fragment_list_tool)
                selectedPositions.remove(position) // 선택 상태를 해제
            } else {
                // 선택되지 않은 상태일 경우 선택
                itemClickListener.onClick(it, position)
                holder.itemView.setBackgroundResource(R.drawable.fragment_list_tool_purple)
                selectedPositions.add(position) // 선택 상태를 저장
            }
        }

    }

    fun addItemBackground(background: Drawable?) {
        itemBackground = background
        for (i in dataList.indices) {
            selectedPositions.add(i)
        }
        notifyDataSetChanged()
    }

    fun deleteItemBackground(background: Drawable?) {
        itemBackground = background
        for (i in dataList.indices) {
            selectedPositions.remove(i)
        }
        notifyDataSetChanged()
    }

    interface OnItemClickListener {
        fun onClick(v: View, position: Int)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener){
        this.itemClickListener = onItemClickListener
    }

    private lateinit var itemClickListener : OnItemClickListener

}