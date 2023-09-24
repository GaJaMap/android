package com.pg.gajamap.ui.view

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import com.pg.gajamap.R
import com.pg.gajamap.base.BaseActivity
import com.pg.gajamap.databinding.ActivityMainBinding
import com.pg.gajamap.ui.fragment.customerList.ListFragment
import com.pg.gajamap.ui.fragment.map.MapFragment
import com.pg.gajamap.ui.fragment.setting.SettingFragment
import com.pg.gajamap.viewmodel.MainViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : BaseActivity<ActivityMainBinding>(R.layout.activity_main) {
    private val ACCESS_FINE_LOCATION = 1000   // Request Code
    private var mapFragment: MapFragment? = null
    lateinit var bnMain : BottomNavigationView
    private val TAG_MAP = "map_fragment"
    private val TAG_LIST = "list_fragment"
    private val TAG_SETTING = "setting_fragment"
    //뒤로가기 두번 클릭 시 앱 종료
    private var backPressedTime: Long = 0

    override val viewModel by viewModels<MainViewModel> {
        MainViewModel.MainViewModelFactory("tmp")
    }

    override fun initViewModel(viewModel: ViewModel) {
        binding.lifecycleOwner = this@MainActivity
        binding.viewModel = this.viewModel
    }

    override fun onCreateAction() {
        // 이곳에 화면 기능 구현

        // bottom navigation
        bnMain = binding.navBn
        mapFragment = MapFragment()

        // 맨 처음 화면을 켰을 때 map 탭이 보여지도록
        bnMain.selectedItemId = R.id.menu_map
        // 프래그먼트 초기화 및 추가
        setFragment(TAG_MAP, mapFragment!!)

        bnMain.setOnItemSelectedListener { menuItem ->
            val transaction = supportFragmentManager.beginTransaction()

            when (menuItem.itemId) {
                R.id.menu_map -> {
                    val mapFragment = MapFragment()
                    // 현재 표시 중인 프래그먼트를 제거
                    supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    transaction.replace(R.id.nav_fl, mapFragment, TAG_MAP)
                }
                R.id.menu_list -> {
                    val listFragment = ListFragment()
                    // 현재 표시 중인 프래그먼트를 제거
                    supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    transaction.replace(R.id.nav_fl, listFragment, TAG_LIST)
                }
                R.id.menu_setting -> {
                    val settingFragment = SettingFragment()
                    // 현재 표시 중인 프래그먼트를 제거
                    supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    transaction.replace(R.id.nav_fl, settingFragment, TAG_SETTING)
                }
            }

            transaction.commit()
            true
        }

        // 탭 버튼 재 호출시 이벤트 없이 처리
        bnMain.setOnItemReselectedListener {
            when (it.itemId) {
                R.id.menu_map -> {}
                R.id.menu_list -> {}
                R.id.menu_setting -> {}
            }
        }

        // deprecated 된 onBackPressed() 대신 사용
        // 위에서 생성한 콜백 인스턴스 붙여주기
        this.onBackPressedDispatcher.addCallback(this, callback)
    }

    // fragment 상태 유지를 위한 컨트롤 함수
    fun setFragment(tag: String, fragment: Fragment) {
        val manager : FragmentManager = supportFragmentManager
        val bt = manager.beginTransaction()

        // 바텀 네비게이션의 tag(즉, 메뉴)가 선택 되었을 때 생성되있지 않을 경우
        // 바로 생성(add)
        if (manager.findFragmentByTag(tag) == null) {
            bt.add(R.id.nav_fl, fragment, tag)
        }
        //코드 작성에 용이하게 따로 변수로 할당
        val map = manager.findFragmentByTag(TAG_MAP)
        val list = manager.findFragmentByTag(TAG_LIST)
        val setting = manager.findFragmentByTag(TAG_SETTING)

        //위에서 생성한 fragment들을
        //우선 전부 hide 시킨 후
        if (map != null) {
            bt.hide(map)
        }
        if (list != null) {
            bt.hide(list)
        }
        if (setting != null) {
            bt.hide(setting)
        }

        //tag로 입력받은 fragment만 show를 통해 보여주도록 함
        if (tag == TAG_MAP) {
            if (map != null) {
                bt.show(map)
            }
        }
        else if (tag == TAG_LIST) {
            if (list != null) {
                bt.show(list)
            }
        }
        else if (tag == TAG_SETTING) {
            if (setting != null) {
                bt.show(setting)
            }
        }
        bt.commitAllowingStateLoss()
    }

    // MapFragment에서 위치 권한 요청 후 행동
    @SuppressLint("ResourceAsColor")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ACCESS_FINE_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한 요청 후 승인됨 (추적 시작)
                Toast.makeText(this, "위치 권한 승인", Toast.LENGTH_SHORT).show()
                mapFragment?.startTracking()
            } else {
                // 권한 요청 후 거절됨 (다시 요청 or 토스트)
                Toast.makeText(this, "위치 권한 거절", Toast.LENGTH_SHORT).show()
                val bgShape = findViewById<ImageButton>(R.id.ib_gps).background as GradientDrawable
                bgShape.setColor(resources.getColor(R.color.white))
                findViewById<ImageButton>(R.id.ib_gps).setImageResource(R.drawable.ic_gray_gps)
            }
        }
    }

    // todo : 확인하기!
    // 뒤로가기 두 번 클릭 시 앱 종료
    // 콜백 인스턴스 생성
    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // 뒤로가기 버튼 이벤트 처리
            if(System.currentTimeMillis() - backPressedTime >= 2000) {
                backPressedTime = System.currentTimeMillis()
                Toast.makeText(this@MainActivity, "한번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show()
            } else {
                // 앱 자체 종료하기
                ActivityCompat.finishAffinity(this@MainActivity)
                System.runFinalization()
                System.exit(0)
            }
        }
    }
}