package com.pg.gajamap.ui.fragment.customerAdd

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.pg.gajamap.BR
import com.pg.gajamap.R
import com.pg.gajamap.base.BaseFragment
import com.pg.gajamap.base.GajaMapApplication
import com.pg.gajamap.base.UserData
import com.pg.gajamap.data.model.Client
import com.pg.gajamap.databinding.FragmentEditProfileBinding
import com.pg.gajamap.viewmodel.ClientViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class EditProfileFragment : BaseFragment<FragmentEditProfileBinding>(R.layout.fragment_edit_profile) {

    override val viewModel by viewModels<ClientViewModel> {
        ClientViewModel.SettingViewModelFactory("tmp")
    }
    var clientList = UserData.clientListResponse?.clients

    private var groupId : Int = -1
    override fun initViewModel(viewModel: ViewModel) {
        binding.setVariable(BR.viewModel, viewModel)
        binding.lifecycleOwner = this@EditProfileFragment
        binding.fragment = this@EditProfileFragment
    }

    var imageFile : File? = null
    private var isCamera = false

    companion object {
        // 갤러리 권한 요청
        const val REQ_GALLERY = 1
    }

    override fun onCreateAction() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.frame_fragment, CustomerInfoFragment())
                    .addToBackStack(null)
                    .commit()
            }
        })

        binding.topBackBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_fragment, CustomerInfoFragment())
                .addToBackStack(null)
                .commit()
        }

        val name = requireActivity().intent.getStringExtra("name")
        val address1 = requireActivity().intent.getStringExtra("address1")
        val address2 = requireActivity().intent.getStringExtra("address2")
        val phone = requireActivity().intent.getStringExtra("phoneNumber")
        val image = requireActivity().intent.getStringExtra("filePath")
        val latitude = requireActivity().intent.getDoubleExtra("latitude", 0.0)
        val longitude = requireActivity().intent.getDoubleExtra("longtitude", 0.0)
        binding.infoProfileNameEt.setText(name)
        binding.infoProfileAddressTv1.text = address1
        binding.infoProfileAddressTv2.setText(address2)
        binding.infoProfilePhoneEt.setText(phone)

        if(image != null){
            val imageUrl = UserData.imageUrlPrefix
            val file = imageUrl + image
            Glide.with(binding.infoProfileImg.context)
                .load(file)
                .fitCenter()
                .apply(RequestOptions().override(500,500))
                .error(R.drawable.profile_img_origin)
                .into(binding.infoProfileImg)
        }

        binding.infoProfileCameraBtn.setOnClickListener {
            selectGallery()
            isCamera = true
        }
        if(!isCamera){
            sendImage1()
        }
    }

    // 이미지를 결과값으로 받는 변수
    private val imageResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()){
            result ->
        if (result.resultCode == Activity.RESULT_OK){
            // 이미지를 받으면 ImageView에 적용
            val imageUri = result.data?.data
            Log.d("img", imageUri.toString())
            imageUri?.let{
                // 서버 업로드를 위해 파일 형태로 변환
                val file = File(getRealPathFromURI(it))
                val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), file)
                val body = MultipartBody.Part.createFormData("clientImage", file.name, requestFile)
                sendImage(body)

                // 이미지를 불러온다
                Glide.with(this)
                    .load(imageUri)
                    .fitCenter()
                    .apply(RequestOptions().override(500,500))
                    .into(binding.infoProfileImg)
            }
        }
    }

    // 이미지 실제 경로 반환
    fun getRealPathFromURI(uri: Uri): String {
        val buildName = Build.MANUFACTURER
        if(buildName.equals("Xiaomi")){
            return uri.path!!
        }
        var columnIndex = 0
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = activity?.contentResolver?.query(uri, proj, null,null,null)
        if(cursor!!.moveToFirst()){
            columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        }
        val result = cursor.getString(columnIndex)
        cursor.close()
        return result
    }

    // 갤러리를 부르는 메서드
    private fun selectGallery(){
//        val writePermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
//        val readPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
//
//        // 권한 확인
//        if(writePermission == PackageManager.PERMISSION_DENIED || readPermission == PackageManager.PERMISSION_DENIED){
//            // 권한 요청
//            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
//                EditProfileFragment.REQ_GALLERY
//            )
//        }else{
//            // 권한이 있는 경우 갤러리 실행
//            val intent = Intent(Intent.ACTION_PICK)
//            // intent의 data와 type을 동시에 설정하는 메서드
//            intent.setDataAndType(
//                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/*"
//            )
//            imageResult.launch(intent)
//        }

        val intent = Intent(Intent.ACTION_PICK)
        // intent의 data와 type을 동시에 설정하는 메서드
        intent.setDataAndType(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/*"
        )
        imageResult.launch(intent)
    }

    private fun sendImage(clientImage: MultipartBody.Part){
        //확인 버튼
        binding.btnSubmit.setOnClickListener {
            val clientId = requireActivity().intent.getLongExtra("clientId", -1).toString()
            Log.d("edit", clientId)
            val clientName1 = binding.infoProfileNameEt.text
            Log.d("edit", clientName1.toString())
            val clientName = clientName1.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val groupId1 = requireActivity().intent.getLongExtra("groupId", -1).toString()
            Log.d("edit", groupId1)
            val groupId = groupId1.toRequestBody("text/plain".toMediaTypeOrNull())
            val phoneNumber1 = binding.infoProfilePhoneEt.text
            Log.d("edit", phoneNumber1.toString())
            val phoneNumber = phoneNumber1.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val mainAddress1 = requireActivity().intent.getStringExtra("address1")
            Log.d("edit", mainAddress1!!)
            val mainAddress = mainAddress1.toRequestBody("text/plain".toMediaTypeOrNull())
            val detail1 = binding.infoProfileAddressTv2.text
            Log.d("edit", detail1.toString())
            val detail = detail1.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val latitude1 = requireActivity().intent.getDoubleExtra("latitude", 0.0)
            val longitude1 = requireActivity().intent.getDoubleExtra("longtitude", 0.0)
            Log.d("edit", latitude1.toString())
            val latitude = latitude1.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val longitude = longitude1.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val isBasicImage1 = false
            val isBasicImage = isBasicImage1.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            viewModel.putClient( groupId1.toLong(), clientId.toLong(), clientName, groupId, phoneNumber, mainAddress , detail, latitude, longitude, clientImage, isBasicImage)
            viewModel.putClient.observe(viewLifecycleOwner, Observer {
                Log.d("editwhy", it.toString())
                // 클라이언트 리스트 가져오기
                //val clientList = UserData.clientListResponse
                val targetClientId = requireActivity().intent.getLongExtra("clientId", -1).toString()

                // 클라이언트 리스트가 null이 아니고, clients가 null이 아닌 경우에만 처리
                clientList?.let { clients ->
                    // 특정 clientId에 해당하는 클라이언트 찾기
                    val targetClient = clients.find { it.clientId == targetClientId.toLong() }

                    // 해당 clientId의 클라이언트를 찾았을 경우 값 변경
                    targetClient?.apply {
                        this.clientId = it.clientId
                        this.groupInfo.groupId = it.groupInfo.groupId
                        this.groupInfo.groupName = it.groupInfo.groupName
                        this.clientName = it.clientName
                        this.phoneNumber = it.phoneNumber
                        this.address.mainAddress = it.address.mainAddress
                        this.address.detail = it.address.detail
                        this.location.latitude = it.location.latitude
                        this.location.longitude = it.location.longitude
                        this.image.filePath = it.image.filePath
                        this.image.originalFileName = it.image.originalFileName
                        this.distance = it.distance
                        this.createdAt = it.createdAt

                        // 변경된 클라이언트 정보를 클라이언트 리스트에 업데이트
                        clientList!!.indexOf(this).let { index ->
                            clientList!![index] = this
                        }
                        val bundle = Bundle()
                        bundle.putString("clientName", this.clientName)
                        bundle.putString("address1", it.address.mainAddress)
                        bundle.putString("address2", it.address.detail)
                        bundle.putString("phone", it.phoneNumber)
                        bundle.putString("image", it.image.filePath)

                        val customerInfoFragment = CustomerInfoFragment()
                        customerInfoFragment.arguments = bundle
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.frame_fragment, customerInfoFragment)
                            .addToBackStack(null)
                            .commit()
                    }
                }
                Log.d("editlist", clientList.toString())
            })
        }

    }

    private fun sendImage1(){
        //확인 버튼
        binding.btnSubmit.setOnClickListener {
            val clientId = requireActivity().intent.getLongExtra("clientId", -1).toString()
            Log.d("edit", clientId)
            val clientName1 = binding.infoProfileNameEt.text
            Log.d("edit", clientName1.toString())
            val clientName = clientName1.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val groupId1 = requireActivity().intent.getLongExtra("groupId", -1).toString()
            Log.d("edit", groupId1)
            val groupId = groupId1.toRequestBody("text/plain".toMediaTypeOrNull())
            val phoneNumber1 = binding.infoProfilePhoneEt.text
            Log.d("edit", phoneNumber1.toString())
            val phoneNumber =
                phoneNumber1.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val mainAddress1 = requireActivity().intent.getStringExtra("address1")
            Log.d("edit", mainAddress1!!)
            val mainAddress = mainAddress1.toRequestBody("text/plain".toMediaTypeOrNull())
            val detail1 = binding.infoProfileAddressTv2.text
            Log.d("edit", detail1.toString())
            val detail = detail1.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val latitude1 = requireActivity().intent.getDoubleExtra("latitude", 0.0).toString()
            val longitude1 = requireActivity().intent.getDoubleExtra("longtitude", 0.0).toString()
            Log.d("edit", latitude1)
            val latitude = latitude1.toRequestBody("text/plain".toMediaTypeOrNull())
            val longitude = longitude1.toRequestBody("text/plain".toMediaTypeOrNull())
            val isBasicImage1 = true
            val isBasicImage =
                isBasicImage1.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            viewModel.putClient(
                groupId1.toLong(),
                clientId.toLong(),
                clientName,
                groupId,
                phoneNumber,
                mainAddress,
                detail,
                latitude,
                longitude,
                null,
                isBasicImage
            )
            viewModel.putClient.observe(viewLifecycleOwner, Observer {
                Log.d("editwhy", it.toString())
                Log.d("postAddDirect", it.toString())
                // 클라이언트 리스트 가져오기
                //val clientList = UserData.clientListResponse
                val targetClientId = requireActivity().intent.getStringExtra("clientId")

                // 클라이언트 리스트가 null이 아니고, clients가 null이 아닌 경우에만 처리
                clientList?.let { clients ->
                    // 특정 clientId에 해당하는 클라이언트 찾기
                    val targetClient = clients.find { it.clientId == targetClientId!!.toLong() }
                    Log.d("targetClient", targetClient.toString())
                    // 해당 clientId의 클라이언트를 찾았을 경우 값 변경
                    targetClient?.apply {
                        this.clientId = it.clientId
                        this.groupInfo.groupId = it.groupInfo.groupId
                        this.groupInfo.groupName = it.groupInfo.groupName
                        this.clientName = it.clientName
                        this.phoneNumber = it.phoneNumber
                        this.address.mainAddress = it.address.mainAddress
                        this.address.detail = it.address.detail
                        this.location.latitude = it.location.latitude
                        this.location.longitude = it.location.longitude
                        this.image.filePath = it.image.filePath
                        this.image.originalFileName = it.image.originalFileName
                        this.distance = it.distance
                        this.createdAt = it.createdAt

                        // 변경된 클라이언트 정보를 클라이언트 리스트에 업데이트
                        clientList!!.indexOf(this).let { index ->
                            clientList!![index] = this
                        }
                    }
                }
                Log.d("editlist", clientList.toString())
                parentFragmentManager.beginTransaction()
                    .replace(R.id.frame_fragment, CustomerInfoFragment())
                    .addToBackStack(null)
                    .commit()
            })
        }

    }
}