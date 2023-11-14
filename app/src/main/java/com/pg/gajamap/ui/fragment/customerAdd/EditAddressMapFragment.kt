package com.pg.gajamap.ui.fragment.customerAdd

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.contains
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import com.pg.gajamap.BuildConfig
import com.pg.gajamap.R
import com.pg.gajamap.api.retrofit.KakaoSearchClient
import com.pg.gajamap.base.BaseFragment
import com.pg.gajamap.data.response.LocationSearchData
import com.pg.gajamap.data.response.ResultSearchCoord2addressData
import com.pg.gajamap.data.response.ResultSearchKeywordData
import com.pg.gajamap.databinding.FragmentEditAddressMapBinding
import com.pg.gajamap.ui.adapter.LocationSearchAdapter
import com.pg.gajamap.ui.view.CustomerInfoActivity
import com.pg.gajamap.viewmodel.GetClientViewModel
import net.daum.mf.map.api.MapPOIItem
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EditAddressMapFragment: BaseFragment<FragmentEditAddressMapBinding>(R.layout.fragment_edit_address_map), MapView.POIItemEventListener, MapView.MapViewEventListener {

    var customerInfoActivity: CustomerInfoActivity? = null

    private lateinit var mapView: MapView
    private lateinit var marker: MapPOIItem

    private val locationSearchList = arrayListOf<LocationSearchData>()
    private lateinit var locationSearchAdapter: LocationSearchAdapter

    private var bottomGPSBtn = false
    private var keyword = "" // 검색 키워드
    var latitude = 0.0
    var longitude = 0.0
    var address = ""
    var markerCheck = false

    override val viewModel by viewModels<GetClientViewModel> {
        GetClientViewModel.AddViewModelFactory("tmp")
    }

    override fun initViewModel(viewModel: ViewModel) {
        binding.lifecycleOwner = this@EditAddressMapFragment
        binding.viewModel = this.viewModel
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreateAction() {
        mapView = MapView(context)
        binding.kakaoMapContainer.addView(mapView)
//        val mapPoint = MapPoint.mapPointWithGeoCoord(
//            GajaMapApplication.prefs.getString("mapLatitude", "37.7").toDouble(),
//            GajaMapApplication.prefs.getString("mapLongitude", "127").toDouble()
//        )
//        mapView.setMapCenterPoint(mapPoint, true)

        mapView.setMapViewEventListener(this)
        mapView.setPOIItemEventListener(this)

        locationSearchAdapter = LocationSearchAdapter(requireContext(), locationSearchList)
        // 지도 타일 이미지 Persistent Cache 기능 : 네트워크를 통해 다운로드한 지도 이미지 데이터를 단말의 영구(persistent) 캐쉬 영역에 저장하는 기능
        MapView.setMapTilePersistentCacheEnabled(true)

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
                    binding.tvLocationAddress.text = "내 위치 검색중..."
                    // GPS가 켜져있을 경우
                    val a = getLocation()

                    val bgShape = binding.ibBottomGps.background as GradientDrawable
                    bgShape.setColor(resources.getColor(R.color.main))
                    binding.ibBottomGps.setImageResource(R.drawable.ic_white_gps)

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

                stopTracking()
                mapView.setShowCurrentLocationMarker(false)
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
                val bundle = Bundle()
                bundle.putString("address", text)
                bundle.putDouble("latitude", locationSearchList[position].y)
                bundle.putDouble("longitude", locationSearchList[position].x)

                val editProfileFragment = EditProfileFragment()
                editProfileFragment.arguments = bundle

                parentFragmentManager.beginTransaction()
                    .replace(R.id.frame_fragment, editProfileFragment)
                    .addToBackStack(null)
                    .commit()
            }
        })


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
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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

            val bundle = Bundle()
            bundle.putString("address", address)
            bundle.putDouble("latitude", latitude)
            bundle.putDouble("longitude", longitude)

            val editProfileFragment = EditProfileFragment()
            editProfileFragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_fragment, editProfileFragment)
                .addToBackStack(null)
                .commit()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.frame_fragment, EditProfileFragment())
                    .addToBackStack(null)
                    .commit()
            }
        })
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

    // GPS가 켜져있는지 확인
    private fun checkLocationService(): Boolean {
        val locationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        customerInfoActivity = context as CustomerInfoActivity
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
    }

    override fun onPause() {
        super.onPause()
        binding.kakaoMapContainer.removeView(mapView)
    }

    private fun startTracking() {
        mapView.currentLocationTrackingMode =
            MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading
    }

    // 위치추적 중지
    private fun stopTracking() {
        mapView.currentLocationTrackingMode =
            MapView.CurrentLocationTrackingMode.TrackingModeOff
    }

    override fun onPOIItemSelected(p0: MapView?, p1: MapPOIItem?) {
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

    override fun onMapViewInitialized(p0: MapView?) {
    }

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

    override fun onMapViewSingleTapped(p0: MapView?, p1: MapPoint?) {
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
    }
}