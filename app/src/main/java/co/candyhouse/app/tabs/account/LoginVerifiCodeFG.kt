package co.candyhouse.app.tabs.account

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseNFG
import co.candyhouse.app.databinding.FgVerifyMailBinding
import co.candyhouse.app.tabs.devices.model.CHLoginViewModel
import co.candyhouse.sesame.utils.L

import co.utils.alertview.fragments.toastMSG
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.results.SignInResult
import com.amazonaws.mobile.client.results.UserCodeDeliveryDetails
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes
import com.amazonaws.mobileconnectors.cognitoidentityprovider.util.CognitoServiceConstants


class LoginVerifiCodeFG : BaseNFG<FgVerifyMailBinding>() {
    private val mloginViewModel: CHLoginViewModel by activityViewModels()
    override fun getViewBinder()= FgVerifyMailBinding.inflate(layoutInflater)

    fun confirmSignIn(code: String) {
        requireActivity().runOnUiThread {
            requireActivity().findViewById<View>(R.id.progress_g).visibility=View.VISIBLE
        }

        val res: MutableMap<String, String> = HashMap()
        res[CognitoServiceConstants.CHLG_RESP_ANSWER] = code
        mloginViewModel.isJustLogin = true
        AWSMobileClient.getInstance().confirmSignIn(res, object : Callback<SignInResult?> {
            override fun onResult(signInResult: SignInResult?) {
                updateNickNameInNeed()
                activity?.runOnUiThread {
                    findNavController().popBackStack()
                    findNavController().popBackStack()
                    requireActivity().findViewById<View>(R.id.progress_g).visibility=View.GONE
                }
            }

            private fun updateNickNameInNeed() {
                val nickname = AWSMobileClient.getInstance().getUserAttributes()["nickname"]
                val email = AWSMobileClient.getInstance().getUserAttributes()["email"]
                if (nickname.isNullOrEmpty() && !TextUtils.isEmpty(email)) {
                    updateUserNameToCognito(email.toString().split("@").first())
                }
            }

            override fun onError(e: Exception) {
              
                activity?.runOnUiThread {
                    if (isAdded && view != null) {
                        toastMSG(e.localizedMessage)
                        findNavController().popBackStack()
                        mloginViewModel.isJustLogin = false
                        requireActivity().findViewById<View>(R.id.progress_g).visibility = View.GONE
                    }
                }
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind.etInput.isFocusable = true
        bind.etInput.isFocusableInTouchMode = true
        bind.etInput.requestFocus()
        (bind.etInput.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(bind.etInput, 0)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        bind.etInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length == 4) {
                    bind.etInput.isFocusable = false
                    bind.etInput.isFocusableInTouchMode = false
                    (activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(bind.etInput.windowToken, 0)
                    confirmSignIn(s.toString())
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

    }

    private fun updateUserNameToCognito(name: String) {
        val awsAttributes = CognitoUserAttributes()
        awsAttributes.addAttribute("nickname", name)
        AWSMobileClient.getInstance().updateUserAttributes(
            awsAttributes.attributes,
            object : Callback<List<UserCodeDeliveryDetails>> {
                override fun onResult(result: List<UserCodeDeliveryDetails>?) {
                    L.d("LoginVerifyCodeFG", "update nickName success")
                }
                override fun onError(e: Exception?) {
                    L.d("LoginVerifyCodeFG", "update nickName error")
                }
            })
    }

    // 退出界面时，对进度条进行隐藏保护：退出此界面时，进度条应该被隐藏，否则会影响其他界面的显示
    override fun onDestroyView() {
        if (View.GONE != requireActivity().findViewById<View>(R.id.progress_g).visibility) {
            requireActivity().findViewById<View>(R.id.progress_g).visibility=View.GONE
        }
        super.onDestroyView()
    }


}
