package com.pg.gajamap.ui.fragment.setting

import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.pg.gajamap.BR
import com.pg.gajamap.R
import com.pg.gajamap.base.BaseFragment
import com.pg.gajamap.databinding.FragmentExcelBinding
import com.pg.gajamap.viewmodel.ClientViewModel

class ExcelFragment : BaseFragment<FragmentExcelBinding>(R.layout.fragment_excel) {

    override val viewModel by viewModels<ClientViewModel> {
        ClientViewModel.SettingViewModelFactory("tmp")
    }

    override fun initViewModel(viewModel: ViewModel) {
        binding.setVariable(BR.viewModel, viewModel)
        binding.lifecycleOwner = this@ExcelFragment
        binding.fragment = this@ExcelFragment
    }

    override fun onCreateAction() {

        hideBottomNavigation(true)

        binding.topBackBtn.setOnClickListener {
            parentFragmentManager.beginTransaction().replace(R.id.nav_fl, SettingFragment()).addToBackStack(null).commit()
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