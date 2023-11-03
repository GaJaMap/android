package com.pg.gajamap.ui.view

import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import com.pg.gajamap.R
import com.pg.gajamap.base.BaseActivity
import com.pg.gajamap.databinding.ActivityCustomerInfoBinding
import com.pg.gajamap.ui.fragment.customerAdd.CustomerInfoFragment
import com.pg.gajamap.viewmodel.GetClientViewModel

class CustomerInfoActivity : BaseActivity<ActivityCustomerInfoBinding>(R.layout.activity_customer_info) {
    // 뒤로가기 두 번 클릭 시 앱 종료
    private var backPressedTime: Long = 0

    override val viewModel by viewModels<GetClientViewModel> {
        GetClientViewModel.AddViewModelFactory("tmp")
    }
    override fun initViewModel(viewModel: ViewModel) {
        binding.lifecycleOwner = this@CustomerInfoActivity
        binding.viewModel = this.viewModel
    }

    override fun onCreateAction() {
        supportFragmentManager.beginTransaction().replace(R.id.frame_fragment, CustomerInfoFragment()).commit()

        // deprecated 된 onBackPressed() 대신 사용
        // 위에서 생성한 콜백 인스턴스 붙여주기
        this.onBackPressedDispatcher.addCallback(this, callback)
    }

    // 뒤로가기 두 번 클릭 시 앱 종료
    // 콜백 인스턴스 생성
    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()
        }
    }
}