package com.pg.gajamap.ui.view

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.telephony.PhoneNumberFormattingTextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.pg.gajamap.R
import com.pg.gajamap.base.BaseActivity
import com.pg.gajamap.base.GajaMapApplication
import com.pg.gajamap.base.UserData
import com.pg.gajamap.data.model.GroupInfoResponse
import com.pg.gajamap.databinding.ActivityAddDirectBinding
import com.pg.gajamap.viewmodel.ClientViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class AddDirectActivity : BaseActivity<ActivityAddDirectBinding>(R.layout.activity_add_direct) {
    var latitude = 0.0
    var longitude = 0.0
    var address = ""

    override val viewModel by viewModels<ClientViewModel> {
        ClientViewModel.SettingViewModelFactory("tmp")
    }

    override fun preLoad() {
    }

    private var groupId: Long = -1
    override fun initViewModel(viewModel: ViewModel) {
        binding.lifecycleOwner = this@AddDirectActivity
        binding.viewModel = this.viewModel
    }

    private var isBtnActivated = false // 버튼 활성화 되었는지 여부, true면 활성화, false면 비활성화
    private var isCamera = false

    override fun onCreateAction() {
        latitude = intent.getDoubleExtra("latitude", 0.0)
        longitude = intent.getDoubleExtra("longitude", 0.0)
        address = intent.getStringExtra("address")!!

        binding.topBackBtn.setOnClickListener {
            finish()
        }
        //주소 데이터 가져오기
        binding.infoProfileAddressTv1.text = address

        // 전화번호 작성 시 자동으로 하이픈 추가
        binding.infoProfilePhoneEt.addTextChangedListener(PhoneNumberFormattingTextWatcher())

        // deprecated 된 onBackPressed() 대신 사용
        // 위에서 생성한 콜백 인스턴스 붙여주기
        this.onBackPressedDispatcher.addCallback(this, callback)

        //스피너
        viewModel.checkGroup()
        viewModel.checkGroup.observe(this, Observer { it ->
            // GroupResponse에서 GroupInfoResponse의 groupName 속성을 추출하여 리스트로 변환합니다.
            val groupNames = mutableListOf<String>()
            // groupResponse의 groupInfos에서 각 GroupInfoResponse의 groupName을 추출하여 리스트에 추가합니다.
            it.groupInfos.forEach { groupInfo ->
                groupNames.add(groupInfo.groupName)
            }
            groupNames.add(0, "그룹 선택")

            val adapter = object : ArrayAdapter<String>(this, R.layout.spinner_list, groupNames) {

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
            binding.infoProfileGroup.adapter = adapter

            binding.infoProfileGroup.setSelection(it.groupInfos.indexOfFirst { it.groupId == UserData.groupinfo!!.groupId } + 1)

        })

        binding.infoProfileGroup.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                    //binding.result.text = data[pos] //배열이라서 []로 된다.
                    //textView를 위에서 선언한 리스트(data)와 연결. [pos]는 리스트에서 선택된 항목의 위치값.
                    // 스피너에서 선택한 아이템의 그룹 아이디를 가져옵니다.
                    //if (pos == 0) return

                    if (pos != 0) {
                        val selectedGroupInfoResponse: GroupInfoResponse =
                            viewModel.checkGroup.value?.groupInfos?.get(pos - 1) ?: return
                        groupId = selectedGroupInfoResponse.groupId
                        chkBtnActivate()
                    } else {
                        groupId = -1L
                        chkBtnActivate()
                    }
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {

                }
            }

        binding.infoProfileCameraBtn.setOnClickListener {

            if (GajaMapApplication.prefs.getString("authority", "") == "FREE") {
                Toast.makeText(this, "VIP등급만 사용 가능합니다.", Toast.LENGTH_SHORT).show()
            } else {
                openImagePickOption()
            }

        }
        if (!isCamera) {
            sendImage1()
        }

        chkInputData()
        onContentAdd()

        binding.topBackBtn.setOnClickListener {
            // 지도 fragment로 이동
            finish()
        }
    }

    // 필수 입력사항에 값이 변경될 때 확인 버튼 활성화 시킬 함수 호출
    private fun onContentAdd() {
        binding.infoProfileNameEt.addTextChangedListener {
            chkBtnActivate()
        }
        binding.infoProfilePhoneEt.addTextChangedListener {
            chkBtnActivate()
        }
    }

    private fun chkInputData() =
        binding.infoProfileNameEt.text.isNotEmpty() && binding.infoProfilePhoneEt.text.isNotEmpty()

    // 필수 입력사항을 모두 작성하였을 때 확인 버튼 활성화시키기
    private fun chkBtnActivate() {
        // 버튼이 활성화되어 있지 않은 상황에서 확인
        if (chkInputData() && groupId != -1L) {
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

    // 이미지를 결과값으로 받는 변수
    private val imageResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // 이미지를 받으면 ImageView에 적용
            val imageUri = result.data?.data
            Log.d("imgUrl", imageUri.toString())
            imageUri?.let {
                // 서버 업로드를 위해 파일 형태로 변환
                val file = File(getRealPathFromURI(it))
                val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), file)
                val body = MultipartBody.Part.createFormData("clientImage", file.name, requestFile)
                sendImage(body)

                // 이미지를 불러온다
                Glide.with(this)
                    .load(imageUri)
                    .fitCenter()
                    .apply(RequestOptions().override(500, 500))
                    .into(binding.infoProfileImg)
            }
        }
    }

    // 이미지 실제 경로 반환
    fun getRealPathFromURI(uri: Uri): String {
        val buildName = Build.MANUFACTURER
        if (buildName.equals("Xiaomi")) {
            return uri.path!!
        }
        var columnIndex = 0
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, proj, null, null, null)
        if (cursor!!.moveToFirst()) {
            columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        }
        val result = cursor.getString(columnIndex)
        cursor.close()
        return result
    }

    // 갤러리를 부르는 메서드
    private fun selectGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        // intent의 data와 type을 동시에 설정하는 메서드
        intent.setDataAndType(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*"
        )
        imageResult.launch(intent)

    }

    private fun openImagePickOption() {
        val items = arrayOf<CharSequence>("앨범에서 사진 선택", "기본 이미지로 변경")
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("프로필 사진")
        builder.setItems(items, DialogInterface.OnClickListener { dialog, item ->
            if (items[item] == "앨범에서 사진 선택") {
                selectGallery()
                isCamera = true
            } else if (items[item] == "기본 이미지로 변경") {
                Glide.with(this)
                    .load(R.drawable.profile_img_origin)
                    .fitCenter()
                    .apply(RequestOptions().override(500, 500))
                    .into(binding.infoProfileImg)
                sendImage1()
                isCamera = false
            }
        })
        builder.show()
    }

    private fun sendImage(clientImage: MultipartBody.Part) {
        //확인 버튼
        binding.btnSubmit.setOnClickListener {
            val clientName1 = binding.infoProfileNameEt.text
            val clientName = clientName1.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val groupId1 = groupId.toString()
            val groupId = groupId1.toRequestBody("text/plain".toMediaTypeOrNull())
            val phoneNumber1 = binding.infoProfilePhoneEt.text
            val phoneNumber = phoneNumber1.toString().replace("-", "")
                .toRequestBody("text/plain".toMediaTypeOrNull())
            val mainAddress1 = binding.infoProfileAddressTv1.text.toString()
            val mainAddress = mainAddress1.toRequestBody("text/plain".toMediaTypeOrNull())
            val detail1 = binding.infoProfileAddressTv2.text
            val detail = detail1.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val latitude = latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val longitude = longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val isBasicImage1 = false
            val isBasicImage =
                isBasicImage1.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            dialogShow()
            viewModel.postClient(
                clientName,
                groupId,
                phoneNumber,
                mainAddress,
                detail,
                latitude,
                longitude,
                clientImage,
                isBasicImage
            )
            viewModel.postClient.observe(this, Observer {
                if (UserData.groupinfo?.groupId == viewModel.postClient.value?.body()?.groupInfo?.groupId || UserData.groupinfo?.groupId == -1L) {
                    Log.d("postAddDirect", it.body().toString())
                    viewModel.postClient.value!!.body()
                        ?.let { it1 -> UserData.clientListResponse?.clients?.add(it1) }
                    UserData.groupinfo!!.clientCount = UserData.groupinfo!!.clientCount + 1
                }
                dialogHide()
                finish()
            })

            viewModel.postErrorClient.observe(this, Observer {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                dialogHide()
                finish()
            })
        }
    }

    private fun sendImage1() {
        //확인 버튼
        binding.btnSubmit.setOnClickListener {
            val clientName1 = binding.infoProfileNameEt.text
            val clientName = clientName1.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val groupId1 = groupId.toString()
            val groupId = groupId1.toRequestBody("text/plain".toMediaTypeOrNull())
            val phoneNumber1 = binding.infoProfilePhoneEt.text
            val phoneNumber = phoneNumber1.toString().replace("-", "")
                .toRequestBody("text/plain".toMediaTypeOrNull())
            val mainAddress1 = binding.infoProfileAddressTv1.text.toString()
            val mainAddress = mainAddress1.toRequestBody("text/plain".toMediaTypeOrNull())
            val detail1 = binding.infoProfileAddressTv2.text
            val detail = detail1.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val latitude = latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val longitude = longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val isBasicImage1 = true
            val isBasicImage =
                isBasicImage1.toString().toRequestBody("text/plain".toMediaTypeOrNull())

            dialogShow()
            viewModel.postClient(
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
            viewModel.postClient.observe(this, Observer {
                if (UserData.groupinfo?.groupId == viewModel.postClient.value?.body()?.groupInfo?.groupId || UserData.groupinfo?.groupId == -1L) {
                    Log.d("postAddDirect", it.body().toString())
                    viewModel.postClient.value!!.body()
                        ?.let { it1 -> UserData.clientListResponse?.clients?.add(it1) }
                    UserData.groupinfo!!.clientCount = UserData.groupinfo!!.clientCount + 1
                }
                dialogHide()
                finish()
            })

            viewModel.postErrorClient.observe(this, Observer {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                dialogHide()
                finish()
            })

        }
    }

    private fun dialogShow() {
        binding.progress.isVisible = true
        binding.btnSubmit.text = ""
        window?.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    private fun dialogHide() {
        binding.progress.isVisible = false
        binding.btnSubmit.text = "확인"
        window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    // 뒤로가기 두 번 클릭 시 앱 종료
    // 콜백 인스턴스 생성
    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()
        }
    }
}