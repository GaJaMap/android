package com.pg.gajamap.viewmodel

import android.graphics.Color
import android.util.Log
import androidx.lifecycle.*
import com.pg.gajamap.data.model.*
import com.pg.gajamap.data.repository.GetClientRepository
import com.pg.gajamap.data.repository.GroupRepository
import com.pg.gajamap.data.response.CreateGroupRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

class GetClientViewModel(private val tmp: String): ViewModel() {

    private val getClientRepository = GetClientRepository()
    private val groupRepository = GroupRepository()

    private val _getGroupClient = MutableLiveData<GetGroupClientResponse>()
    val getGroupClient : LiveData<GetGroupClientResponse>
    get() = _getGroupClient

    private val _getAllClient = MutableLiveData<GetAllClientResponse>()
    val getAllClient : LiveData<GetAllClientResponse>
    get() = _getAllClient

    private val _getAllClientName = MutableLiveData<GetAllClientResponse>()
    val getAllClientName : LiveData<GetAllClientResponse>
    get() = _getAllClientName

    private val _getGroupAllClient = MutableLiveData<GetGroupAllClientResponse>()
    val getGroupAllClient : LiveData<GetGroupAllClientResponse>
    get() = _getGroupAllClient

    private val _getGroupAllClientName = MutableLiveData<GetGroupAllClientResponse>()
    val getGroupAllClientName : LiveData<GetGroupAllClientResponse>
        get() = _getGroupAllClientName


    private val _deleteClient = MutableLiveData<BaseResponse>()
    val deleteClient : LiveData<BaseResponse>
        get() = _deleteClient

    private val _deleteAnyClient = MutableLiveData<BaseResponse>()
    val deleteAnyClient : LiveData<BaseResponse>
        get() = _deleteAnyClient

    private val _allNameRadius = MutableLiveData<GetRadiusResponse>()
    val allNameRadius : LiveData<GetRadiusResponse>
    get() = _allNameRadius

    private val _allRadius = MutableLiveData<GetRadiusResponse>()
    val allRadius : LiveData<GetRadiusResponse>
    get() = _allRadius

    private val _groupNameRadius = MutableLiveData<GetRadiusResponse>()
    val groupNameRadius : LiveData<GetRadiusResponse>
    get() = _groupNameRadius

    private val _groupRadius = MutableLiveData<GetRadiusResponse>()
    val groupRadius : LiveData<GetRadiusResponse>
    get() = _groupRadius

    // 값이 변경되는 경우 MutableLiveData로 선언한다.
    private val _checkGroup = MutableLiveData<ArrayList<GroupListData>>()
    val checkGroup : LiveData<ArrayList<GroupListData>>
        get() = _checkGroup
    private var checkItems = ArrayList<GroupListData>()

    // 그룹 생성
    fun createGroup(createRequest: CreateGroupRequest){
        viewModelScope.launch(Dispatchers.IO) {
            val response = groupRepository.createGroup(createRequest)
            Log.d("createGroup", "$response\n${response.code()}")
            Log.d("createResponse", response.body().toString())
            if(response.isSuccessful){
                val data = response.body()
                // MapFragment에서 observer가 실행되기 위해서는 postValue가 필요하다!
                checkItems.add(GroupListData(img = Color.rgb(Random.nextInt(0, 255), Random.nextInt(0, 255), Random.nextInt(0, 255)), id = data!!, name = createRequest.name, person = "0", false, false))
                _checkGroup.postValue(checkItems)
                Log.d("createGroupSuccess", "${response.body()}")
            }else {
                Log.d("createGroupError", "createGroup : ${response.message()}")
            }
        }
    }

    // 그룹 조회
    fun checkGroup(){
        viewModelScope.launch {
            val response = groupRepository.checkGroup()
            Log.d("checkGroup", "$response\n${response.code()}")
            if(response.isSuccessful){
                val data = response.body()
                checkItems.clear()
                Log.d("checkGroupSuccess", "${response.body()}")
                val num = data!!.groupInfos.count()
                var count = 0
                checkItems.add(GroupListData(img = Color.rgb(Random.nextInt(0, 255), Random.nextInt(0, 255), Random.nextInt(0, 255)), id = 0, name = "전체", person = "0", true, true))
                for (i in 0..num-1) {
                    val itemdata = data.groupInfos.get(i)
                    count += itemdata.clientCount
                    checkItems.add(GroupListData(img = Color.rgb(Random.nextInt(0, 255), Random.nextInt(0, 255), Random.nextInt(0, 255)), id = itemdata.groupId, name = itemdata.groupName, person = itemdata.clientCount.toString(), false, false))
                }
                checkItems[0].person = count.toString()
                _checkGroup.value = checkItems
            }else {
                Log.d("checkGroupError", "checkGroup : ${response.message()}")
            }
        }
    }

    // 그룹 삭제
    fun deleteGroup(groupId: Long, pos: Int){
        viewModelScope.launch(Dispatchers.IO) {
            val response = groupRepository.deleteGroup(groupId)
            Log.d("deleteGroup", "$response\n${response.code()}")
            if(response.isSuccessful){
                checkItems.removeAt(pos)
                _checkGroup.postValue(checkItems)
                Log.d("deleteGroupSuccess", "${response.body()}")
            }else {
                Log.d("deleteGroupError", "deleteGroup : ${response.message()}")
            }
        }
    }

    // 그룹 수정
    fun modifyGroup(groupId: Long, createRequest: CreateGroupRequest, pos: Int){
        viewModelScope.launch(Dispatchers.IO) {
            val response = groupRepository.modifyGroup(groupId, createRequest)
            Log.d("modifyGroup", "$response\n${response.code()}")
            if(response.isSuccessful){
                checkItems.get(pos).name = createRequest.name
                _checkGroup.postValue(checkItems)
                Log.d("modifyGroupSuccess", "${response.body()}")

            }else {
                Log.d("modifyGroupError", "modifyGroup : ${response.message()}")
            }
        }
    }
    fun deleteClient(groupId : Long, client : Long){
        viewModelScope.launch(Dispatchers.IO) {
            val response = getClientRepository.deleteClient(groupId, client)
            Log.d("deleteClient", "${response.body()}\n${response.code()}")
            if(response.isSuccessful){
                _deleteClient.postValue(response.body())
                Log.d("deleteClientSuccess", "${response.body()}")
            }else {
                Log.d("deleteClientError", "deleteClient : ${response.message()}")
            }
        }
    }

    fun deleteAnyClient(groupId : Long, deleteRequest: DeleteRequest){
        viewModelScope.launch(Dispatchers.IO) {
            val response = getClientRepository.deleteAnyClient(groupId, deleteRequest)
            Log.d("deleteAnyClient", "${response.body()}\n${response.code()}")
            if(response.isSuccessful){
                _deleteAnyClient.postValue(response.body())
                Log.d("deleteAnyClientSuccess", "${response.body()}")
            }else {
                Log.d("deleteAnyClientError", "deleteAnyClient : ${response.message()}")
            }
        }
    }

    // 특정 그룹 내 고객 전부 조회
    private val _groupClients = MutableLiveData<GetAllClientResponse>()
    val groupClients : LiveData<GetAllClientResponse>
        get() = _groupClients

    fun getGroupAllClient(groupId : Long){
        viewModelScope.launch(Dispatchers.IO) {
            val response = groupRepository.getGroupAllClient(groupId)
            Log.d("getGroupAllClient", "${response.body()}\n${response.code()}")
            if(response.isSuccessful){
                _groupClients.postValue(response.body())
                Log.d("getGroupAllClientSuccess", "${response.body()}")
            }else {
                Log.d("getGroupAllClientError", "getGroupAllClient : ${response.message()}")
            }
        }
    }


    // 전체 고객 전부 조회
    private val _allClients = MutableLiveData<GetAllClientResponse>()
    val allClients : LiveData<GetAllClientResponse>
        get() = _allClients

    fun getAllClient(){
        viewModelScope.launch(Dispatchers.IO) {
            val response = getClientRepository.getAllClient()
            Log.d("getAllClient", "${response.body()}\n${response.code()}")
            if(response.isSuccessful){
                _allClients.postValue(response.body())
                Log.d("getAllClientSuccess", "${response.body()}")
            }else {
                Log.d("getAllClientError", "getAllClient : ${response.message()}")
            }
        }
    }

    //전체 반경 - 이름
    fun allNameRadius(wordCond : String, radius: Double, latitude: Double, longitude: Double){
        viewModelScope.launch(Dispatchers.IO){
            val response = getClientRepository.allNameRadius(wordCond,radius,latitude,longitude)
            Log.d("allNameRadius", "${response.body()}\n${response.code()}")
            if(response.isSuccessful){
                _allNameRadius.postValue(response.body())
                Log.d("allNameRadiusSuccess", "${response.body()}")
            }else {
                Log.d("allNameRadiusError", "allNameRadius : ${response.message()}")
            }
        }
    }

    //전체 반경
    fun allRadius(radius: Double, latitude: Double, longitude: Double){
        viewModelScope.launch(Dispatchers.IO){
            val response = getClientRepository.allRadius(radius,latitude,longitude)
            Log.d("allRadius", "${response.body()}\n${response.code()}")
            if(response.isSuccessful){
                _allRadius.postValue(response.body())
                Log.d("allRadiusSuccess", "${response.body()}")
            }else {
                Log.d("allRadiusError", "allRadius : ${response.message()}")
            }
        }
    }

    //특정 그룹 반경 - 이름
    fun groupNameRadius(groupId : Long, wordCond : String, radius: Double, latitude: Double, longitude: Double){
        viewModelScope.launch(Dispatchers.IO){
            val response = getClientRepository.groupNameRadius(groupId,wordCond,radius,latitude,longitude)
            Log.d("groupNameRadius", "${response.body()}\n${response.code()}")
            if(response.isSuccessful){
                _groupNameRadius.postValue(response.body())
                Log.d("groupNameRadiusSuccess", "${response.body()}")
            }else {
                Log.d("groupNameRadiusError", "groupNameRadius : ${response.message()}")
            }
        }
    }

    //특정 그룹 반경
    fun groupRadius(groupId : Long, radius: Double, latitude: Double, longitude: Double){
        viewModelScope.launch(Dispatchers.IO){
            val response = getClientRepository.groupRadius(groupId,radius,latitude,longitude)
            Log.d("groupRadius", "${response.body()}\n${response.code()}")
            if(response.isSuccessful){
                _groupRadius.postValue(response.body())
                Log.d("groupRadiusSuccess", "${response.body()}")
            }else {
                Log.d("groupRadiusError", "groupRadius : ${response.message()}")
            }
        }
    }

    class AddViewModelFactory(private val tmp: String)
        : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            // modelClass에 MainViewModel이 상속되었는지 확인
            if (modelClass.isAssignableFrom(GetClientViewModel::class.java)) {
                // 맞다면 MainViewModel의 파라미터 값을 넘겨줌
                return GetClientViewModel(tmp) as T
            }
            // 상속이 되지 않았다면 IllegalArgumentException을 통해 상속이 되지 않았다는 에러를 띄움
            throw IllegalArgumentException("Not found ViewModel class.")
        }
    }
}