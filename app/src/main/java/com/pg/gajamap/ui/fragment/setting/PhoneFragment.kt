package com.pg.gajamap.ui.fragment.setting

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.provider.Settings
import android.telephony.PhoneNumberUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.pg.gajamap.BR
import com.pg.gajamap.R
import com.pg.gajamap.base.BaseFragment
import com.pg.gajamap.base.UserData
import com.pg.gajamap.data.model.Address
import com.pg.gajamap.data.model.Client
import com.pg.gajamap.data.model.Clients
import com.pg.gajamap.data.model.GroupInfo
import com.pg.gajamap.data.model.GroupInfoResponse
import com.pg.gajamap.data.model.Image
import com.pg.gajamap.data.model.Location
import com.pg.gajamap.data.model.PostKakaoPhoneRequest
import com.pg.gajamap.databinding.FragmentPhoneBinding
import com.pg.gajamap.ui.adapter.PhoneListAdapter
import com.pg.gajamap.viewmodel.ClientViewModel

class PhoneFragment : BaseFragment<FragmentPhoneBinding>(R.layout.fragment_phone),
    PhoneListAdapter.OnItemClickListener2 {

    // 선택된 클라이언트들을 저장하기 위한 리스트
    private var selectedClients: MutableList<Clients?> = mutableListOf()
    private var groupId: Long = -1
    private var groupName: String = ""
    var client = UserData.clientListResponse
    var clientList = UserData.clientListResponse?.clients
    var groupInfo = UserData.groupinfo
    val regex = """^\d{2,3}-\d{3,4}-\d{4}$""".toRegex()

    private var isBtnActivated = false // 버튼 활성화 되었는지 여부, true면 활성화, false면 비활성화

    companion object {
        const val PERMISSION_REQUEST_CODE = 100
    }

    private var contactsList = ArrayList<ContactsData>()
    private var phoneListAdapter: PhoneListAdapter? = null
    private val ACCESS_FINE_LOCATION = 1000


    override val viewModel by viewModels<ClientViewModel> {
        ClientViewModel.SettingViewModelFactory("tmp")
    }

    override fun initViewModel(viewModel: ViewModel) {
        binding.setVariable(BR.viewModel, viewModel)
        binding.lifecycleOwner = this@PhoneFragment
        binding.fragment = this@PhoneFragment
    }

    override fun onCreateAction() {

//        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
//            override fun handleOnBackPressed() {
//                parentFragmentManager.beginTransaction()
//                    .replace(R.id.menu_setting, SettingFragment())
//                    .addToBackStack(null)
//                    .commit()
//            }
//        })

        hideBottomNavigation(true)

        //var groupInfo = UserData.groupinfo

        //스피너
        viewModel.checkGroup()
        viewModel.checkGroup.observe(this, Observer {
            // GroupResponse에서 GroupInfoResponse의 groupName 속성을 추출하여 리스트로 변환합니다.
            val groupNames = mutableListOf<String>()
            // groupResponse의 groupInfos에서 각 GroupInfoResponse의 groupName을 추출하여 리스트에 추가합니다.
            it.groupInfos.forEach { groupInfo ->
                groupNames.add(groupInfo.groupName)
            }
            //groupNames.add(groupNames.size, "그룹 선택")
            groupNames.add(0, "그룹 선택")

            val adapter = object :
                ArrayAdapter<String>(requireActivity(), R.layout.spinner_list, groupNames) {

                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val textView = super.getView(position, convertView, parent) as TextView
                    textView.setTextColor(
                        ContextCompat.getColor(
                            context,
                            android.R.color.black
                        )
                    ) // 검정색으로 변경
                    return textView
                }

                override fun getDropDownView(
                    position: Int,
                    convertView: View?,
                    parent: ViewGroup
                ): View {
                    val textView = super.getDropDownView(position, convertView, parent) as TextView
                    textView.setTextColor(
                        ContextCompat.getColor(
                            context,
                            android.R.color.black
                        )
                    ) // 검정색으로 변경
                    return textView
                }
            }

            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.settingPhoneSpinner.adapter = adapter

        })

        binding.settingPhoneSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {

                    if (pos != 0) {
                        val selectedGroupInfoResponse: GroupInfoResponse = viewModel.checkGroup.value?.groupInfos?.get(pos - 1) ?: return
                        groupId = selectedGroupInfoResponse.groupId
                        Log.d("groupId", groupId.toString())
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {

                }
            }

        binding.topBackBtn.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().remove(this).commit()
            requireActivity().supportFragmentManager.popBackStack()
        }

        //연락처 권한
        onCheckContactsPermission()
        requestPermission()
    }

    override fun onResume() {
        super.onResume()
        onCheckContactsPermission()
        //requestPermission()
    }

    private fun chkBtnActivate() {
        // 버튼이 활성화되어 있지 않은 상황에서 확인
        if (selectedClients.size >=1 && groupId != -1L) {
            isBtnActivated = true
            binding.btnSubmit.apply {
                isEnabled = true
                setBackgroundResource(R.drawable.fragment_add_bottom_purple)
            }
        } else {
            isBtnActivated = false
            binding.btnSubmit.apply {
                isEnabled = false
                setBackgroundResource(R.drawable.bg_notworkbtn)
            }
        }
    }

    //연락처 가져오기
    private fun getContactsList() {

        val contacts = context?.contentResolver
            ?.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null)
        val list = ArrayList<ContactsData>()
        contacts?.let {
            while (it.moveToNext()) {
                val contactsId =
                    contacts.getInt(contacts.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))
                val name =
                    contacts.getString(contacts.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val number =
                    PhoneNumberUtils.formatNumber(contacts.getString(contacts.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)))

                if (name.length <= 10) {
                    // 중복 확인
                    val isDuplicate = list.any { existingContact ->
                        existingContact.name == name && existingContact.number == number
                    }
                    if (!isDuplicate) {
                        list.add(ContactsData(contactsId, name, number))
                    }
                }
            }
        }
        list.sortBy { it.name }
        contacts?.close()
        if (contactsList != list) {
            contactsList = list
            setContacts()
        }
    }

    // 연락처 리사이클러뷰
    private fun setContacts() {
        binding.topTvNumber2.text = contactsList.size.toString()

        phoneListAdapter = PhoneListAdapter(contactsList, this)
        binding.phoneListRv.apply {
            adapter = phoneListAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            addItemDecoration(PhoneListVerticalItemDecoration())
        }

        //전체선택
        binding.settingPhoneCheckEvery.setOnCheckedChangeListener { _, isChecked ->
            selectedClients.clear()
            if (isChecked) {
                selectedClients.addAll(contactsList.map {
                    Clients(it.name, it.number)
                } ?: emptyList())
            } else {
                selectedClients.clear()
            }
            phoneListAdapter?.setAllItemsChecked(isChecked)
            binding.topTvNumber1.text = selectedClients.size.toString()
            chkBtnActivate()
        }


        phoneListAdapter?.setOnItemClickListener(object :
            PhoneListAdapter.OnItemClickListener {
            override fun onClick(v: View, position: Int) {
                // 아이템 클릭시 해당 아이템의 선택 여부를 토글하고 선택된 클라이언트 리스트 업데이트
                val item = contactsList[position]
                Log.d("selectItem", item.toString())
                item.let {
                    if (phoneListAdapter!!.isChecked(position)) {
                        it.name.let { nickname ->
                            selectedClients.add(Clients(nickname, it.number))
                            chkBtnActivate()
                        }
                    } else {
                        it.name.let { nickname ->
                            selectedClients.remove(Clients(nickname, it.number))
                            chkBtnActivate()
                        }
                    }
                }
                binding.topTvNumber1.text = selectedClients.size.toString()

            }
        })
        binding.settingPhoneSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {

                    if (pos != 0) {
                        val selectedGroupInfoResponse: GroupInfoResponse =
                            viewModel.checkGroup.value?.groupInfos?.get(pos - 1) ?: return
                        groupId = selectedGroupInfoResponse.groupId
                        groupName = selectedGroupInfoResponse.groupName
                        chkBtnActivate()

                        binding.btnSubmit.setOnClickListener {
                            dialogShow()
                            viewModel.postKakaoPhoneClient(
                                PostKakaoPhoneRequest(
                                    selectedClients,
                                    groupId.toInt()
                                )
                            )
                            viewModel.postKakaoPhoneClient.observe(
                                viewLifecycleOwner,
                                Observer { response ->

                                    if (groupId == groupInfo?.groupId || groupInfo?.groupId == -1L) {
                                        val ids = response.body() // Response에서 Int 리스트를 가져옵니다.

                                        if (ids != null) {
                                            val newClients = mutableListOf<Client>()

                                            for (i in ids.indices) {
                                                val clientId = ids[i] // List에서 현재 순서의 Int 값을 가져옵니다.
                                                val selectedClient =
                                                    selectedClients.getOrNull(i) // selectedClients에서 현재 순서의 선택된 클라이언트를 가져옵니다.

                                                if (selectedClient != null) {
                                                    val newClient = Client(
                                                        address = Address("", ""), // 적절한 값으로 대체하세요
                                                        clientId = clientId.toLong(), // List에서 가져온 clientId 값을 할당합니다.
                                                        clientName = selectedClient.clientName, // 선택된 클라이언트의 이름을 사용합니다
                                                        distance = null, // 적절한 값으로 대체하세요
                                                        groupInfo = GroupInfo(
                                                            groupId,
                                                            groupName
                                                        ), // 적절한 값으로 대체하세요
                                                        image = Image(null, null), // 적절한 값으로 대체하세요
                                                        location = Location(
                                                            null,
                                                            null
                                                        ), // 적절한 값으로 대체하세요
                                                        phoneNumber = selectedClient.phoneNumber, // 선택된 클라이언트의 전화번호를 사용합니다
                                                        createdAt = "" // 적절한 값으로 대체하세요
                                                    )
                                                    Log.d("selectNew", newClient.toString())
                                                    newClients.add(newClient)
                                                }
                                            }

                                            // 새로운 Clients를 기존 clientList에 추가합니다.
                                            clientList?.addAll(newClients)
                                            Log.d("selectList", clientList.toString())
                                        }
                                    }

                                    dialogHide()
                                    requireActivity().supportFragmentManager.beginTransaction()
                                        .remove(this@PhoneFragment).commit()
                                    requireActivity().supportFragmentManager.popBackStack()
                                })

                        }
                    } else {
                        groupId = -1L
                        chkBtnActivate()
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    TODO("Not yet implemented")
                }
            }


    }

    private fun onCheckContactsPermission() {
        val permissionDenied = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_DENIED
                || ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_DENIED
        if (permissionDenied) {
            Toast.makeText(requireContext(), "권한이 거절되었습니다.", Toast.LENGTH_SHORT).show()
        } else {
            getContactsList()
        }
    }

    private fun requestPermission() {
        requestPermissions(
            arrayOf(
                Manifest.permission.WRITE_CONTACTS,
                Manifest.permission.READ_CONTACTS
            ), PERMISSION_REQUEST_CODE
        )
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ACCESS_FINE_LOCATION) {
            if (checkSelfPermission(
                    requireContext(),
                    permissions[0]
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                onCheckContactsPermission()
            } else {
                if (shouldShowRequestPermissionRationale(permissions[0])) {
                    Toast.makeText(requireContext(), "권한이 거절되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    AlertDialog.Builder(requireContext()).apply {
                        setTitle("권한")
                        setMessage("권한을 허용하기 위해서 설정으로 이동합니다.")
                        setPositiveButton("확인") { _, _ ->
                            val intent =
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    //data = Uri.fromParts("package", "com.example.gajamap", null)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                            startActivity(intent)
                        }
                        setNegativeButton("거절") { dialog, _ ->
                            dialog.dismiss()
                        }
                        show()
                    }
                }
            }
        }
    }

    // 프래그먼트 바텀 네비게이션 뷰 숨기기
    private fun hideBottomNavigation(bool: Boolean) {
        val bottomNavigation = requireActivity().findViewById<BottomNavigationView>(R.id.nav_bn)
        if (bool) bottomNavigation.visibility = View.GONE else bottomNavigation.visibility =
            View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideBottomNavigation(false)
    }

    override fun onItemClick(position: Int, isChecked: Boolean) {
        val item = contactsList[position]
        if (isChecked) {


            selectedClients.add(Clients(item.name, item.number))

            binding.topTvNumber1.text = selectedClients.size.toString()
            chkBtnActivate()
        } else {
            selectedClients.remove(Clients(item.name, item.number))

            binding.topTvNumber1.text = selectedClients.size.toString()
            chkBtnActivate()

        }
    }

    private fun dialogShow() {
        binding.progress.isVisible = true
        binding.btnSubmit.text = ""
        activity?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun dialogHide() {
        binding.progress.isVisible = false
        binding.btnSubmit.text = "확인"
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

}