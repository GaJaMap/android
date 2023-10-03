package com.pg.gajamap.ui.adapter

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.kakao.sdk.user.model.User
import com.pg.gajamap.R
import com.pg.gajamap.base.UserData
import com.pg.gajamap.data.model.Client
import com.pg.gajamap.databinding.ItemListBinding
import com.pg.gajamap.ui.view.CustomerInfoActivity

class CustomerListAdapter(private var dataList: List<Client>) :
    RecyclerView.Adapter<CustomerListAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val mainCardView = binding.cardview
        val buttonNavi: ConstraintLayout = binding.itemProfileCarBtn

        init {
            binding.itemProfilePhoneBtn.setOnClickListener {
                Log.d("phone", "why")
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val client = dataList[position]
                    val phoneNumber = client.phoneNumber

                    // 여기서 전화 걸기 기능을 수행하도록 코드를 작성합니다.
                    // 예를 들어, 다음과 같이 작성할 수 있습니다:
                    val intent = Intent(Intent.ACTION_DIAL)
                    intent.data = Uri.parse("tel:$phoneNumber")
                    binding.root.context.startActivity(intent)
                }
            }
        }

        fun bind(data: Client) {
            val address = data.address.mainAddress
            Log.d("distance", data.distance.toString())
            if (data.distance.toString() == "null") {
                val distance1 = "- " + "km"
                binding.itemProfileDistance.text = distance1
            } else {
                val distanceInMeters = data.distance // data.distance는 미터(m) 단위로 가정
                val distanceInKilometers = distanceInMeters?.div(1000.0) // 미터를 킬로미터로 변환
                val formattedDistance = String.format("%.1f km", distanceInKilometers) // 소수점 한 자리까지 표시
                binding.itemProfileDistance.text = formattedDistance
            }
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
            binding.itemProfileAddressDetail.isSelected = true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataList[position])

        holder.mainCardView.setOnClickListener {
            intentToData(it, position)
        }
        holder.buttonNavi.setOnClickListener {
            naviClickListener.onClick(it, position)
        }
    }

    interface OnItemClickListener {
        fun onClick(v: View, position: Int)
    }

    interface ItemClickListener {
        fun onClick(v: View, position: Int)
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.itemClickListener = onItemClickListener
    }

    fun setItemClickListener(itemClickListener: ItemClickListener) {
        this.naviClickListener = itemClickListener
    }

    private lateinit var itemClickListener: OnItemClickListener
    private lateinit var naviClickListener: ItemClickListener

    fun updateData(newDataList: List<Client>) {
        //differ.submitList(newDataList)
        dataList = newDataList
        notifyDataSetChanged()
    }

    fun intentToData(v: View, position: Int) {
        val intent = Intent(v.context, CustomerInfoActivity::class.java)
        intent.putExtra("filePath", UserData.imageUrlPrefix + dataList[position].image.filePath)
        intent.putExtra("name", dataList[position].clientName)
        intent.putExtra("phoneNumber", dataList[position].phoneNumber)
        intent.putExtra("address1", dataList[position].address.mainAddress)
        intent.putExtra("address2", dataList[position].address.detail)
        intent.putExtra("clientId", dataList[position].clientId)
        intent.putExtra("groupId", dataList[position].groupInfo.groupId)
        intent.putExtra("latitude", dataList[position].location.latitude)
        intent.putExtra("longitude", dataList[position].location.longitude)
        v.context.startActivity(intent)
    }
}

