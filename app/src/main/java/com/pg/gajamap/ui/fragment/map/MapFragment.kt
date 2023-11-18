package com.pg.gajamap.ui.fragment.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.contains
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import com.pg.gajamap.BR
import com.pg.gajamap.BuildConfig
import com.pg.gajamap.R
import com.pg.gajamap.api.retrofit.KakaoSearchClient
import com.pg.gajamap.base.BaseFragment
import com.pg.gajamap.base.GajaMapApplication
import com.pg.gajamap.base.UserData
import com.pg.gajamap.data.model.AutoLoginGroupInfo
import com.pg.gajamap.data.model.ViewPagerData
import com.pg.gajamap.data.response.*
import com.pg.gajamap.databinding.DialogAddGroupBottomSheetBinding
import com.pg.gajamap.databinding.DialogGroupBinding
import com.pg.gajamap.databinding.FragmentMapBinding
import com.pg.gajamap.ui.adapter.GroupListAdapter
import com.pg.gajamap.ui.adapter.LocationSearchAdapter
import com.pg.gajamap.ui.adapter.SearchResultAdapter
import com.pg.gajamap.ui.adapter.ViewPagerAdapter
import com.pg.gajamap.ui.view.AddDirectActivity
import com.pg.gajamap.ui.view.MainActivity
import com.pg.gajamap.viewmodel.MapViewModel
import net.daum.mf.map.api.MapPOIItem
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapView
import net.daum.mf.map.api.MapView.clearMapTilePersistentCache
import net.daum.mf.map.api.MapView.setMapTilePersistentCacheEnabled
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.Integer.min


class MapFragment : BaseFragment<FragmentMapBinding>(R.layout.fragment_map),
    MapView.POIItemEventListener, MapView.MapViewEventListener {
    // 그룹 리스트 recyclerview
    lateinit var groupListAdapter: GroupListAdapter
    var gName: String = ""
    var pos: Int = 0
    var posDelete: Int = 0
    var markerCheck = false

    private lateinit var mapView: MapView

    // 지도에서 직접 추가하기를 위한 중심 위치 point
    private lateinit var marker: MapPOIItem

    // LocationSearch recyclerview
    private val locationSearchList = arrayListOf<LocationSearchData>()
    private lateinit var locationSearchAdapter: LocationSearchAdapter

    // SearchResult recyclerview
    private val searchResultList = arrayListOf<SearchResultData>()
    val searchResultAdapter = SearchResultAdapter(searchResultList)

    // viewpager 설정
    private val viewpagerList = arrayListOf<ViewPagerData>()
    var sheetView: DialogAddGroupBottomSheetBinding? = null
    private var mActivity: MainActivity? = null
    private val viewpagerAdapter: ViewPagerAdapter by lazy {
        ViewPagerAdapter(
            viewpagerList,
            mActivity as Context
        )
    }
    private var keyword = "" // 검색 키워드
    private var doubleBackToExitPressedOnce = false
    var gid: Long = 0
    var itemId: Long = 0
    var groupNum = 0

    // 반경 3km, 5km 버튼 클릭되었는지 check
    var threeCheck = false
    var fiveCheck = false
    var plusBtn = false
    var bottomGPSBtn = false
    var kmBtn = false
    var GPSBtn = false
    var latitude = 0.0
    var longitude = 0.0
    var address = ""

    override val viewModel by viewModels<MapViewModel> {
        MapViewModel.MapViewModelFactory()
    }

    override fun initViewModel(viewModel: ViewModel) {
        binding.setVariable(BR.viewModel, viewModel)
        binding.lifecycleOwner = this@MapFragment
        binding.fragment = this@MapFragment
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivity) {
            mActivity = context
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("ResourceAsColor")
    override fun onCreateAction() {
        mapView = MapView(context)
        binding.kakaoMapContainer.addView(mapView)
        val mapPoint = MapPoint.mapPointWithGeoCoord(
            GajaMapApplication.prefs.getString("mapLatitude", "37.7").toDouble(),
            GajaMapApplication.prefs.getString("mapLongitude", "127").toDouble()
        )
        mapView.setMapCenterPoint(mapPoint, true)

        locationSearchAdapter = LocationSearchAdapter(requireContext(), locationSearchList)
        // 지도 타일 이미지 Persistent Cache 기능 : 네트워크를 통해 다운로드한 지도 이미지 데이터를 단말의 영구(persistent) 캐쉬 영역에 저장하는 기능
        setMapTilePersistentCacheEnabled(true)

        val groupDialog = BottomSheetDialog(requireContext(), R.style.BottomSheetTheme)
        sheetView = DialogAddGroupBottomSheetBinding.inflate(layoutInflater)

        // 검색결과 recyclerview 크기 아이템 개수에 따라 조절
        val maxRecyclerViewHeight =
            resources.getDimensionPixelSize(R.dimen.max_recycler_view_height)
        val itemHeight = resources.getDimensionPixelSize(R.dimen.item_height)

        // 자동 로그인 response 데이터 값 받아오기
        val groupInfo = UserData.groupinfo

        clientMarker()

        // 그룹 더보기 및 검색창 그룹 이름, 현재 선택된 이름으로 변경
        if (groupInfo != null) {
            binding.tvSearch.text = groupInfo.groupName
            sheetView!!.tvAddgroupMain.text = groupInfo.groupName
        }

        mapView.setMapViewEventListener(this)
        mapView.setPOIItemEventListener(this)

        binding.vpClient.orientation = ViewPager2.ORIENTATION_HORIZONTAL // 방향을 가로로

        // 추가한 그룹이 존재하는지 확인한 뒤에 그룹을 추가하라는 다이얼로그를 띄울지 말지 결정해야 하기에 일단 여기에서 호출
        checkGroup()

        // GPS 권한 설정
        binding.ibGps.setOnClickListener {

            if (!GPSBtn) {
                if (checkLocationService()) {

                    if (ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestPermission()
                        return@setOnClickListener
                    }

                    GPSBtn = true
                    bottomGPSBtn = true

                    // GPS가 켜져있을 경우
                    getLocation()

                    // gps 버튼 클릭 상태로 변경
                    // 원을 유지한 상태로 drawable 색상만 변경할 때 사용
                    val bgShape = binding.ibGps.background as GradientDrawable
                    bgShape.setColor(resources.getColor(R.color.main))
                    binding.ibGps.setImageResource(R.drawable.ic_white_gps)

                    val bgShape2 = binding.ibBottomGps.background as GradientDrawable
                    bgShape2.setColor(resources.getColor(R.color.main))
                    binding.ibBottomGps.setImageResource(R.drawable.ic_white_gps)
                } else {
                    // GPS가 꺼져있을 경우 클릭한 상태가 아님
                    Toast.makeText(requireContext(), "GPS를 켜주세요", Toast.LENGTH_SHORT).show()
                }
            } else {

                GPSBtn = false
                val bgShape = binding.ibGps.background as GradientDrawable
                bgShape.setColor(resources.getColor(R.color.white))
                binding.ibGps.setImageResource(R.drawable.ic_gray_gps)

                bottomGPSBtn = false
                val bgShape2 = binding.ibBottomGps.background as GradientDrawable
                bgShape2.setColor(resources.getColor(R.color.white))
                binding.ibBottomGps.setImageResource(R.drawable.ic_gray_gps)
                stopTracking()
                mapView.setShowCurrentLocationMarker(false)
            }
        }

        // 지도에 직접 위치 추가하기 클릭시 보이는 GPS 버튼에 대한 위치 권한 설정
        binding.ibBottomGps.setOnClickListener {
            // gps 버튼 클릭 상태로 변경
            // 원을 유지한 상태로 drawable 색상만 변경할 때 사용
            if (!bottomGPSBtn) {
                if (checkLocationService()) {

                    if (ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // 전화걸기 권한을 요청합니다.
                        requestPermission()
                        return@setOnClickListener
                    }
                    bottomGPSBtn = true
                    GPSBtn = true
                    binding.tvLocationAddress.text = "내 위치 검색중..."
                    // GPS가 켜져있을 경우
                    val a = getLocation()

                    val bgShape = binding.ibBottomGps.background as GradientDrawable
                    bgShape.setColor(resources.getColor(R.color.main))
                    binding.ibBottomGps.setImageResource(R.drawable.ic_white_gps)

                    val bgShape2 = binding.ibGps.background as GradientDrawable
                    bgShape2.setColor(resources.getColor(R.color.main))
                    binding.ibGps.setImageResource(R.drawable.ic_white_gps)

                    mapView.removePOIItem(marker)
                    // 지도에서 직접 추가하기 마커 위치
                    marker = MapPOIItem()
                    marker.itemName = "Marker"
                    marker.mapPoint = MapPoint.mapPointWithGeoCoord(a.first, a.second)
                    marker.markerType = MapPOIItem.MarkerType.RedPin
                    mapView.addPOIItem(marker)
                    reverseGeoCoderFoundAddress(
                        marker.mapPoint.mapPointGeoCoord.longitude.toString(),
                        marker.mapPoint.mapPointGeoCoord.latitude.toString()
                    )
                    latitude = marker.mapPoint.mapPointGeoCoord.latitude
                    longitude = marker.mapPoint.mapPointGeoCoord.longitude
                    markerCheck = true
                } else {
                    // GPS가 꺼져있을 경우
                    Toast.makeText(requireContext(), "GPS를 켜주세요", Toast.LENGTH_SHORT).show()
                }
            }
            // 두 번 클릭 시 원상태로 돌아오게 하기 및 위치 추적 중지
            else {
                bottomGPSBtn = false
                val bgShape = binding.ibBottomGps.background as GradientDrawable
                bgShape.setColor(resources.getColor(R.color.white))
                binding.ibBottomGps.setImageResource(R.drawable.ic_gray_gps)

                GPSBtn = false
                val bgShape2 = binding.ibGps.background as GradientDrawable
                bgShape2.setColor(resources.getColor(R.color.white))
                binding.ibGps.setImageResource(R.drawable.ic_gray_gps)
                stopTracking()
                mapView.setShowCurrentLocationMarker(false)
            }
        }

        // groupListAdapter를 우선적으로 초기화해줘야 함
        groupListAdapter = GroupListAdapter(object : GroupListAdapter.GroupDeleteListener {
            override fun click(id: Long, name: String, position: Int) {
                // 그룹 삭제 dialog
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("해당 그룹을 삭제하시겠습니까?")
                    .setMessage("그룹을 삭제하시면 영구 삭제되어 복구할 수 없습니다.")
                    .setPositiveButton("확인") { _: DialogInterface, _: Int ->
                        // 그룹 삭제 서버 연동 함수 호출
                        deleteGroup(gid, position)
                        groupDialog.hide()
                        Toast.makeText(requireContext(), "그룹 삭제 완료", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("취소") { _: DialogInterface, _: Int ->
                    }
                val alertDialog = builder.create()
                alertDialog.show()
                gName = name
                posDelete = position
                gid = id
            }
        }, object : GroupListAdapter.GroupEditListener {
            override fun click2(id: Long, name: String, position: Int) {
                // 그룹 수정 dialog
                val mDialogView = DialogGroupBinding.inflate(layoutInflater)
                val mBuilder = AlertDialog.Builder(requireContext())
                val addDialog = mBuilder.create()
                addDialog.setView(mDialogView.root)
                addDialog.show()
                gid = id

                mDialogView.ivClose.setOnClickListener {
                    addDialog.dismiss()
                }

                mDialogView.btnDialogSubmit.setOnClickListener {
                    // 그룹 수정 api 연동

                    if (mDialogView.etName.text.toString() == "전체" ||
                        mDialogView.etName.text.toString().isEmpty()
                    ) {
                        Toast.makeText(requireContext(), "사용할 수 없는 그룹 이름입니다", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        modifyGroup(gid, mDialogView.etName.text.toString(), position)
                        addDialog.dismiss()
                    }
                }
            }
        })

        // 그룹 recyclerview 아이템 클릭 시 값 변경 및 배경색 바꾸기
        groupListAdapter.setItemClickListener(object : GroupListAdapter.OnItemClickListener {
            override fun onClick(v: View, position: Int, gid: Long, gname: String) {
                itemId = gid
                binding.tvSearch.text = gname
                sheetView!!.tvAddgroupMain.text = gname
                pos = position

                // 그룹 더보기 아이템 클릭 시 반경 버튼 비활성화
                // 만약 3km나 5km 버튼이 활성화되어있을 경우는 km버튼도 활성화 되어있는 것이므로 같이 처리
                if (threeCheck) {
                    threeCheck = false
                    binding.btn3km.setBackgroundResource(R.drawable.bg_km_notclick)
                    binding.btn3km.setTextColor(resources.getColor(R.color.main))
                }
                if (fiveCheck) {
                    fiveCheck = false
                    binding.btn5km.setBackgroundResource(R.drawable.bg_km_notclick)
                    binding.btn5km.setTextColor(resources.getColor(R.color.main))
                }

                kmBtn = false
                val bgShapebtn = binding.ibKm.background as GradientDrawable
                bgShapebtn.setColor(resources.getColor(R.color.white))
                binding.ibKm.setImageResource(R.drawable.ic_km)
                binding.clKm.visibility = View.GONE

                binding.clSearchResult.visibility = View.GONE
                binding.clCardview.visibility = View.GONE

                if (position == 0) {
                    getAllClient()
                } else {
                    getGroupClient(gid, gname)
                }

                groupDialog.hide()
            }
        })

        // search bar 클릭 시 바텀 다이얼로그 띄우기
        binding.clSearch.setOnClickListener {
            // 그룹 더보기 바텀 다이얼로그 띄우기
            sheetView!!.rvAddgroup.adapter = groupListAdapter

            groupDialog.setContentView(sheetView!!.root)
            groupDialog.show()

            sheetView!!.btnAddgroup.setOnClickListener {
                // 그룹 추가 dialog
                val mDialogView = DialogGroupBinding.inflate(layoutInflater)
                mDialogView.tvTitle.text = "그룹 추가하기"
                val mBuilder = AlertDialog.Builder(requireContext())
                val addDialog = mBuilder.create()
                addDialog.setView(mDialogView.root)
                addDialog.show()

                mDialogView.ivClose.setOnClickListener {
                    addDialog.dismiss()
                }

                mDialogView.btnDialogSubmit.setOnClickListener {
                    // 그룹 생성 api 연동
                    if (mDialogView.etName.text.toString() == "전체" ||
                        mDialogView.etName.text.toString().isEmpty()
                    ) {
                        Toast.makeText(requireContext(), "사용할 수 없는 그룹 이름입니다", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        createGroup(mDialogView.etName.text.toString())
                        addDialog.dismiss()
                    }
                }
            }
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                searchResultList.clear()
                val size = UserData.clientListResponse?.clients!!.size
                for (i in 0 until size) {
                    val name = UserData.clientListResponse?.clients!![i].clientName
                    val latitude = UserData.clientListResponse?.clients!![i].location.latitude
                    if (name.contains(binding.etSearch.text.toString()) && latitude != null) {
                        searchResultList.add(SearchResultData(name, i))
                    }
                }

                binding.rvSearch.adapter = searchResultAdapter
                val itemCount = searchResultList.size
                // 최대 크기와 비교하여 결정
                val calculatedRecyclerViewHeight =
                    min(itemHeight * itemCount, maxRecyclerViewHeight)
                // RecyclerView의 높이를 동적으로 설정
                binding.rvSearch.layoutParams.height = calculatedRecyclerViewHeight
                searchResultAdapter.notifyDataSetChanged()

                binding.clSearchResult.visibility = View.VISIBLE
            }

            override fun afterTextChanged(p0: Editable?) {

            }
        })

        // 검색 결과 recyclerview 아이템 클릭 시 해당 고객에 대한 마커 위치로 이동
        searchResultAdapter.setItemClickListener(object : SearchResultAdapter.OnItemClickListener {
            override fun onClick(v: View, position: Int, index: Int) {
                val itemData = UserData.clientListResponse?.clients?.get(index)
                val mapPoint = MapPoint.mapPointWithGeoCoord(
                    itemData!!.location.latitude!!,
                    itemData.location.longitude!!
                )
                mapView.setMapCenterPoint(mapPoint, true)
            }
        })

        // plus버튼, 지도에 직접 추가하기 dialog 보여짐
        binding.ibPlus.setOnClickListener {
            checkGroup()
            if (groupNum == 1) {
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("현재 생성된 그룹이 없습니다.")
                    .setMessage("그룹을 생성 하시겠습니까?")
                    .setPositiveButton("확인") { dialog, which ->
                        sheetView!!.rvAddgroup.adapter = groupListAdapter

                        groupDialog.setContentView(sheetView!!.root)
                        groupDialog.show()

                        val mDialogView = DialogGroupBinding.inflate(layoutInflater)
                        mDialogView.tvTitle.text = "그룹 추가하기"
                        val mBuilder = AlertDialog.Builder(requireContext())
                        val addDialog = mBuilder.create()
                        addDialog.setView(mDialogView.root)
                        addDialog.show()

                        mDialogView.ivClose.setOnClickListener {
                            addDialog.dismiss()
                        }

                        mDialogView.btnDialogSubmit.setOnClickListener {
                            // 그룹 생성 api 연동
                            if (mDialogView.etName.text.toString() == "전체" ||
                                mDialogView.etName.text.toString().isEmpty()
                            ) {
                                Toast.makeText(
                                    requireContext(),
                                    "사용할 수 없는 그룹 이름입니다",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            } else {
                                createGroup(mDialogView.etName.text.toString())
                                addDialog.dismiss()
                            }
                        }
                    }
                    .setNegativeButton("취소") { dialog, which ->

                    }
                val alertDialog = builder.create()
                alertDialog.show()
            } else {
                if (!plusBtn) {
                    // plus 버튼 클릭 상태로 변경
                    plusBtn = true
                    val bgShape = binding.ibPlus.background as GradientDrawable
                    bgShape.setColor(resources.getColor(R.color.main))
                    binding.ibPlus.setImageResource(R.drawable.ic_white_plus)

                    mapView.removeAllPOIItems()
                    // 화면 변경
                    binding.clSearchWhole.visibility = View.GONE
                    binding.clSearchLocation.visibility = View.VISIBLE
                    binding.clLocation.visibility = View.VISIBLE
                    binding.ibPlus.visibility = View.GONE
                    binding.ibGps.visibility = View.GONE
                    binding.ibKm.visibility = View.GONE

                    if (threeCheck || fiveCheck) {
                        binding.clKm.visibility = View.GONE
                    } else {
                        binding.clKm.visibility = View.GONE
                        val bgShape2 = binding.ibKm.background as GradientDrawable
                        bgShape2.setColor(resources.getColor(R.color.white))
                        binding.ibKm.setImageResource(R.drawable.ic_km)
                    }

                    // 지도에서 직접 추가하기 마커 위치
                    val centerPoint = mapView.mapCenterPoint
                    marker = MapPOIItem()
                    mapView.setMapCenterPoint(centerPoint, true)
                    marker.itemName = "마커"
                    marker.isShowCalloutBalloonOnTouch = false
                    marker.mapPoint = MapPoint.mapPointWithGeoCoord(
                        mapView.mapCenterPoint.mapPointGeoCoord.latitude,
                        mapView.mapCenterPoint.mapPointGeoCoord.longitude
                    )
                    latitude = mapView.mapCenterPoint.mapPointGeoCoord.latitude
                    longitude = mapView.mapCenterPoint.mapPointGeoCoord.longitude
                    marker.markerType = MapPOIItem.MarkerType.RedPin
                    mapView.addPOIItem(marker)
                    reverseGeoCoderFoundAddress(longitude.toString(), latitude.toString())
                    markerCheck = true
                } else {
                    // plus 버튼 클릭하지 않은 상태로 변경
                    plusBtn = false
                    val bgShape = binding.ibPlus.background as GradientDrawable
                    bgShape.setColor(resources.getColor(R.color.white))
                    binding.ibPlus.setImageResource(R.drawable.ic_plus)
                }
            }

        }

        // km 메인 버튼 클릭 이벤트, 3km와 5km 버튼 띄우기
        binding.ibKm.setOnClickListener {
            if (!kmBtn) {
                if (checkLocationService()) {
                    if (ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestPermission()
                        return@setOnClickListener
                    }

                    searchLocationsGPS()

                    if (!GPSBtn) {
                        stopTracking()
                    }
                } else {
                    Toast.makeText(requireContext(), "GPS를 켜주세요", Toast.LENGTH_SHORT).show()
                }

                if (threeCheck || fiveCheck) {
                    binding.clKm.visibility = View.GONE
                    kmBtn = true
                }
            } else {
                if (threeCheck || fiveCheck) {
                    binding.clKm.visibility = View.VISIBLE
                } else {
                    val bgShape = binding.ibKm.background as GradientDrawable
                    bgShape.setColor(resources.getColor(R.color.white))
                    binding.ibKm.setImageResource(R.drawable.ic_km)
                    binding.clKm.visibility = View.GONE
                }
                kmBtn = false
            }
        }

        // LocationSearch recyclerview
        binding.rvLocation.adapter = locationSearchAdapter
        // recyclerview 아이템 클릭 시 해당 위치로 이동
        locationSearchAdapter.setItemClickListener(object :
            LocationSearchAdapter.OnItemClickListener {
            override fun onClick(v: View, position: Int, text: String) {
                val mapPoint = MapPoint.mapPointWithGeoCoord(
                    locationSearchList[position].y,
                    locationSearchList[position].x
                )
                mapView.setMapCenterPoint(mapPoint, true)

            }
        })

        locationSearchAdapter.setItemClickListener2(object :
            LocationSearchAdapter.OnItemClickListener2 {
            override fun onClick(v: View, position: Int, text: String) {
                val intent = Intent(context, AddDirectActivity::class.java)
                intent.putExtra("latitude", locationSearchList[position].y)
                intent.putExtra("longitude", locationSearchList[position].x)
                intent.putExtra("address", text)
                context?.startActivity(intent)

            }
        })

        // 오른쪽 화살표를 누르면 화면 전환되는 것으로 구현
        binding.tvLocationSearchGo.setOnClickListener {
            dialogShow()
            binding.clLocationSearch.visibility = View.VISIBLE
            binding.clLocation.visibility = View.GONE
            // 검색 키워드 받기
            keyword = binding.etLocationSearch.text.toString()
            searchKeyword(keyword)
        }

        // edittext 완료 클릭 시 화면 전환되는 것으로 추가 구현
        binding.etLocationSearch.setOnKeyListener { view, i, keyEvent ->
            // Enter Key Action
            if (keyEvent.action == KeyEvent.ACTION_DOWN && i == KeyEvent.KEYCODE_ENTER) {
                dialogShow()
                // 키패드 내리기
                val imm =
                    requireContext().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.etLocationSearch.windowToken, 0)
                binding.clLocationSearch.visibility = View.VISIBLE
                binding.clLocation.visibility = View.GONE
                // 검색 키워드 받기
                keyword = binding.etLocationSearch.text.toString()
                searchKeyword(keyword)
                true
            }
            false
        }

        binding.tvLocationBtn.setOnClickListener {
            // 고객 추가하기 activity로 이동
            val intent = Intent(activity, AddDirectActivity::class.java)
            intent.putExtra("address", address)
            intent.putExtra("latitude", latitude)
            intent.putExtra("longitude", longitude)
            startActivity(intent)
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (doubleBackToExitPressedOnce) {
                        // 2초 내에 다시 뒤로가기 버튼을 누르면 앱을 종료합니다.
                        requireActivity().finish()
                    } else {
                        doubleBackToExitPressedOnce = true
                        Toast.makeText(requireContext(), "한번 더 누르면 종료 됩니다.", Toast.LENGTH_SHORT)
                            .show()

                        // 2초 후에 doubleBackToExitPressedOnce 값을 초기화합니다.
                        Handler(Looper.getMainLooper()).postDelayed({
                            doubleBackToExitPressedOnce = false
                        }, 2000)
                    }
                }
            })
    }

    // 그룹 생성 api
    private fun createGroup(name: String) {
        viewModel.createGroup(CreateGroupRequest(name))
        viewModel.createGroup.observe(this, Observer {
            groupListAdapter.setData(viewModel.checkGroup.value!!)
        })
        viewModel.checkErrorGroup.observe(this, Observer {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        })
    }

    // 그룹 조회 api
    private fun checkGroup() {
        viewModel.checkGroup()
        viewModel.checkGroup.observe(this@MapFragment, Observer {
            groupListAdapter.setData(it)
            groupNum = viewModel.checkGroup.value!!.size
        })
        viewModel.checkErrorGroup.observe(this, Observer {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        })
    }

    // 그룹 삭제 api
    private fun deleteGroup(groupId: Long, position: Int) {
        viewModel.deleteGroup(groupId, position)
        viewModel.deleteGroup.observe(this, Observer {
            groupListAdapter.setData(it)
            // 현재 선택한 리사이클러뷰 아이템의 그룹을 삭제했을 경우
            // 전체 고객을 조회하는 api 호출 후 전체 고객 마커 찍고 UserData 값 변경
            if (posDelete == position) {
                getAllClient()
                binding.tvSearch.text = "전체"
                sheetView!!.tvAddgroupMain.text = "전체"
            }
        })
    }

    // 그룹 수정 api
    private fun modifyGroup(groupId: Long, name: String, position: Int) {
        viewModel.modifyGroup(groupId, CreateGroupRequest(name), position)
        viewModel.modifyGroup.observe(this, Observer {
            groupListAdapter.setData(it)

            // 변경한 그룹 이름 저장 데이터에도 갱신
            UserData.groupinfo!!.groupName = name

            // 현재 선택한 리사이클러뷰 아이템의 그룹 이름을 변경했을 경우
            if (pos == position) {
                binding.tvSearch.text = name
                sheetView!!.tvAddgroupMain.text = name
            }
        })
    }

    // 전체 고객 대상 반경 검색 api
    private fun wholeRadius(radius: Int, latitude: Double, longitude: Double) {
        viewModel.wholeRadius(radius, latitude, longitude)
        viewModel.wholeRadius.observe(this, Observer {
            if (viewModel.wholeRadius.value != null) {
                UserData.clientListResponse = viewModel.wholeRadius.value
                val data = viewModel.wholeRadius.value!!.clients
                val num = data.count()

                mapView.removeAllPOIItems()
                for (i in 0 until num) {
                    val itemdata = data[i]
                    // 지도에 마커 추가
                    val point = MapPOIItem()
                    point.apply {
                        itemName = itemdata.clientName
                        tag = itemdata.clientId.toInt()
                        mapPoint = MapPoint.mapPointWithGeoCoord(
                            itemdata.location.latitude!!,
                            itemdata.location.longitude!!
                        )
                        userObject = itemdata.location
                        markerType = MapPOIItem.MarkerType.RedPin
                        selectedMarkerType = MapPOIItem.MarkerType.YellowPin
                    }
                    mapView.addPOIItem(point)
                }
            }
        })
        viewModel.checkErrorGroup.observe(this, Observer {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        })
    }

    // 특정 그룹 내에 고객 대상 반경 검색 api
    private fun specificRadius(radius: Int, latitude: Double, longitude: Double, groupId: Long) {
        viewModel.specificRadius(radius, latitude, longitude, groupId)
        viewModel.specificRadius.observe(this, Observer {
            if (viewModel.specificRadius.value != null) {
                UserData.clientListResponse = viewModel.specificRadius.value
                val data = viewModel.specificRadius.value!!.clients
                val num = data.count()

                mapView.removeAllPOIItems()
                for (i in 0 until num) {
                    val itemdata = data[i]
                    // 지도에 마커 추가
                    val point = MapPOIItem()
                    point.apply {
                        itemName = itemdata.clientName
                        tag = itemdata.clientId.toInt()
                        mapPoint = MapPoint.mapPointWithGeoCoord(
                            itemdata.location.latitude!!,
                            itemdata.location.longitude!!
                        )
                        userObject = itemdata.location
                        markerType = MapPOIItem.MarkerType.RedPin
                        selectedMarkerType = MapPOIItem.MarkerType.YellowPin
                    }
                    mapView.addPOIItem(point)
                }
            }
        })
        viewModel.checkErrorGroup.observe(this, Observer {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        })
    }

    // 특정 그룹 내에 고객 전부 조회 api
    private fun getGroupClient(groupId: Long, gname: String) {
        viewModel.getGroupAllClient(groupId)
        viewModel.groupClients.observe(this, Observer {
            val data = viewModel.groupClients.value!!.clients
            val num = data.count()

            // UserData 값 갱신
            UserData.clientListResponse = viewModel.groupClients.value
            // groupinfo 값도 변경
            UserData.groupinfo = AutoLoginGroupInfo(groupId, num, gname)

            mapView.removeAllPOIItems()
            for (i in 0 until num) {
                val itemdata = data[i]
                if (itemdata.location.latitude == null)
                    continue
                // 지도에 마커 추가
                val point = MapPOIItem()
                point.apply {
                    itemName = itemdata.clientName
                    tag = itemdata.clientId.toInt()
                    mapPoint = MapPoint.mapPointWithGeoCoord(
                        itemdata.location.latitude!!,
                        itemdata.location.longitude!!
                    )
                    userObject = itemdata.location
                    markerType = MapPOIItem.MarkerType.RedPin
                    selectedMarkerType = MapPOIItem.MarkerType.YellowPin
                }
                mapView.addPOIItem(point)
            }
        })
    }

    // 전체 고객 전부 조회 api
    private fun getAllClient() {
        viewModel.getAllClient()
        viewModel.allClients.observe(this, Observer {
            val data = viewModel.allClients.value!!.clients
            val num = data.count()

            // UserData 값 갱신
            UserData.clientListResponse = viewModel.allClients.value
            // groupinfo 값도 변경
            UserData.groupinfo = AutoLoginGroupInfo(-1, num, "전체")

            mapView.removeAllPOIItems()
            for (i in 0 until num) {
                val itemdata = data[i]
                if (itemdata.location.latitude == null)
                    continue
                // 지도에 마커 추가
                val point = MapPOIItem()
                point.apply {
                    itemName = itemdata.clientName
                    tag = itemdata.clientId.toInt()
                    mapPoint = MapPoint.mapPointWithGeoCoord(
                        itemdata.location.latitude!!,
                        itemdata.location.longitude!!
                    )
                    userObject = itemdata.location
                    markerType = MapPOIItem.MarkerType.RedPin
                    selectedMarkerType = MapPOIItem.MarkerType.YellowPin
                }
                mapView.addPOIItem(point)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (!binding.kakaoMapContainer.contains(mapView)) {
            try {
                mapView = MapView(context)
                binding.kakaoMapContainer.addView(mapView)
                mapView.setPOIItemEventListener(this)
                mapView.setMapViewEventListener(this)
            } catch (e: RuntimeException) {
                Log.d("error", e.toString())
            }
        }

        GPSBtn = false
        val bgShape = binding.ibGps.background as GradientDrawable
        bgShape.setColor(resources.getColor(R.color.white))
        binding.ibGps.setImageResource(R.drawable.ic_gray_gps)

        bottomGPSBtn = false
        val bgShape3 = binding.ibBottomGps.background as GradientDrawable
        bgShape3.setColor(resources.getColor(R.color.white))
        binding.ibBottomGps.setImageResource(R.drawable.ic_gray_gps)

        stopTracking()
        mapView.setShowCurrentLocationMarker(false)
        plusBtnInactivation()
        clientMarker()

        kmBtn = false
        threeCheck = false
        fiveCheck = false
        val bgShape2 = binding.ibKm.background as GradientDrawable
        bgShape2.setColor(resources.getColor(R.color.white))
        binding.ibKm.setImageResource(R.drawable.ic_km)
        binding.clKm.visibility = View.GONE

        binding.btn3km.setBackgroundResource(R.drawable.bg_km_notclick)
        binding.btn3km.setTextColor(resources.getColor(R.color.main))

        binding.btn5km.setBackgroundResource(R.drawable.bg_km_notclick)
        binding.btn5km.setTextColor(resources.getColor(R.color.main))

        if (binding.tvSearch.text == "전체") {
            getAllClient()
        } else {
            getGroupClient(UserData.groupinfo!!.groupId, UserData.groupinfo!!.groupName)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 저장된 지도 타일 캐쉬 데이터를 모두 삭제
        // MapView가 동작 중인 상태에서도 사용 가능
        clearMapTilePersistentCache()
//`        binding.kakaoMapContainer.removeView(mapView)`
    }

    override fun onPause() {
        super.onPause()
        binding.kakaoMapContainer.removeView(mapView)
    }

//    override fun onDestroyView() {
//        super.onDestroyView()
//        binding.kakaoMapContainer.removeView(mapView)
//    }

    // ViewPager에 들어갈 아이템
    private fun getClientList(userObject: Any) {
        viewpagerList.clear()
        val size = UserData.clientListResponse!!.clients.size

        for (i in 0..size - 1) {
            val itemdata = UserData.clientListResponse!!.clients.get(i)

            if (userObject == itemdata.location) {
                if (itemdata.image.filePath != null) {
                    if (itemdata.distance == null) {
                        viewpagerList.add(
                            ViewPagerData(
                                itemdata.clientId,
                                itemdata.groupInfo.groupId,
                                UserData.imageUrlPrefix + itemdata.image.filePath,
                                itemdata.clientName,
                                itemdata.address.mainAddress,
                                itemdata.address.detail,
                                itemdata.phoneNumber,
                                null,
                                itemdata.location.latitude,
                                itemdata.location.longitude
                            )
                        )

                    } else {
                        viewpagerList.add(
                            ViewPagerData(
                                itemdata.clientId,
                                itemdata.groupInfo.groupId,
                                UserData.imageUrlPrefix + itemdata.image.filePath,
                                itemdata.clientName,
                                itemdata.address.mainAddress,
                                itemdata.address.detail,
                                itemdata.phoneNumber,
                                itemdata.distance,
                                itemdata.location.latitude,
                                itemdata.location.longitude
                            )
                        )
                    }
                } else {
                    if (itemdata.distance == null) {
                        viewpagerList.add(
                            ViewPagerData(
                                itemdata.clientId,
                                itemdata.groupInfo.groupId,
                                "null",
                                itemdata.clientName,
                                itemdata.address.mainAddress,
                                itemdata.address.detail,
                                itemdata.phoneNumber,
                                null,
                                itemdata.location.latitude,
                                itemdata.location.longitude
                            )
                        )

                    } else {
                        viewpagerList.add(
                            ViewPagerData(
                                itemdata.clientId,
                                itemdata.groupInfo.groupId,
                                "null",
                                itemdata.clientName,
                                itemdata.address.mainAddress,
                                itemdata.address.detail,
                                itemdata.phoneNumber,
                                itemdata.distance,
                                itemdata.location.latitude,
                                itemdata.location.longitude
                            )
                        )
                    }
                }
            }
        }

        binding.vpClient.adapter = viewpagerAdapter
        searchResultAdapter.notifyDataSetChanged()
    }

    // 키워드 검색 함수
    private fun searchKeyword(keyword: String) {
        // API 서버에 요청
        KakaoSearchClient.kakaoSearchService?.getSearchKeyword(
            BuildConfig.KAKAO_REST_API_KEY,
            keyword,
            1
        )?.enqueue(object : Callback<ResultSearchKeywordData> {
            override fun onResponse(
                call: Call<ResultSearchKeywordData>,
                response: Response<ResultSearchKeywordData>
            ) {
                if (response.isSuccessful) {
                    // 직접 지도에 추가하기 위해 기존에 존재한 마커는 없애주기
                    mapView.removePOIItem(marker)
                    markerCheck = false
                    addItemsAndMarkers(response.body())
                    Log.d("LocationSearch", "Success : ${response.body()}")
                } else {  /// 이곳은 에러 발생할 경우 실행됨
                    Log.d("LocationSearch", "fail : ${response.code()}")
                }
                dialogHide()
            }

            override fun onFailure(call: Call<ResultSearchKeywordData>, t: Throwable) {
                Log.w("LocalSearch", "통신 실패: ${t.message}")
            }
        })
    }

    // 검색 결과 처리 함수
    private fun addItemsAndMarkers(searchResult: ResultSearchKeywordData?) {
        if (!searchResult?.documents.isNullOrEmpty()) {
            // 검색 결과 있을 경우
            locationSearchList.clear()           // 리사이클러뷰 초기화

            mapView.removeAllPOIItems()  // 지도의 마커 모두 제거
            for (document in searchResult!!.documents) {
                // 결과를 리사이클러뷰에 추가
                val item = LocationSearchData(
                    document.place_name,
                    document.road_address_name,
                    document.address_name,
                    document.x.toDouble(),
                    document.y.toDouble()
                )
                locationSearchList.add(item)
                // 지도에 마커 추가
                val point = MapPOIItem()
                point.apply {
                    itemName = document.place_name
                    mapPoint =
                        MapPoint.mapPointWithGeoCoord(document.y.toDouble(), document.x.toDouble())
                    markerType = MapPOIItem.MarkerType.BluePin
                    selectedMarkerType = MapPOIItem.MarkerType.RedPin
                }
                mapView.addPOIItem(point)
            }
            locationSearchAdapter.notifyDataSetChanged()
        } else {
        }
    }

    private fun getLocation(): Pair<Double, Double> {
        var userLatitude = 0.0
        var userLongitude = 0.0

        stopTracking()
        startTracking()

        val lm: LocationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val userNowLocation: Location? = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        userNowLocation?.let {
            userLatitude = it.latitude
            userLongitude = it.longitude
        }

        return Pair(userLatitude, userLongitude)
    }

    // GPS가 켜져있는지 확인
    private fun checkLocationService(): Boolean {
        val locationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun clientMarker() {
        // MapFragment 띄우자마자 현재 선택된 고객들의 위치 마커 찍기
        val clientNum = UserData.clientListResponse!!.clients.size

        for (i in 0 until clientNum) {

            val itemdata = UserData.clientListResponse!!.clients[i]
            if (itemdata.location.latitude == null)
                continue
            // 지도에 마커 추가
            val point = MapPOIItem()
            point.apply {
                itemName = itemdata.clientName
                tag = itemdata.clientId.toInt()
                mapPoint = MapPoint.mapPointWithGeoCoord(
                    itemdata.location.latitude!!,
                    itemdata.location.longitude!!
                )
                userObject = itemdata.location
                markerType = MapPOIItem.MarkerType.RedPin
                selectedMarkerType = MapPOIItem.MarkerType.YellowPin
            }
            mapView.addPOIItem(point)
        }
    }

    private fun searchLocationsGPS() {
        if (checkLocationService()) {
            val a = getLocation()
            kmBtn = true
            val bgShape = binding.ibKm.background as GradientDrawable
            bgShape.setColor(resources.getColor(R.color.main))
            binding.ibKm.setImageResource(R.drawable.ic_white_km)
            binding.clKm.visibility = View.VISIBLE

            // 자신의 현재 위치를 기준으로 반경 3km, 5km에 위치한 전체 고객 정보 가져오기
            binding.btn3km.setOnClickListener {
                if (!threeCheck) {
                    if (fiveCheck) {
                        fiveCheck = false
                        binding.btn5km.setBackgroundResource(R.drawable.bg_km_notclick)
                        binding.btn5km.setTextColor(resources.getColor(R.color.main))
                    }
                    threeCheck = true
                    binding.btn3km.setBackgroundResource(R.drawable.bg_km_click)
                    binding.btn3km.setTextColor(resources.getColor(R.color.white))

                    if (a.first != 0.0 && a.second != 0.0) {
                        if (binding.tvSearch.text == "전체") {
                            wholeRadius(3000, a.first, a.second)
                        } else {
                            specificRadius(3000, a.first, a.second, UserData.groupinfo!!.groupId)
                        }
                        mapView.setMapCenterPoint(
                            MapPoint.mapPointWithGeoCoord(
                                a.first,
                                a.second
                            ), false
                        )
                    }
                } else {
                    threeCheck = false
                    mapView.removeAllPOIItems()  // 지도의 마커 모두 제거
                    binding.btn3km.setBackgroundResource(R.drawable.bg_km_notclick)
                    binding.btn3km.setTextColor(resources.getColor(R.color.main))
                }
                // 3km 버튼 누를 시 해당 버튼 창 없애기
                binding.clKm.visibility = View.GONE

                if (!threeCheck && !fiveCheck) {
                    kmBtn = false
                    val bgShapebtn = binding.ibKm.background as GradientDrawable
                    bgShapebtn.setColor(resources.getColor(R.color.white))
                    binding.ibKm.setImageResource(R.drawable.ic_km)
                    if (binding.tvSearch.text == "전체") {
                        getAllClient()
                    } else {
                        getGroupClient(UserData.groupinfo!!.groupId, UserData.groupinfo!!.groupName)
                    }
                }
            }

            binding.btn5km.setOnClickListener {
                if (!fiveCheck) {
                    if (threeCheck) {
                        threeCheck = false
                        binding.btn3km.setBackgroundResource(R.drawable.bg_km_notclick)
                        binding.btn3km.setTextColor(resources.getColor(R.color.main))
                    }
                    fiveCheck = true
                    binding.btn5km.setBackgroundResource(R.drawable.bg_km_click)
                    binding.btn5km.setTextColor(resources.getColor(R.color.white))

                    if (a.first != 0.0 && a.second != 0.0) {
                        if (binding.tvSearch.text == "전체") {
                            wholeRadius(5000, a.first, a.second)
                        } else {
                            specificRadius(5000, a.first, a.second, UserData.groupinfo!!.groupId)
                        }
                        mapView.setMapCenterPoint(
                            MapPoint.mapPointWithGeoCoord(
                                a.first,
                                a.second
                            ), false
                        )
                    }
                } else {
                    fiveCheck = false
                    mapView.removeAllPOIItems()  // 지도의 마커 모두 제거
                    binding.btn5km.setBackgroundResource(R.drawable.bg_km_notclick)
                    binding.btn5km.setTextColor(resources.getColor(R.color.main))
                }

                // 5km 버튼 누를 시 해당 버튼 창 없애기
                binding.clKm.visibility = View.GONE

                if (!threeCheck && !fiveCheck) {
                    kmBtn = false
                    val bgShapebtn = binding.ibKm.background as GradientDrawable
                    bgShapebtn.setColor(resources.getColor(R.color.white))
                    binding.ibKm.setImageResource(R.drawable.ic_km)
                    if (binding.tvSearch.text == "전체") {
                        getAllClient()
                    } else {
                        getGroupClient(UserData.groupinfo!!.groupId, UserData.groupinfo!!.groupName)
                    }
                }
            }
        } else {
            // GPS가 꺼져있을 경우
            Toast.makeText(requireContext(), "GPS를 켜주세요", Toast.LENGTH_SHORT).show()
        }
    }

    private fun reverseGeoCoderFoundAddress(x: String, y: String) {
        KakaoSearchClient.kakaoSearchService?.getCoord2address(
            BuildConfig.KAKAO_REST_API_KEY, x, y
        )
            ?.enqueue(object : Callback<ResultSearchCoord2addressData> {

                override fun onResponse(
                    call: Call<ResultSearchCoord2addressData>,
                    response: Response<ResultSearchCoord2addressData>
                ) {
                    if (response.isSuccessful) {
                        // 직접 지도에 추가하기 위해 기존에 존재한 마커는 없애주기
                        if (response.body()!!.meta.total_count >= 1) {
                            if (response.body()!!.documents[0].road_address == null) {
                                binding.tvLocationAddress.text =
                                    response.body()!!.documents[0].address.address_name
                                address = binding.tvLocationAddress.text as String

                                binding.tvLocationBtn.isEnabled = true
                                binding.tvLocationBtn.setBackgroundResource(R.drawable.fragment_add_bottom_purple)
                            } else {
                                binding.tvLocationAddress.text =
                                    response.body()!!.documents[0].road_address.address_name
                                address = binding.tvLocationAddress.text as String

                                binding.tvLocationBtn.isEnabled = true
                                binding.tvLocationBtn.setBackgroundResource(R.drawable.fragment_add_bottom_purple)
                            }
                        } else {
                            binding.tvLocationBtn.isEnabled = false
                            binding.tvLocationBtn.setBackgroundResource(R.drawable.bg_notworkbtn)
                        }

                        Log.d("LocationSearch", "Success : ${response.body()}")
                    } else {  /// 이곳은 에러 발생할 경우 실행됨
                        Log.d("LocationSearch", "fail : ${response.code()}")
                    }
                }

                override fun onFailure(
                    call: Call<ResultSearchCoord2addressData>,
                    t: Throwable
                ) {
                    Log.w("LocalSearch", "통신 실패: ${t.message}")
                }
            })
    }

    private fun plusBtnInactivation() {
        plusBtn = false
        val bgShape = binding.ibPlus.background as GradientDrawable
        bgShape.setColor(resources.getColor(R.color.white))
        binding.ibPlus.setImageResource(R.drawable.ic_plus)
        binding.clSearchWhole.visibility = View.VISIBLE
        binding.clSearchLocation.visibility = View.GONE
        binding.clLocation.visibility = View.GONE
        binding.ibPlus.visibility = View.VISIBLE
        binding.ibGps.visibility = View.VISIBLE
        binding.ibKm.visibility = View.VISIBLE
        mapView.removeAllPOIItems()
        binding.clLocationSearch.visibility = View.GONE
        binding.clCardview.visibility = View.GONE
        markerCheck = false
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestPermission() {
        TedPermission.create()
            .setPermissionListener(object : PermissionListener {
                override fun onPermissionGranted() {
                }

                override fun onPermissionDenied(deniedPermissions: List<String>) {
                    Toast.makeText(
                        requireContext(),
                        "권한 거부\n$deniedPermissions",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
            .setDeniedMessage("권한을 허용해주세요. [설정] > [앱 및 알림] > [고급] > [앱 권한]")
            .setPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
            .check()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            return
        }

        checkGroup()
        plusBtnInactivation()
        clientMarker()

        binding.clSearchResult.visibility = View.GONE

        // 그룹 더보기 및 검색창 그룹 이름, 현재 선택된 이름으로 변경
        if (UserData.groupinfo != null) {
            binding.tvSearch.text = UserData.groupinfo!!.groupName
            sheetView!!.tvAddgroupMain.text = UserData.groupinfo!!.groupName
        }
    }

    private fun dialogShow() {
        binding.progress.isVisible = true
        activity?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    private fun dialogHide() {
        binding.progress.isVisible = false
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    // 위치추적 시작
    fun startTracking() {
        mapView.currentLocationTrackingMode =
            MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading
    }

    // 위치추적 중지
    fun stopTracking() {
        mapView.currentLocationTrackingMode =
            MapView.CurrentLocationTrackingMode.TrackingModeOff
    }

    override fun onMapViewInitialized(p0: MapView?) {

    }

    // 지도에 직접 추가하기 부분 기능들 구현
    override fun onMapViewCenterPointMoved(p0: MapView?, p1: MapPoint?) {
        if (markerCheck) {
            marker.mapPoint = MapPoint.mapPointWithGeoCoord(
                p0!!.mapCenterPoint.mapPointGeoCoord.latitude,
                p0.mapCenterPoint.mapPointGeoCoord.longitude
            )
            latitude = p0.mapCenterPoint.mapPointGeoCoord.latitude
            longitude = p0.mapCenterPoint.mapPointGeoCoord.longitude
        }
    }

    override fun onMapViewZoomLevelChanged(p0: MapView?, p1: Int) {

    }

    // MapView를 클릭하면 호출되는 콜백 메서드
    override fun onMapViewSingleTapped(p0: MapView?, p1: MapPoint?) {
        binding.clCardview.visibility = View.GONE
        binding.ibPlus.visibility = View.VISIBLE
        binding.ibGps.visibility = View.VISIBLE
        binding.ibKm.visibility = View.VISIBLE

        // 검색한 값 지우기
        binding.etSearch.text = null
        // 검색창 없애기
        binding.clSearchResult.visibility = View.GONE

        val imm = requireContext().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etLocationSearch.windowToken, 0)

        if (plusBtn) {
            plusBtnInactivation()
            clientMarker()
        }
    }

    override fun onMapViewDoubleTapped(p0: MapView?, p1: MapPoint?) {

    }

    override fun onMapViewLongPressed(p0: MapView?, p1: MapPoint?) {

    }

    override fun onMapViewDragStarted(p0: MapView?, p1: MapPoint?) {

    }

    override fun onMapViewDragEnded(p0: MapView?, p1: MapPoint?) {
        if (markerCheck) {

            reverseGeoCoderFoundAddress(
                marker.mapPoint.mapPointGeoCoord.longitude.toString(),
                marker.mapPoint.mapPointGeoCoord.latitude.toString()
            )
        }
    }

    override fun onMapViewMoveFinished(p0: MapView?, p1: MapPoint?) {
        val mapLatitude = p1?.mapPointGeoCoord?.latitude ?: 0.0
        val mapLongitude = p1?.mapPointGeoCoord?.longitude ?: 0.0
        GajaMapApplication.prefs.setString("mapLatitude", mapLatitude.toString())
        GajaMapApplication.prefs.setString("mapLongitude", mapLongitude.toString())

    }

    // 마커 클릭 시 호출되는 콜백 메서드
    override fun onPOIItemSelected(p0: MapView?, p1: MapPOIItem?) {
        binding.clCardview.visibility = View.VISIBLE
        binding.ibPlus.visibility = View.GONE
        binding.ibGps.visibility = View.GONE
        binding.ibKm.visibility = View.GONE
        binding.clKm.visibility = View.GONE

        // 마커 클릭 시 해당 위도 경도값을 사용하여 일치하는 고객들 뷰페이저로 띄우기
        try {
            getClientList(p1!!.userObject)
        } catch (e: Exception) {
            binding.clCardview.visibility = View.GONE
        }

    }

    override fun onCalloutBalloonOfPOIItemTouched(p0: MapView?, p1: MapPOIItem?) {

    }

    override fun onCalloutBalloonOfPOIItemTouched(
        p0: MapView?,
        p1: MapPOIItem?,
        p2: MapPOIItem.CalloutBalloonButtonType?
    ) {

    }

    override fun onDraggablePOIItemMoved(p0: MapView?, p1: MapPOIItem?, p2: MapPoint?) {

    }
}