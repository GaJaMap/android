package com.pg.gajamap.ui.adapter

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.PhoneNumberUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kakao.sdk.navi.Constants
import com.kakao.sdk.navi.NaviClient
import com.kakao.sdk.navi.model.CoordType
import com.kakao.sdk.navi.model.Location
import com.kakao.sdk.navi.model.NaviOption
import com.pg.gajamap.R
import com.pg.gajamap.data.model.ViewPagerData
import com.pg.gajamap.databinding.ItemViewpagerBinding
import com.pg.gajamap.ui.view.CustomerInfoActivity


class ViewPagerAdapter(
    private val itemList: ArrayList<ViewPagerData>,
    private val context: Context
) : RecyclerView.Adapter<ViewPagerAdapter.PagerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerViewHolder {
        val binding =
            ItemViewpagerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PagerViewHolder(binding)
    }

    override fun getItemCount(): Int = itemList.size

    override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {
        holder.bind(itemList[position])
    }

    inner class PagerViewHolder(private val binding: ItemViewpagerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ViewPagerData) {

            binding.clCardPhoneBtn.setOnClickListener {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:${item.phoneNumber}")
                binding.root.context.startActivity(intent)
            }

            binding.clCardCarBtn.setOnClickListener {
                val items = arrayOf("카카오 내비", "네이버 내비")

                val adapter = CustomerDialogAdapter(context, items)

                val dialogBuilder = AlertDialog.Builder(context)
                dialogBuilder.setTitle("내비게이션 선택")
                dialogBuilder.setAdapter(adapter) { _, which ->
                    when (which) {
                        0 -> {
                            if (NaviClient.instance.isKakaoNaviInstalled(context)) {
                                Log.d("latilongti", item.longitude.toString())
                                context.startActivity(
                                    NaviClient.instance.navigateIntent(
                                        Location(
                                            item.name,
                                            item.longitude.toString(),
                                            item.latitude.toString()
                                        ),
                                        NaviOption(coordType = CoordType.WGS84)
                                    )
                                )
                            } else {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(Constants.WEB_NAVI_INSTALL)
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                )
                            }
                        }
                        1 -> {
                            try {
                                val url =
                                    "nmap://navigation?dlat=" + item.latitude.toString() + "&dlng=" + item.longitude.toString() + "&dname=" + item.name + "&appname=com.pg.gajamap"

                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                intent.addCategory(Intent.CATEGORY_BROWSABLE)

                                context.startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("market://details?id=com.nhn.android.nmap")
                                    )
                                )
                            }
                        }
                    }
                }

                val dialog = dialogBuilder.create()
                dialog.show()
            }

            if (item.profileImg == "null") {
                Glide.with(itemView).load(R.drawable.profile_img_origin).into(binding.ivCardProfile)
            } else {
                Glide.with(itemView).load(item.profileImg).into(binding.ivCardProfile)
            }
            binding.tvCardName.text = item.name
            binding.tvCardAddressDetail.text = item.address
            binding.tvCardPhoneDetail.text = PhoneNumberUtils.formatNumber(item.phoneNumber)
            binding.tvCardAddressDetail.isSelected = true
            binding.itemViewpager.setOnClickListener {
                intentToData(position)
            }
            if (item.distance == null) {
                binding.tvCardDistance.text = "- "
            } else {
                binding.tvCardDistance.text = String.format("%.1f ", item.distance?.times(0.001))
            }
        }
    }

    fun intentToData(position: Int) {
        val intent = Intent(context, CustomerInfoActivity::class.java)
        intent.putExtra("clientId", itemList[position].clientId)
        intent.putExtra("groupId", itemList[position].groupId)
        intent.putExtra("filePath", itemList[position].profileImg)
        intent.putExtra("name", itemList[position].name)
        intent.putExtra("phoneNumber", itemList[position].phoneNumber)
        intent.putExtra("address1", itemList[position].address)
        intent.putExtra("address2", itemList[position].detail)
        intent.putExtra("latitude", itemList[position].latitude)
        intent.putExtra("longitude", itemList[position].longitude)
        context.startActivity(intent)
    }
}