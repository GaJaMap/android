package com.pg.gajamap.ui.fragment.customerList

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
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
import com.pg.gajamap.ui.fragment.map.MapFragment
import com.pg.gajamap.ui.view.EditListActivity
import com.pg.gajamap.viewmodel.GetClientViewModel

class ListFragment : BaseFragment<FragmentListBinding>(R.layout.fragment_list) {
    private var groupId: Int = -1
    private var radius = 0
    private var clientList = UserData.clientListResponse
    private var groupInfo = UserData.groupinfo
    private lateinit var mapFragment: MapFragment
    lateinit var groupListAdapter: GroupListAdapter
    private lateinit var customerListAdapter: CustomerListAdapter
    private var doubleBackToExitPressedOnce = false
    var groupNum = 0
    var pos: Int = 0
    var sheetView : DialogAddGroupBottomSheetBinding? = null
    var posDelete: Int = 0
    var gName: String = ""
    var gid: Long = 0
    var itemId: Long = 0
    private var state = 1

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
        customerListAdapter = CustomerListAdapter(clientList!!.clients,requireContext())
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

            customerListAdapter.updateData(clientList!!.clients.sortedBy { it.distance })
        }

        // groupListAdapter를 우선적으로 초기화해줘야 함
        groupListAdapter = GroupListAdapter(object : GroupListAdapter.GroupDeleteListener{
            override fun click(id: Long, name: String, position: Int) {
                // 그룹 삭제 dialog
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("해당 그룹을 삭제하시겠습니까?")
                    .setMessage("그룹을 삭제하시면 영구 삭제되어 복구할 수 없습니다.")
                    .setPositiveButton("확인") { _: DialogInterface, _: Int ->
                        // 그룹 삭제 서버 연동 함수 호출
                        deleteGroup(gid, position)
                        Toast.makeText(requireContext(),"그룹 삭제 완료", Toast.LENGTH_SHORT).show()
                        groupDialog.hide()
                    }
                    .setNegativeButton("취소") { _: DialogInterface, _: Int ->
                    }
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
                    if(mDialogView.etName.text.toString() == "전체" ||
                        mDialogView.etName.text.toString().isEmpty()) {
                        Toast.makeText(requireContext(), "사용할 수 없는 그룹 이름입니다", Toast.LENGTH_SHORT).show()
                    } else {
                        createGroup(mDialogView.etName.text.toString())
                        addDialog.dismiss()
                    }
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

        view?.isFocusableInTouchMode = true
        view?.requestFocus()
        view?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                handleOnBackPressed()
                return@setOnKeyListener true
            }
            false
        }

    }

    override fun onResume() {
        super.onResume()
        updateData()
        if(binding.tvSearch.text == "전체") {
            getAllClient()
        } else {
            getGroupClient(UserData.groupinfo!!.groupId, UserData.groupinfo!!.groupName)
        }
    }

    private fun updateData() {
        clientList?.let { ListRv(it) }
    }

    private fun ListRv(it: GetAllClientResponse) {
        customerListAdapter = CustomerListAdapter(it.clients,requireContext())
        sortedRVList()
    }

    fun filterClientList(searchText: String) {
        val filteredList = clientList?.clients?.filter { client ->
            client.clientName.contains(searchText, ignoreCase = true)
        }
        customerListAdapter = clientList?.let { CustomerListAdapter(it.clients,requireContext()) }!!
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
        viewModel.createGroup.observe(this, Observer {
            groupListAdapter.setData(viewModel.checkGroup.value!!)
        })
        viewModel.checkErrorGroup.observe(this, Observer {
            Toast.makeText(requireContext(), it , Toast.LENGTH_SHORT).show()
        })
    }

    // 그룹 조회 api
    private fun checkGroup(){
        viewModel.checkGroup()
        viewModel.checkGroup.observe(this, Observer {
            groupListAdapter.setData(it)
            groupNum = viewModel.checkGroup.value!!.size
        })
        viewModel.checkErrorGroup.observe(this, Observer {
            Toast.makeText(requireContext(), it , Toast.LENGTH_SHORT).show()
        })
    }

    // 그룹 삭제 api
    private fun deleteGroup(groupId: Long, position: Int){
        viewModel.deleteGroup(groupId, position)
        viewModel.deleteGroup.observe(this, Observer {
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
        viewModel.modifyGroup.observe(this, Observer {
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
            customerListAdapter = CustomerListAdapter(clientList!!.clients,requireContext())

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
            customerListAdapter = CustomerListAdapter(clientList!!.clients,requireContext())

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

            if(customerListAdapter.itemCount == 0) {
                binding.userAddTv.visibility = View.VISIBLE
            } else {
                binding.userAddTv.visibility = View.GONE
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if(hidden) {
            return
        }
        checkGroup()
        clientList = UserData.clientListResponse
        customerListAdapter = CustomerListAdapter(clientList!!.clients,requireContext())
        updateData()

        // 그룹 더보기 및 검색창 그룹 이름, 현재 선택된 이름으로 변경
        if (UserData.groupinfo != null) {
            binding.tvSearch.text = UserData.groupinfo!!.groupName
            sheetView!!.tvAddgroupMain.text = UserData.groupinfo!!.groupName
        }
    }

    private fun handleOnBackPressed() {
        if (doubleBackToExitPressedOnce) {
            requireActivity().finish()
        } else {
            doubleBackToExitPressedOnce = true
            Toast.makeText(requireContext(), "한번 더 누르면 종료 됩니다.", Toast.LENGTH_SHORT).show()

            Handler(Looper.getMainLooper()).postDelayed({
                doubleBackToExitPressedOnce = false
            }, 2000)
        }
    }
}