package com.pg.gajamap.ui.fragment.customerList

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.kakao.sdk.navi.Constants
import com.kakao.sdk.navi.NaviClient
import com.kakao.sdk.navi.model.CoordType
import com.kakao.sdk.navi.model.Location
import com.kakao.sdk.navi.model.NaviOption
import com.pg.gajamap.BR
import com.pg.gajamap.R
import com.pg.gajamap.base.BaseFragment
import com.pg.gajamap.base.UserData
import com.pg.gajamap.data.model.*
import com.pg.gajamap.data.response.CreateGroupRequest
import com.pg.gajamap.databinding.DialogAddGroupBottomSheetBinding
import com.pg.gajamap.databinding.DialogGroupBinding
import com.pg.gajamap.databinding.FragmentListBinding
import com.pg.gajamap.ui.adapter.CustomerListAdapter
import com.pg.gajamap.ui.adapter.GroupListAdapter
import com.pg.gajamap.ui.view.EditListActivity
import com.pg.gajamap.viewmodel.GetClientViewModel

class ListFragment : BaseFragment<FragmentListBinding>(R.layout.fragment_list) {
    private var groupId: Int = -1
    private var radius = 0
    private var clientList = UserData.clientListResponse
    private var groupInfo = UserData.groupinfo
    lateinit var groupListAdapter: GroupListAdapter
    private lateinit var customerListAdapter: CustomerListAdapter
    var groupNum = 0
    var pos: Int = 0
    var sheetView : DialogAddGroupBottomSheetBinding? = null
    var posDelete: Int = 0
    var gName: String = ""
    var gid: Long = 0
    var itemId: Long = 0
    private var state = 1

    private val ACCESS_FINE_LOCATION = 1000
    private val CALL_PHONE_PERMISSION_CODE = 101

    override val viewModel by viewModels<GetClientViewModel> {
        GetClientViewModel.AddViewModelFactory("tmp")
    }

    override fun initViewModel(viewModel: ViewModel) {
        binding.setVariable(BR.viewModel, viewModel)
        binding.lifecycleOwner = this@ListFragment
        binding.fragment = this@ListFragment
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateAction() {
        clientList = UserData.clientListResponse
        customerListAdapter = CustomerListAdapter(clientList!!.clients)
        binding.listRv.apply {
            adapter = customerListAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            customerListAdapter.updateData(clientList!!.clients.sortedBy { it.clientId }.reversed())
        }

        val groupDialog = BottomSheetDialog(requireContext(), R.style.BottomSheetTheme)
        sheetView = DialogAddGroupBottomSheetBinding.inflate(layoutInflater)

        // 자동 로그인 response 데이터 값 받아오기
        val groupInfo = UserData.groupinfo

        // 추가한 그룹이 존재하는지 확인한 뒤에 그룹을 추가하라는 다이얼로그를 띄울지 말지 결정해야 하기에 일단 여기에서 호출
        checkGroup()

        // 그룹 더보기 및 검색창 그룹 이름, 현재 선택된 이름으로 변경
        if (groupInfo != null) {
            binding.tvSearch.text = groupInfo.groupName
            sheetView!!.tvAddgroupMain.text = groupInfo.groupName
        }

        binding.fragmentListCategory1.setOnClickListener { view ->
            binding.fragmentListCategory3.setBackgroundResource(R.drawable.fragment_list_category_background)
            binding.fragmentListCategory2.setBackgroundResource(R.drawable.fragment_list_category_background)
            binding.fragmentListCategory1.setBackgroundResource(R.drawable.list_distance_purple)
            state = 1

            customerListAdapter.updateData(clientList!!.clients.sortedBy { it.clientId }.reversed())
        }
        binding.fragmentListCategory2.setOnClickListener { view ->
            binding.fragmentListCategory1.setBackgroundResource(R.drawable.fragment_list_category_background)
            binding.fragmentListCategory3.setBackgroundResource(R.drawable.fragment_list_category_background)
            binding.fragmentListCategory2.setBackgroundResource(R.drawable.list_distance_purple)
            state = 2

            customerListAdapter.updateData(clientList!!.clients.sortedBy { it.clientId })
        }

        binding.fragmentListCategory3.setOnClickListener { view ->
            binding.fragmentListCategory1.setBackgroundResource(R.drawable.fragment_list_category_background)
            binding.fragmentListCategory2.setBackgroundResource(R.drawable.fragment_list_category_background)
            binding.fragmentListCategory3.setBackgroundResource(R.drawable.list_distance_purple)
            state = 3

            if (checkLocationService()) {
                // GPS가 켜져있을 경우
                permissionCheck()
            } else {
                // GPS가 꺼져있을 경우
                Toast.makeText(requireContext(), "GPS를 켜주세요", Toast.LENGTH_SHORT).show()
            }

            customerListAdapter.updateData(clientList!!.clients.sortedBy { it.distance })
        }

        //내비게이션
        customerListAdapter.setItemClickListener(object :
            CustomerListAdapter.ItemClickListener {
            override fun onClick(v: View, position: Int) {
                val latitude = clientList!!.clients[position].location.latitude
                val longitude = clientList!!.clients[position].location.longitude
                val name = clientList!!.clients[position].clientName
                Log.d("navi", latitude.toString())
                Log.d("navi", longitude.toString())
                //카카오내비
                // 카카오내비 앱으로 길 안내
                if (NaviClient.instance.isKakaoNaviInstalled(requireContext())) {
                    // 카카오내비 앱으로 길 안내 - WGS84
                    startActivity(
                        NaviClient.instance.navigateIntent(
                            //위도 경도를 장소이름으로 바꿔주기
                            Location(name, longitude.toString(), latitude.toString()),
                            NaviOption(coordType = CoordType.WGS84)
                        )
                    )
                } else {
                    // 카카오내비 설치 페이지로 이동
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(Constants.WEB_NAVI_INSTALL)
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    )
                }
            }
        })

        // groupListAdapter를 우선적으로 초기화해줘야 함
        groupListAdapter = GroupListAdapter(object : GroupListAdapter.GroupDeleteListener{
            override fun click(id: Long, name: String, position: Int) {
                // 그룹 삭제 dialog
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("해당 그룹을 삭제하시겠습니까?")
                    .setMessage("그룹을 삭제하시면 영구 삭제되어 복구할 수 없습니다.")
                    .setPositiveButton("확인", { dialogInterface: DialogInterface, i: Int ->
                        // 그룹 삭제 서버 연동 함수 호출
                        deleteGroup(gid, position)
                    })
                    .setNegativeButton("취소", { dialogInterface: DialogInterface, i: Int ->
                        Toast.makeText(requireContext(), "취소", Toast.LENGTH_SHORT).show()
                    })
                val alertDialog = builder.create()
                alertDialog.show()
                gName = name
                posDelete = position
                gid = id
            }
        }, object : GroupListAdapter.GroupEditListener{
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
                    modifyGroup(gid, mDialogView.etName.text.toString(), position)
                    addDialog.dismiss()
                }
            }
        })

        // 그룹 recyclerview 아이템 클릭 시 값 변경 및 배경색 바꾸기
        groupListAdapter.setItemClickListener(object : GroupListAdapter.OnItemClickListener{
            override fun onClick(v: View, position: Int, gid: Long, gname: String) {
                itemId = gid
                binding.tvSearch.text = gname
                sheetView!!.tvAddgroupMain.text = gname
                pos = position

                if (position == 0){
                    getAllClient()
                }else{
                    getGroupClient(gid, gname)
                }
            }
        })

        // search bar 클릭 시 바텀 다이얼로그 띄우기
        binding.clSearch.setOnClickListener {
            // 그룹 더보기 바텀 다이얼로그 띄우기
            checkGroup()
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
                    createGroup(mDialogView.etName.text.toString())
                    addDialog.dismiss()
                }
            }
        }

        //리사이클러뷰
        binding.listRv.addItemDecoration(CustomerListVerticalItemDecoration())

        binding.fragmentEditBtn.setOnClickListener {

            Log.d("groupinfo.groupname",groupInfo?.groupName!!)
            // 고객 편집하기 activity로 이동
            if (binding.tvSearch.text == "전체") {
                Toast.makeText(requireContext(), "전체 그룹은 편집 기능을 사용하지 못합니다.", Toast.LENGTH_SHORT)
                    .show()
            } else {
                val intent = Intent(activity, EditListActivity::class.java)
                startActivity(intent)
            }
        }

        //최신순은 보라색으로 시작
        binding.fragmentListCategory1.setBackgroundResource(R.drawable.list_distance_purple)
        binding.fragmentListCategory3.setBackgroundResource(R.drawable.fragment_list_category_background)
        binding.fragmentListCategory2.setBackgroundResource(R.drawable.fragment_list_category_background)

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                //입력이 끝날 때 작동됩니다.
                val searchText = editable.toString().trim()
                filterClientList(searchText)
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                //입력 하기 전에 작동됩니다.

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                //타이핑 되는 텍스트에 변화가 있으면 작동됩니다.
            }
        })

    }

    // GPS가 켜져있는지 확인
    private fun checkLocationService(): Boolean {
        val locationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    // 위치 권한 확인
    private fun permissionCheck() {
        val preference = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val isFirstCheck = preference.getBoolean("isFirstPermissionCheck", true)
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 권한이 없는 상태
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    requireActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                // 권한 거절 (다시 한 번 물어봄)
                val builder = AlertDialog.Builder(requireContext())
                builder.setMessage("현재 위치를 확인하시려면 위치 권한을 허용해주세요.")
                builder.setPositiveButton("확인") { dialog, which ->
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        ACCESS_FINE_LOCATION
                    )
                }
                builder.setNegativeButton("취소") { dialog, which ->

                }
                builder.show()
            } else {
                if (isFirstCheck) {
                    // 최초 권한 요청
                    preference.edit().putBoolean("isFirstPermissionCheck", false).apply()
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        ACCESS_FINE_LOCATION
                    )
                } else {
                    // 다시 묻지 않음 클릭 (앱 정보 화면으로 이동)
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setMessage("현재 위치를 확인하시려면 설정에서 위치 권한을 허용해주세요.")
                    builder.setPositiveButton("확인") { dialog, which ->

                    }
                    builder.setNegativeButton("취소") { dialog, which ->

                    }
                    builder.show()
                }
            }
        } else {
            // 권한이 있는 상태
            // 위치추적 시작하는 코드 추가
        }
    }


    // 권한 요청 후 행동
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ACCESS_FINE_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한 요청 후 승인됨 (추적 시작)
                Toast.makeText(requireContext(), "위치 권한 승인", Toast.LENGTH_SHORT).show()
                //startTracking()
            } else {
                // 권한 요청 후 거절됨 (다시 요청 or 토스트)
                Toast.makeText(requireContext(), "위치 권한 거절", Toast.LENGTH_SHORT).show()
                permissionCheck()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateData()
    }

    private fun updateData() {
        clientList?.let { ListRv(it) }
    }

    fun ListRv(it: GetAllClientResponse) {
        customerListAdapter = CustomerListAdapter(it.clients)
        sortedRVList()
    }

    fun filterClientList(searchText: String) {
        val filteredList = clientList?.clients?.filter { client ->
            // 여기에서 clientName을 검색합니다. 대소문자를 무시하려면 equals를 equalsIgnoreCase로 바꿀 수 있습니다.
            client.clientName.contains(searchText, ignoreCase = true)
        }
        customerListAdapter = clientList?.let { CustomerListAdapter(it.clients) }!!
        binding.listRv.apply {
            adapter = customerListAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        }
        // 필터링된 결과를 리사이클러뷰 어댑터에 설정합니다.
        if (filteredList != null) {
            customerListAdapter.updateData(filteredList)
        }
    }

    // 그룹 생성 api
    private fun createGroup(name: String){
        viewModel.createGroup(CreateGroupRequest(name))
        viewModel.checkGroup.observe(this, Observer {
            groupListAdapter.setData(viewModel.checkGroup.value!!)
        })
    }

    // 그룹 조회 api
    private fun checkGroup(){
        viewModel.checkGroup()
        viewModel.checkGroup.observe(this, Observer {
            groupListAdapter.setData(it)
            groupNum = viewModel.checkGroup.value!!.size
        })
    }

    // 그룹 삭제 api
    private fun deleteGroup(groupId: Long, position: Int){
        viewModel.deleteGroup(groupId, position)
        viewModel.checkGroup.observe(this, Observer {
            groupListAdapter.setData(it)
            // 현재 선택한 리사이클러뷰 아이템의 그룹을 삭제했을 경우
            // 전체 고객을 조회하는 api 호출 후 전체 고객 마커 찍고 UserData 값 변경
            if(posDelete == position){
                getAllClient()
                binding.tvSearch.text = "전체"
                sheetView!!.tvAddgroupMain.text = "전체"
            }
        })
    }

    // 그룹 수정 api
    private fun modifyGroup(groupId: Long, name: String, position: Int){
        viewModel.modifyGroup(groupId, CreateGroupRequest(name), position)
        viewModel.checkGroup.observe(this, Observer {
            groupListAdapter.setData(it)

            // 변경한 그룹 이름 저장 데이터에도 갱신
            UserData.groupinfo!!.groupName = name

            // 현재 선택한 리사이클러뷰 아이템의 그룹 이름을 변경했을 경우
            if(pos == position){
                binding.tvSearch.text = name
                sheetView!!.tvAddgroupMain.text = name
            }
        })
    }

    private fun getGroupClient(groupId: Long, gname : String){
        viewModel.getGroupAllClient(groupId)
        viewModel.groupClients.observe(this, Observer {
            val data = viewModel.groupClients.value!!.clients
            val num = data.count()

            // UserData 값 갱신
            UserData.clientListResponse = viewModel.groupClients.value
            // groupinfo 값도 변경
            UserData.groupinfo = AutoLoginGroupInfo(groupId, num, gname)

            clientList = UserData.clientListResponse
            customerListAdapter = CustomerListAdapter(clientList!!.clients)

            sortedRVList()
        })
    }

    // 전체 고객 전부 조회 api
    private fun getAllClient(){
        viewModel.getAllClient()
        viewModel.allClients.observe(this, Observer {

            val data = viewModel.allClients.value!!.clients
            val num = data.count()

            // UserData 값 갱신
            UserData.clientListResponse = viewModel.allClients.value
            // groupinfo 값도 변경
            UserData.groupinfo = AutoLoginGroupInfo(-1, num, "전체")

            clientList = UserData.clientListResponse
            customerListAdapter = CustomerListAdapter(clientList!!.clients)

            sortedRVList()
        })
    }

    private fun sortedRVList() {
        binding.listRv.apply {
            adapter = customerListAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            val sortedData = when (state) {
                1 -> clientList!!.clients.sortedBy { it.clientId }.reversed()
                2 -> clientList!!.clients.sortedBy { it.clientId }
                3 -> clientList!!.clients.sortedBy { it.distance }
                else -> clientList!!.clients // 기본값으로 정렬하지 않음
            }
            customerListAdapter.updateData(sortedData)
        }
    }
}