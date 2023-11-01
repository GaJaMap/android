package com.pg.gajamap.ui.fragment.setting

import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.pg.gajamap.BR
import com.pg.gajamap.R
import com.pg.gajamap.base.BaseFragment
import com.pg.gajamap.databinding.FragmentInquireBinding
import com.pg.gajamap.viewmodel.ClientViewModel


class InquireFragment: BaseFragment<FragmentInquireBinding>(R.layout.fragment_inquire) {

    override val viewModel by viewModels<ClientViewModel> {
        ClientViewModel.SettingViewModelFactory("tmp")
    }

    override fun initViewModel(viewModel: ViewModel) {
        binding.setVariable(BR.viewModel, viewModel)
        binding.lifecycleOwner = this@InquireFragment
        binding.fragment = this@InquireFragment
    }

    override fun onCreateAction() {
        hideBottomNavigation(true)
        binding.topBackBtn.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().remove(this).commit()
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    // 프래그먼트 바텀 네비게이션 뷰 숨기기
    private fun hideBottomNavigation(bool: Boolean) {
        val bottomNavigation = requireActivity().findViewById<BottomNavigationView>(R.id.nav_bn)
        if (bool) bottomNavigation.visibility = View.GONE else bottomNavigation.visibility =
            View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideBottomNavigation(false)
    }
}