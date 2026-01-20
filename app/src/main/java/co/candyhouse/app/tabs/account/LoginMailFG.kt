package co.candyhouse.app.tabs.account

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.Patterns
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseNFG
import co.candyhouse.app.databinding.FgLoginMailBinding
import co.candyhouse.app.tabs.devices.model.CHLoginViewModel
import co.candyhouse.sesame.utils.L
import co.utils.safeNavigate
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.UserState
import com.amazonaws.mobile.client.results.SignInResult
import com.amazonaws.mobile.client.results.SignInState
import com.amazonaws.mobile.client.results.SignUpResult
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes
import com.amazonaws.services.cognitoidentityprovider.model.UsernameExistsException

class LoginMailFG : BaseNFG<FgLoginMailBinding>() {
    private val mloginViewModel: CHLoginViewModel by activityViewModels()
    override fun getViewBinder() = FgLoginMailBinding.inflate(layoutInflater)

    override fun onPause() {
        super.onPause()

        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private var isLoginClick = false
    private fun checkEdt(s: String) {
        isLoginClick = isValidEmail(s)
        if (isLoginClick) {
            bind.login.setBackgroundResource(R.drawable.round_blue)
        } else {
            bind.login.setBackgroundResource(R.drawable.round_blue_no)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (mloginViewModel.gUserState.value == UserState.SIGNED_IN) {
            return
        }
        bind.etInput.isFocusableInTouchMode = true
        bind.etInput.requestFocus()
        bind.etInput.addTextChangedListener {
            it?.apply {
                checkEdt(this.toString())
            }
        }

        (bind.etInput.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(bind.etInput, 0)

        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        bind.login.setOnClickListener {
            if (isLoginClick) {
                nextStep()
            }
        }
        bind.etInput.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    // 通过按键触发登录功能，需要先检验输入合法性
                    if (isLoginClick) {
                        nextStep()
                    } else {
                        Toast.makeText(activity, R.string.login_fg_input_mail_warn, Toast.LENGTH_SHORT).show()
                    }
                    return true
                }
                return false
            }
        })
        bind.centerZone.viewTreeObserver?.addOnGlobalLayoutListener {
            val rect = Rect()
            activity?.window?.decorView?.getWindowVisibleDisplayFrame(rect)
            val screenHeight = activity?.window?.decorView?.height ?: 0
            val softHeight: Int = screenHeight - rect.bottom
            bind.centerZone.scrollTo(0, if (softHeight > screenHeight / 4) (screenHeight / 8) else 0)
        }
    }

    private fun nextStep() {
        requireActivity().findViewById<View>(R.id.progress_g).visibility = View.VISIBLE

        (requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(
            bind.etInput.windowToken,
            0
        )

        val awsAttributes = CognitoUserAttributes()
        awsAttributes.addAttribute("email", bind.etInput.text.toString())
        AWSMobileClient.getInstance()
            .signUp(bind.etInput.text.toString(), "dummypwk", awsAttributes.attributes, mapOf(), object : Callback<SignUpResult> {
                override fun onResult(result: SignUpResult?) {
                    goSignIIn(bind.etInput.text.toString())
                }

                override fun onError(exception: Exception?) {
                    if (exception is UsernameExistsException) {
                        goSignIIn(bind.etInput.text.toString())
                        return
                    }
                    L.d("hcia", "💋signUp ? :$exception")

                    if (!isAdded) return
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireActivity(), exception?.localizedMessage, Toast.LENGTH_LONG).show()
                        requireActivity().findViewById<View>(R.id.progress_g).visibility = View.GONE
                    }
                }
            })
    }

    private fun goSignIIn(mail: String) {
        AWSMobileClient.getInstance().signIn(mail, "dummypwk", null, object : Callback<SignInResult> {
            override fun onResult(result: SignInResult?) {
                L.d("hcia", "💋 signInState:" + result!!.signInState)
                when (result.signInState) {
                    SignInState.DONE -> {
                        L.d("hcia", "💋 登入完成")
                    }

                    SignInState.CUSTOM_CHALLENGE -> {
                        requireActivity().runOnUiThread {
                            requireActivity().findViewById<View>(R.id.progress_g).visibility = View.GONE

                            safeNavigate(R.id.action_LoginMailFG_to_LoginVerifiCodeFG)
                        }
                    }

                    else -> L.d("hcia", "Unsupported sign-in confirmation: " + result.signInState)
                }
            }

            override fun onError(exception: Exception?) {
                L.d("hcia", " 💋signIn e:$exception")
            }
        })
    }

    // 退出界面时，对进度条进行隐藏保护：退出此界面时，进度条应该被隐藏，否则会影响其他界面的显示
    override fun onDestroyView() {
        if (View.GONE != requireActivity().findViewById<View>(R.id.progress_g).visibility) {
            requireActivity().findViewById<View>(R.id.progress_g).visibility = View.GONE
        }
        super.onDestroyView()
    }

}