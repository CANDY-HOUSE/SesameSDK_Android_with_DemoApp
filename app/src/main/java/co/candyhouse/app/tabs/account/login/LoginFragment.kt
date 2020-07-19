package co.candyhouse.app.tabs.account.login

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import co.candyhouse.app.BuildConfig.BUILD_TYPE
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.results.SignInResult
import co.candyhouse.app.tabs.devices.DeviceListFG
import co.candyhouse.app.tabs.friends.FriendsFG
import co.candyhouse.sesame.server.CHAccountManager
import co.candyhouse.app.R
import co.utils.L
import kotlinx.android.synthetic.main.fragment_login.*


class LoginFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(activity, R.style.AppTheme_FlatDialog)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            dialog?.window?.setStatusBarColor(Color.WHITE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                dialog?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fg = this
    }

    override fun onDestroy() {
        fg = null
        super.onDestroy()
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)
//        view.setFocusableInTouchMode(true)
//        view.requestFocus()
//
//        view.setOnKeyListener(object : View.OnKeyListener {
//            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
//                return true
//            }
//        })

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        test_zone.visibility = if (BUILD_TYPE == "debug") View.VISIBLE else View.GONE


        pBar.visibility = View.GONE
        sign_up.setOnClickListener {
            SignUpFragment.newInstance().show(activity!!.supportFragmentManager, "")
        }
        forget_password.setOnClickListener {
            ForgetPassWordFragment.newInstance().show(activity!!.supportFragmentManager, "")
        }

        loginBtn.setOnClickListener {

            if (nameEdt.text.isEmpty()) {
                return@setOnClickListener
            }
            if (passwordEdt.text.isEmpty()) {
                return@setOnClickListener
            }
            if (pBar.isShown) {
                return@setOnClickListener
            }
            pBar.visibility = View.VISIBLE




//            AWSMobileClient.getInstance().signIn(nameEdt.text.toString().toMail(), passwordEdt.text.toString(), null, object : Callback<SignInResult> {
//                override fun onResult(result: SignInResult?) {
//                    L.d("hcia", "signInState:" + result?.signInState + "isSignedIn:" + AWSMobileClient.getInstance().isSignedIn )
//
//                    this@LoginFragment.dismiss()
//                }
//
//                override fun onError(exception: Exception?) {
//                    L.d("hcia", "signIn e:" + exception)
//                    toastError(exception)
//
//                    pBar?.post {
//                        pBar.visibility = View.GONE
//                    }
//
//                }
//            })
        }
        tsetest.setOnClickListener {
            nameEdt.setText("tse@candyhouse.co")
            passwordEdt.setText("222222")
            loginBtn.performClick()
        }
        hootest.setOnClickListener {
            nameEdt.setText("tse+5@candyhouse.co")
            passwordEdt.setText("111111")
            loginBtn.performClick()
        }

        peter3.setOnClickListener {
            nameEdt.setText("tse+27@candyhouse.co")
            passwordEdt.setText("111111")
            loginBtn.performClick()
        }

        tse31test.setOnClickListener {
            nameEdt.setText("tse+31@candyhouse.co")
            passwordEdt.setText("111111")
            loginBtn.performClick()
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        if (isShowing) {
            return
        }
        super.show(manager, tag)
        isShowing = true
    }

    override fun dismiss() {
        super.dismiss()
        isShowing = false
    }

    companion object {
        var isShowing = false
        var fg: LoginFragment? = null

        @JvmStatic
        fun newInstance() =
                LoginFragment()
    }
}


fun String.toMail(): String {
    val ss = this.toLowerCase().replace(" ", "").replace(" ", "").replace("\n", "")
    return ss
}
