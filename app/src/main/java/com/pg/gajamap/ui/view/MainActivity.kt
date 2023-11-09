package com.pg.gajamap.ui.view

import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.pg.gajamap.R
import com.pg.gajamap.base.BaseActivity
import com.pg.gajamap.databinding.ActivityMainBinding
import com.pg.gajamap.ui.fragment.customerList.ListFragment
import com.pg.gajamap.ui.fragment.map.MapFragment
import com.pg.gajamap.ui.fragment.setting.SettingFragment
import com.pg.gajamap.viewmodel.MainViewModel

class MainActivity : BaseActivity<ActivityMainBinding>(R.layout.activity_main) {
    private val ACCESS_FINE_LOCATION = 1000   // Request Code
    private var mapFragment: MapFragment? = null
    lateinit var bnMain : BottomNavigationView
    private val TAG_MAP = "map_fragment"
    private val TAG_LIST = "list_fragment"
    private val TAG_SETTING = "setting_fragment"

    override val viewModel by viewModels<MainViewModel> {
        MainViewModel.MainViewModelFactory("tmp")
    }

    override fun preLoad() {
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

        bnMain.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.menu_map -> setFragment(TAG_MAP, MapFragment())
                R.id.menu_list -> setFragment(TAG_LIST, ListFragment())
                R.id.menu_setting -> setFragment(TAG_SETTING, SettingFragment())
            }
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

    override fun onDestroy() {
        super.onDestroy()
        mapFragment?.stopTracking()
    }
}