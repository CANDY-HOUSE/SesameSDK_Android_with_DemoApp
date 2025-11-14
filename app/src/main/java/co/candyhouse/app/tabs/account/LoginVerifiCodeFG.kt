package co.candyhouse.app.tabs.account

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseNFG
import co.candyhouse.app.databinding.FgVerifyMailBinding
import co.candyhouse.app.ext.webview.manager.WebViewPoolManager
import co.candyhouse.app.tabs.devices.model.CHLoginViewModel
import co.candyhouse.sesame.utils.L
import co.utils.alertview.fragments.toastMSG
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.results.SignInResult
import com.amazonaws.mobile.client.results.UserCodeDeliveryDetails
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes
import com.amazonaws.mobileconnectors.cognitoidentityprovider.util.CognitoServiceConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LoginVerifiCodeFG : BaseNFG<FgVerifyMailBinding>() {
    private val mloginViewModel: CHLoginViewModel by activityViewModels()
    override fun getViewBinder() = FgVerifyMailBinding.inflate(layoutInflater)

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
                    (activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
                        bind.etInput.windowToken,
                        0
                    )
                    confirmSignIn(s.toString())
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
    }

    fun confirmSignIn(code: String) {
        lifecycleScope.launch {
            try {
                showProgress(true)

                withContext(Dispatchers.IO) {
                    confirmSignInAsync(code)
                }

                withContext(Dispatchers.IO) {
                    updateNickNameIfNeeded()
                }

                handleLoginSuccess()
            } catch (e: Exception) {
                handleLoginError(e)
            } finally {
                showProgress(false)
            }
        }
    }

    private suspend fun confirmSignInAsync(code: String): SignInResult =
        suspendCancellableCoroutine { continuation ->
            val res = mutableMapOf(
                CognitoServiceConstants.CHLG_RESP_ANSWER to code
            )

            mloginViewModel.isJustLogin = true

            AWSMobileClient.getInstance().confirmSignIn(
                res,
                object : Callback<SignInResult?> {
                    override fun onResult(result: SignInResult?) {
                        if (continuation.isActive) {
                            continuation.resume(result ?: throw Exception("SignIn result is null"))
                        }
                    }

                    override fun onError(e: Exception) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(e)
                        }
                    }
                })
        }

    private suspend fun updateNickNameIfNeeded() = withContext(Dispatchers.IO) {
        try {
            val userAttributes = AWSMobileClient.getInstance().getUserAttributes()
            val nickname = userAttributes["nickname"]
            val email = userAttributes["email"]

            if (nickname.isNullOrEmpty() && !email.isNullOrEmpty()) {
                updateUserNameToCognito(email.split("@").firstOrNull() ?: "")
            }
        } catch (e: Exception) {
            L.e("LoginVerifyCodeFG", "Failed to get user attributes", e)
        }
    }

    private suspend fun updateUserNameToCognito(name: String) =
        suspendCancellableCoroutine { continuation ->
            val awsAttributes = CognitoUserAttributes().apply {
                addAttribute("nickname", name)
            }

            AWSMobileClient.getInstance().updateUserAttributes(
                awsAttributes.attributes,
                object : Callback<List<UserCodeDeliveryDetails>> {
                    override fun onResult(result: List<UserCodeDeliveryDetails>?) {
                        L.d("LoginVerifyCodeFG", "update nickName success")
                        if (continuation.isActive) {
                            continuation.resume(Unit)
                        }
                    }

                    override fun onError(e: Exception?) {
                        L.e("LoginVerifyCodeFG", "update nickName error")
                        if (continuation.isActive) {
                            continuation.resume(Unit)
                        }
                    }
                }
            )
        }

    private fun handleLoginSuccess() {
        if (!isAdded) return

        WebViewPoolManager.setPendingRefresh("me-index")
        findNavController().apply {
            popBackStack()
            popBackStack()
        }
    }

    private fun handleLoginError(e: Exception) {
        if (!isAdded) return

        toastMSG(e.localizedMessage)
        WebViewPoolManager.setPendingRefresh("me-index")
        findNavController().popBackStack()
        mloginViewModel.isJustLogin = false
    }

    private fun showProgress(show: Boolean) {
        if (!isAdded) return

        activity?.findViewById<View>(R.id.progress_g)?.visibility =
            if (show) View.VISIBLE else View.GONE
    }

    // 退出界面时，对进度条进行隐藏保护：退出此界面时，进度条应该被隐藏，否则会影响其他界面的显示
    override fun onDestroyView() {
        if (View.GONE != requireActivity().findViewById<View>(R.id.progress_g).visibility) {
            requireActivity().findViewById<View>(R.id.progress_g).visibility = View.GONE
        }
        super.onDestroyView()
    }

}