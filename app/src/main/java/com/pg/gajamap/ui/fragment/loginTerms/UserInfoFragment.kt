package com.pg.gajamap.ui.fragment.loginTerms

import android.content.Context
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import com.pg.gajamap.R
import com.pg.gajamap.base.BaseFragment
import com.pg.gajamap.databinding.FragmentLocationInfoBinding
import com.pg.gajamap.databinding.FragmentServiceInfoBinding
import com.pg.gajamap.databinding.FragmentUserInfoBinding
import com.pg.gajamap.viewmodel.GetClientViewModel
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class UserInfoFragment: BaseFragment<FragmentUserInfoBinding>(R.layout.fragment_user_info) {


    override val viewModel by viewModels<GetClientViewModel> {
        GetClientViewModel.AddViewModelFactory("tmp")
    }
    override fun initViewModel(viewModel: ViewModel) {
        binding.lifecycleOwner = this@UserInfoFragment
        binding.viewModel = this.viewModel
    }



    override fun onCreateAction() {


        binding.userInfoContent.text = getAssetsTextString(requireContext(),"UserInfoContent")

    }

    fun getAssetsTextString(mContext: Context, fileName: String): String{
        val termsString = StringBuilder()
        val reader: BufferedReader

        try {
            reader = BufferedReader(
                InputStreamReader(mContext.resources.assets.open("$fileName.txt"))
            )

            var str: String?
            while (reader.readLine().also { str = it } != null) {
                termsString.append(str)
                termsString.append('\n') //줄 변경
            }
            reader.close()
            return termsString.toString()

        } catch (e: IOException) {
            e.printStackTrace()
        }
        return "fail"
    }


}