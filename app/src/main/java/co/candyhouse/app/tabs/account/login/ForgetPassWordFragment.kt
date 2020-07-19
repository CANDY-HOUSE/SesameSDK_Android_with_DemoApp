package co.candyhouse.app.tabs.account.login

import android.app.Dialog
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import co.candyhouse.app.R
import com.amazonaws.AmazonServiceException
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.results.ForgotPasswordResult
import com.amazonaws.mobile.client.results.ForgotPasswordState
import co.utils.L
import kotlinx.android.synthetic.main.fragment_fg_pass.*
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.android.synthetic.main.fragment_login.pBar
import pe.startapps.alerts.ext.Alert
import java.lang.Exception


class ForgetPassWordFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(activity, R.style.AppTheme_FlatDialog)
        dialog.window.attributes.windowAnimations = R.style.DialogAnimation
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            dialog?.window?.setStatusBarColor(Color.WHITE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                dialog?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_fg_pass, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pBar.visibility = View.GONE
        backicon.setOnClickListener {
            dismiss()
        }

//        mail_edt.setText("tse@candyhouse.co")
        confirmBtn.setOnClickListener {

            if (confirm_edt.text.isEmpty()) {
                return@setOnClickListener
            }
//            L.d("hcia", "confirm_edt.text.toString():" + mail_edt.text.toString())
//            L.d("hcia", "confirm_edt.text.toString():" + confirm_edt.text.toString())
//            L.d("hcia", "confirm_edt.text.toString():" + new_password_edt.text.toString())
            AWSMobileClient.getInstance().confirmForgotPassword(new_password_edt.text.toString(), confirm_edt.text.toString(), object : Callback<ForgotPasswordResult> {
                override fun onResult(result: ForgotPasswordResult?) {
                    when (result?.state) {
                        ForgotPasswordState.CONFIRMATION_CODE -> {

                        }
                        ForgotPasswordState.DONE -> {
                            toastMSG("OK")
                            dismiss()
                        }
                        ForgotPasswordState.UNKNOWN -> {

                        }
                    }
                }

                override fun onError(exception: Exception?) {
                    when (exception) {
                        is AmazonServiceException -> {
                            toastMSG(exception.errorMessage)
                        }
                        else -> {
                            toastMSG(exception?.localizedMessage)
                        }
                    }
                }
            })

        }
        checkBtn.setOnClickListener {
            if (mail_edt.text.isEmpty()) {
                return@setOnClickListener
            }
            AWSMobileClient.getInstance().forgotPassword(mail_edt.text.toString().toMail(), object : Callback<ForgotPasswordResult> {
                override fun onResult(result: ForgotPasswordResult?) {
                    when (result?.state) {
                        ForgotPasswordState.CONFIRMATION_CODE -> {
//                            result.parameters.destination
                            L.d("hcia", "result.parameters.destination:" + result.parameters.destination)
                            activity?.runOnUiThread {
                                Alert("Code Sent", "Code Sent to " + mail_edt.text.toString().toMail())?.show()
                            }
                            activity?.runOnUiThread {
                                second_zone_p.visibility = View.VISIBLE
                            }

                        }
                        ForgotPasswordState.DONE -> {
                            activity?.runOnUiThread {
                                second_zone_p.visibility = View.VISIBLE
                            }
                        }
                        ForgotPasswordState.UNKNOWN -> {

                        }
                    }
                }

                override fun onError(exception: Exception?) {
                    when (exception) {
                        is AmazonServiceException -> {
                            toastMSG(exception.errorMessage)
                        }
                        else -> {
                            toastMSG(exception?.localizedMessage)
                        }
                    }
                }
            })

        }
    }


    override fun show(manager: FragmentManager, tag: String?) {
        if (isShowing) {
            return
        }
        super.show(manager, tag)
        isShowing = true
    }

    override fun onDestroyView() {
        LoginFragment.fg?.nameEdt?.setText(mail_edt.text.toString().toMail())
        LoginFragment.fg?.passwordEdt?.setText(new_password_edt.text)
        super.onDestroyView()
        isShowing = false
    }

    companion object {
        var isShowing = false

        @JvmStatic
        fun newInstance() =
                ForgetPassWordFragment()
    }
}
