package com.pg.gajamap.ui.view

import android.app.AlertDialog
import android.content.DialogInterface
import com.pg.gajamap.data.model.Client
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.pg.gajamap.R
import com.pg.gajamap.base.BaseActivity
import com.pg.gajamap.base.UserData
import com.pg.gajamap.data.model.DeleteRequest
import com.pg.gajamap.data.model.GetAllClientResponse
import com.pg.gajamap.databinding.ActivityEditListBinding
import com.pg.gajamap.ui.adapter.CustomerAnyListAdapter
import com.pg.gajamap.ui.fragment.customerList.CustomerListVerticalItemDecoration
import com.pg.gajamap.viewmodel.GetClientViewModel

class EditListActivity : BaseActivity<ActivityEditListBinding>(R.layout.activity_edit_list) {
    var selectedClientIds = mutableListOf<Long>()
    var groupId: Long = 0
    var client = UserData.clientListResponse
    var clientList = UserData.clientListResponse?.clients
    var groupInfo = UserData.groupinfo

    override val viewModel by viewModels<GetClientViewModel> {
        GetClientViewModel.AddViewModelFactory("tmp")
    }

    override fun initViewModel(viewModel: ViewModel) {
        binding.lifecycleOwner = this@EditListActivity
        binding.viewModel = this.viewModel
    }

    override fun onCreateAction() {
        // 자동 로그인 response 데이터 값 받아오기
        //val clientList = UserData.clientListResponse
        //val groupInfo = UserData.groupinfo

        if (groupInfo != null) {
            groupId = groupInfo!!.groupId
        }

        //리사이클러뷰
        binding.listRv.addItemDecoration(CustomerListVerticalItemDecoration())
        client?.let { ListRv(it) }

        binding.topBackBtn.setOnClickListener {
            // 리스트 fragment로 이동
            finish()
        }

        binding.topDeleteBtn.setOnClickListener {

            val builder = AlertDialog.Builder(this)
            builder.setTitle("해당 고객을 삭제하시겠습니까?")
                .setMessage("고객을 삭제하시면 영구 삭제되어 복구할 수 없습니다.")
                .setPositiveButton("확인",
                    DialogInterface.OnClickListener { dialog, id ->
                        val deleteRequest = DeleteRequest(selectedClientIds)
                        viewModel.deleteAnyClient(groupId, deleteRequest)
                        viewModel.deleteAnyClient.observe(this, Observer {

                            for (id in selectedClientIds) {
                                removeClientWithClientId(id)
                            }


                            // 선택된 클라이언트들 삭제 후, 클라이언트 목록 업데이트
                            val newClientList = clientList?.filter { client ->
                                client.clientId !in selectedClientIds
                            }
                            Log.d("newdelete", clientList.toString())

                            if (newClientList != null) {
                                val newResponse = client?.let { it1 ->
                                    GetAllClientResponse(
                                        newClientList as MutableList<Client>,
                                        it1.imageUrlPrefix
                                    )
                                }
                                if (newResponse != null) {
                                    ListRv(newResponse)
                                }
                            }

                            //clientList = newClientList
                            //Log.d("newdelete", clientList.toString())

                            // 선택된 클라이언트 아이디 목록 초기화
                            selectedClientIds.clear()

                            binding.topTvNumber1.text = selectedClientIds.size.toString()

                            Toast.makeText(this, "삭제 완료", Toast.LENGTH_SHORT).show()
                            finish()
                        })
                    })
                .setNegativeButton("취소",
                    DialogInterface.OnClickListener { dialog, id ->
                    })
            // 다이얼로그를 띄워주기
            builder.show()

        }
    }

    private fun ListRv(it: GetAllClientResponse) {

        //고객 리스트
        binding.topTvNumber2.text = it.clients.size.toString()
        val customerAnyListAdapter = CustomerAnyListAdapter(it.clients, this)
        binding.listRv.apply {
            adapter = customerAnyListAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        }

        //전체 선택
        binding.checkEvery.setOnCheckedChangeListener { _, isChecked ->
            // 선택한 모든 clientId들을 selectedClientIds 리스트에 추가 또는 삭제
            selectedClientIds.clear() // 기존 선택한 아이템들 초기화

            if (isChecked) {
                selectedClientIds.addAll(it.clients.map { client -> client.clientId })
                // 배경색 변경
                val backgroundDrawable: Drawable? by lazy {
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.fragment_list_tool_purple
                    )
                }
                customerAnyListAdapter.addItemBackground(backgroundDrawable)
                binding.topTvNumber1.text = selectedClientIds.size.toString()
            } else {
                // 배경색 변경
                selectedClientIds.removeAll { true }
                val backgroundDrawable: Drawable? by lazy {
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.fragment_list_tool
                    )
                }
                customerAnyListAdapter.deleteItemBackground(backgroundDrawable)
                binding.topTvNumber1.text = selectedClientIds.size.toString()
            }
        }

        //리사이클러뷰 클릭
        customerAnyListAdapter.setOnItemClickListener(object :
            CustomerAnyListAdapter.OnItemClickListener {
            override fun onClick(v: View, position: Int) {
                val selectedClientId = it.clients[position].clientId

                if (selectedClientIds.contains(selectedClientId)) {
                    selectedClientIds.remove(selectedClientId)
                } else {
                    selectedClientIds.add(selectedClientId)
                }

                binding.topTvNumber1.text = selectedClientIds.size.toString()
            }
        })
    }

    private fun removeClientWithClientId(clientIdToRemove: Long) {
        // 자동 로그인 response 데이터 값 받아오기
        val clientList = UserData.clientListResponse?.clients as? MutableList<Client>

        if (clientList != null) {
            val iterator = clientList.iterator()
            while (iterator.hasNext()) {
                val client = iterator.next()
                if (client.clientId == clientIdToRemove) {
                    iterator.remove()
                    Log.d("delete", clientList.toString())
                    break  // 원하는 클라이언트를 찾고 삭제 후 반복문 종료
                }
            }
        }
    }

}