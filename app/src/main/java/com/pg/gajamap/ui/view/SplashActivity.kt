package com.pg.gajamap.ui.view

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.pg.gajamap.R
import com.pg.gajamap.base.BaseActivity
import com.pg.gajamap.base.GajaMapApplication
import com.pg.gajamap.base.NetworkManager
import com.pg.gajamap.base.UserData
import com.pg.gajamap.databinding.ActivityLoginBinding
import com.pg.gajamap.databinding.ActivitySplashBinding
import com.pg.gajamap.viewmodel.LoginViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity<ActivitySplashBinding>(R.layout.activity_splash) {

    override val viewModel by viewModels<LoginViewModel> {
        LoginViewModel.LoginViewModelFactory("tmp")
    }

    override fun preLoad() {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { true }
    }

    override fun initViewModel(viewModel: ViewModel) {
        binding.lifecycleOwner = this@SplashActivity
        binding.activity = this@SplashActivity
    }

    override fun onCreateAction() {

        if (!NetworkManager.checkNetworkState(this)) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("네트워크 연결 확인")
            builder.setMessage("네트워크에 연결되어 있지 않습니다. 네트워크 연결을 확인해주세요.")
            builder.setPositiveButton("확인") { dialog, _ ->
                finish()
                dialog.dismiss()
            }
            builder.create().show()
        } else {
            startSomeNextActivity()
        }
    }

    private fun startSomeNextActivity() {
        lifecycleScope.launch {
            if (GajaMapApplication.prefs.getString("authority", "") == "") {
                delay(500)
                val intent = Intent(this@SplashActivity, LoginActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                delay(500)
                viewModel.autoLogin()
                viewModel.autoLogin.observe(this@SplashActivity, Observer {
                    // 싱글톤 패턴을 이용하여 자동 로그인 response 데이터 값 저장
                    UserData.clientListResponse = it.clientListResponse
                    UserData.groupinfo = it.groupInfo
                    UserData.imageUrlPrefix = it.imageUrlPrefix

                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                })
                viewModel.autoLoginError.observe(this@SplashActivity) {
                    startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                    finish()
                }
            }
        }
    }
}