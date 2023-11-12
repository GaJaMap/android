package com.pg.gajamap.ui.fragment.customerAdd

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.activity.OnBackPressedCallback
import androidx.core.view.contains
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import com.pg.gajamap.R
import com.pg.gajamap.base.BaseFragment
import com.pg.gajamap.databinding.FragmentEditAddressMapBinding
import com.pg.gajamap.ui.view.CustomerInfoActivity
import com.pg.gajamap.viewmodel.GetClientViewModel
import net.daum.mf.map.api.MapView

class EditAddressMapFragment: BaseFragment<FragmentEditAddressMapBinding>(R.layout.fragment_edit_address_map) {

    var customerInfoActivity: CustomerInfoActivity? = null
    private lateinit var mapView: MapView

    override val viewModel by viewModels<GetClientViewModel> {
        GetClientViewModel.AddViewModelFactory("tmp")
    }

    override fun initViewModel(viewModel: ViewModel) {
        binding.lifecycleOwner = this@EditAddressMapFragment
        binding.viewModel = this.viewModel
    }

    override fun onCreateAction() {

        mapView = MapView(context)
        binding.kakaoMapContainer.addView(mapView)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    customerInfoActivity!!.finish()
                }
            })
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        customerInfoActivity = context as CustomerInfoActivity
    }

    override fun onResume() {
        super.onResume()
        if (binding.kakaoMapContainer.contains(mapView)) {
            try{
                // 다시 맵뷰 초기화 및 추가
                binding.kakaoMapContainer.addView(mapView)
            }catch (re: RuntimeException){
                Log.d("error",re.toString())
            }
        }

    }

    override fun onPause() {
        super.onPause()
        binding.kakaoMapContainer.removeView(mapView)
    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        binding.kakaoMapContainer.removeView(mapView)
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        binding.kakaoMapContainer.removeView(mapView)
//    }
}