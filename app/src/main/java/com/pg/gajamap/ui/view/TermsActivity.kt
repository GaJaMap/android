package com.pg.gajamap.ui.view

import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.pg.gajamap.R
import com.pg.gajamap.base.BaseActivity
import com.pg.gajamap.databinding.ActivityTermsBinding
import com.pg.gajamap.ui.fragment.loginTerms.LocationInfoFragment
import com.pg.gajamap.ui.fragment.loginTerms.ServiceInfoFragment
import com.pg.gajamap.ui.fragment.loginTerms.UserInfoFragment
import com.pg.gajamap.viewmodel.GetClientViewModel

class TermsActivity : BaseActivity<ActivityTermsBinding>(R.layout.activity_terms) {
    override val viewModel by viewModels<GetClientViewModel> {
        GetClientViewModel.AddViewModelFactory("tmp")
    }

    override fun preLoad() {
    }

    override fun initViewModel(viewModel: ViewModel) {
        binding.lifecycleOwner = this@TermsActivity
    }

    override fun onCreateAction() {
        supportFragmentManager.beginTransaction().replace(R.id.frame_fragment, determineInitialFragment()).commit()

        // deprecated 된 onBackPressed() 대신 사용
        // 위에서 생성한 콜백 인스턴스 붙여주기
        this.onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun determineInitialFragment(): Fragment {
        val intent = intent

        return when (intent.getStringExtra("fragmentType")) {
            "locationInfo" -> LocationInfoFragment()
            "serviceInfo" -> ServiceInfoFragment()
            "userInfo" -> UserInfoFragment()
            else -> LocationInfoFragment() // Default to ServiceInfoFragment if no match
        }
    }

    // 뒤로가기 두 번 클릭 시 앱 종료
    // 콜백 인스턴스 생성
    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()
        }
    }
}