package com.pg.gajamap.ui.fragment.customerAdd

import android.content.Context
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import com.pg.gajamap.R
import com.pg.gajamap.base.BaseFragment
import com.pg.gajamap.databinding.FragmentEditAddressMapBinding
import com.pg.gajamap.ui.view.CustomerInfoActivity
import com.pg.gajamap.viewmodel.GetClientViewModel
import net.daum.mf.map.api.MapView

class EditAddressMapFragment: BaseFragment<FragmentEditAddressMapBinding>(R.layout.fragment_edit_address_map) {

    var customerInfoActivity: CustomerInfoActivity?=null

    override val viewModel by viewModels<GetClientViewModel> {
        GetClientViewModel.AddViewModelFactory("tmp")
    }
    override fun initViewModel(viewModel: ViewModel) {
        binding.lifecycleOwner = this@EditAddressMapFragment
        binding.viewModel = this.viewModel
    }

    override fun onCreateAction(){

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                customerInfoActivity!!.finish()
            }
        })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        customerInfoActivity = context as CustomerInfoActivity
    }

    override fun onDestroy() {
        super.onDestroy()
        // 저장된 지도 타일 캐쉬 데이터를 모두 삭제
        // MapView가 동작 중인 상태에서도 사용 가능
        MapView.clearMapTilePersistentCache()
    }
}