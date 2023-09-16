package com.pg.gajamap.ui.fragment.customerAdd

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.pg.gajamap.R
import com.pg.gajamap.base.BaseFragment
import com.pg.gajamap.base.GajaMapApplication
import com.pg.gajamap.base.UserData
import com.pg.gajamap.data.model.Client
import com.pg.gajamap.databinding.FragmentCustomerInfoBinding
import com.pg.gajamap.ui.view.CustomerInfoActivity
import com.pg.gajamap.viewmodel.GetClientViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CustomerInfoFragment: BaseFragment<FragmentCustomerInfoBinding>(R.layout.fragment_customer_info) {

    var customerInfoActivity: CustomerInfoActivity?=null

    override val viewModel by viewModels<GetClientViewModel> {
        GetClientViewModel.AddViewModelFactory("tmp")
    }
    override fun initViewModel(viewModel: ViewModel) {
        binding.lifecycleOwner = this@CustomerInfoFragment
        binding.viewModel = this.viewModel
    }



    override fun onCreateAction(){
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("set", "why")
            setView()
        }
        binding.topBackBtn.setOnClickListener {
            // 액티비티 꺼지게 하는 코드 추가
            customerInfoActivity!!.finish()
        }

        Glide.with(this)
            .load("https://maps.googleapis.com/maps/api/staticmap?center=Williamsburg,Brooklyn,NY&zoom=13&size=400x400&markers=color:blue%7Clabel:S%7C11211%7C11206%7C11222&key=AIzaSyA3kg7YWugGl1lTXmAmaBGPNhDW9pEh5bo")
            .error(R.drawable.location_not_found_text)
            .into(binding.mapImage)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                customerInfoActivity!!.finish()
            }
        })

        val clientId = requireActivity().intent.getLongExtra("clientId", -1).toString()
        val groupId = requireActivity().intent.getLongExtra("groupId", -1).toString()

        val positiveButtonClick = { dialogInterface: DialogInterface, i: Int ->
            Log.d("deleteId", clientId)
            viewModel.deleteClient(groupId.toLong(), clientId.toLong())
            viewModel.deleteClient.observe(this, Observer {
                removeClientWithClientId(clientId.toLong())
                customerInfoActivity!!.finish()
            })
            // 액티비티 꺼지게 하는 코드 추가
            Toast.makeText(requireContext(), "삭제되었습니다", Toast.LENGTH_SHORT).show()

        }
        val negativeButtonClick = { dialogInterface: DialogInterface, i: Int ->
            Toast.makeText(requireContext(), "취소", Toast.LENGTH_SHORT).show()
        }

        //고객 삭제 dialog
        binding.topDeleteBtn.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("해당 고객을 삭제하시겠습니까?")
                .setMessage("고객을 삭제하시면 영구 삭제되어 복구할 수 없습니다.")
                .setPositiveButton("확인", positiveButtonClick)
                .setNegativeButton("취소", negativeButtonClick)
            val alertDialog = builder.create()
            alertDialog.show()
        }

        binding.topModifyBtn.setOnClickListener {

            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_fragment, EditProfileFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        customerInfoActivity = context as CustomerInfoActivity
    }

    private fun removeClientWithClientId(clientIdToRemove: Long) {
        // 자동 로그인 response 데이터 값 받아오기
        val clientList = UserData.clientListResponse?.clients as? MutableList<Client>

        if (clientList != null) {
            val iterator = clientList.iterator()
            while (iterator.hasNext()) {
                val client = iterator.next()
                if (client.clientId == clientIdToRemove) {
                    iterator.remove()
                    Log.d("delete", clientList.toString())
                    break  // 원하는 클라이언트를 찾고 삭제 후 반복문 종료
                }
            }
        }
    }

    private suspend fun setView(){
        withContext(Dispatchers.Main){
            text()
        }
    }

    fun text(){
        val name = requireActivity().intent.getStringExtra("name")
        val address1 = requireActivity().intent.getStringExtra("address1")
        val address2 = requireActivity().intent.getStringExtra("address2")
        val phone = requireActivity().intent.getStringExtra("phoneNumber")
        val image = requireActivity().intent.getStringExtra("filePath")

        if(image != null){
            val imageUrl = UserData.imageUrlPrefix
            val file = imageUrl + image
            Glide.with(requireContext())
                .load(file)
                .fitCenter()
                .apply(RequestOptions().override(500,500))
                .error(R.drawable.profile_img_origin)
                .into(binding.infoProfileImg)
        }

        binding.infoProfileNameTv.text = name
        binding.infoProfileAddressTv1.text = address1
        binding.infoProfileAddressTv2.text = address2
        binding.infoProfilePhoneTv.text = phone
    }

    override fun onResume() {
        super.onResume()
        CoroutineScope(Dispatchers.IO).launch {
            updateData()
        }
    }

    private suspend fun updateData() {
        withContext(Dispatchers.Main) {
            text()
        }
    }

}