package com.pg.gajamap.ui.view

import android.app.AlertDialog
import android.content.Intent
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.pg.gajamap.R
import com.pg.gajamap.base.BaseActivity
import com.pg.gajamap.base.GajaMapApplication
import com.pg.gajamap.base.UserData
import com.pg.gajamap.data.model.LoginRequest
import com.pg.gajamap.data.response.SearchResultData
import com.pg.gajamap.databinding.ActivityLoginBinding
import com.pg.gajamap.ui.adapter.SearchResultAdapter
import com.pg.gajamap.viewmodel.LoginViewModel
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.common.util.Utility
import com.kakao.sdk.user.UserApiClient
import com.pg.gajamap.base.NetworkManager
import com.pg.gajamap.ui.fragment.loginTerms.LocationInfoFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : BaseActivity<ActivityLoginBinding>(R.layout.activity_login) {
    // SearchResult recyclerview
    private val searchResultList = arrayListOf<SearchResultData>()
    private val searchResultAdapter = SearchResultAdapter(searchResultList)
    // 뒤로가기 두 번 클릭 시 앱 종료
    private var backPressedTime: Long = 0

    override val viewModel by viewModels<LoginViewModel> {
        LoginViewModel.LoginViewModelFactory("tmp")
    }

    override fun preLoad() {
    }

    override fun initViewModel(viewModel: ViewModel) {
        binding.lifecycleOwner = this@LoginActivity
        binding.activity = this@LoginActivity
    }

    override fun onCreateAction() {

        // val keyHash = Utility.getKeyHash(this)
        // Log.d("Hash", keyHash)
        // 이곳에 화면 기능 구현
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
            viewModel.autoLogin.observe(this, Observer {
                // 싱글톤 패턴을 이용하여 자동 로그인 response 데이터 값 저장
                UserData.clientListResponse = it.clientListResponse
                UserData.groupinfo = it.groupInfo
                UserData.imageUrlPrefix = it.imageUrlPrefix

                dialogHide()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            })
            viewModel.autoLoginError.observe(this) {
                dialogHide()
            }
        }

        binding.locationInfo.setOnClickListener {
            val intent = Intent(this, TermsActivity::class.java)
            intent.putExtra("fragmentType", "locationInfo")
            startActivity(intent)
        }

        binding.serviceInfo.setOnClickListener {
            val intent = Intent(this, TermsActivity::class.java)
            intent.putExtra("fragmentType", "serviceInfo")
            startActivity(intent)
        }

        binding.userInfo.setOnClickListener {
            val intent = Intent(this, TermsActivity::class.java)
            intent.putExtra("fragmentType", "userInfo")
            startActivity(intent)
        }
    }

    fun kakaoLogin() {
        dialogShow()
        // 카카오계정으로 로그인 공통 callback 구성
        // 카카오톡으로 로그인 할 수 없어 카카오계정으로 로그인할 경우 사용됨
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Log.d("kakao", "카카오계정으로 로그인 실패 ${error}")
            } else if (token != null) {
                Log.d("kakoAccessToken", token.accessToken)
                //Log.d("kakoRefreshToken", token.refreshToken)
                postLogin(token.accessToken)
            }
        }
        // 카카오톡이 설치되어 있으면 카카오톡으로 로그인, 아니면 카카오계정으로 로그인
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
            UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
                if (error != null) {
                    Log.d("kakao", "카카오톡으로 로그인 실패 ${error}")
                    // 사용자가 카카오톡 설치 후 디바이스 권한 요청 화면에서 로그인을 취소한 경우,
                    // 의도적인 로그인 취소로 보고 카카오계정으로 로그인 시도 없이 로그인 취소로 처리 (예: 뒤로 가기)
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        return@loginWithKakaoTalk
                    }
                    // 카카오톡에 연결된 카카오계정이 없는 경우, 카카오계정으로 로그인 시도
                    UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
                } else if (token != null) {
                    Log.d("kakoAccessToken", token.accessToken)
                    postLogin(token.accessToken)
                }
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
        }
    }

    //로그인 api
    private fun postLogin(token: String) {

        Log.d("kakoAccessToken_1", token)

        CoroutineScope(Dispatchers.Main).launch {
            withContext(Dispatchers.IO) {  // 비동기 작업 시작
                viewModel.postLogin(LoginRequest(token))// postLogin 호출 및 결과 대기
                //viewModel.autoLogin()
            }
            viewModel.login.observe(this@LoginActivity, Observer {
                GajaMapApplication.prefs.setString("authority", it.authority)
                GajaMapApplication.prefs.setString("email", it.email)
                GajaMapApplication.prefs.setString("createdDate", it.createdDate)
            })

        }
    }

    private fun dialogShow() {
        binding.progress.isVisible = true
        window?.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun dialogHide() {
        binding.progress.isVisible = false
        window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

}