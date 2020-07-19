package co.candyhouse.app.tabs.account

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.base.BaseFG
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.UserState
import com.amazonaws.mobile.client.results.UserCodeDeliveryDetails
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes
import co.candyhouse.app.tabs.MainActivity
import co.candyhouse.app.tabs.account.login.LoginFragment
import co.candyhouse.app.tabs.devices.ssm2.room.avatatImagGenaroter
import co.candyhouse.sesame.server.CHAccountManager
import co.candyhouse.app.R
import co.utils.SharedPreferencesUtils
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertStyle
import co.utils.L
import co.utils.alertview.AlertView
import co.utils.alertview.objects.AlertAction
import kotlinx.android.synthetic.main.fg_me.*
import kotlinx.android.synthetic.main.fragment_register.family_name
import kotlinx.android.synthetic.main.fragment_register.given_name
import kotlinx.android.synthetic.main.fragment_register.mail
import pe.startapps.alerts.ext.inputNameAlert
import java.lang.Exception

class MeFG : BaseFG() {
    companion object {
        var instance: MeFG? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fg_me, container, false)
//        L.d("hcia", "我 onCreateView:")
        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        L.d("hcia", "我 onViewCreated:")

        signout_btn.setOnClickListener {
            val alert = AlertView("", "", AlertStyle.IOS)
            alert.addAction(AlertAction(getString(R.string.logout), AlertActionStyle.NEGATIVE) { action ->
                AWSMobileClient.getInstance().signOut()
//                CHAccountManager.logout()
            })
            alert.show(activity as AppCompatActivity)
        }
        qrcode.setOnClickListener {
            MyQrcodeFG.mailStr = mail.text.toString()
            MyQrcodeFG.givenName = given_name.text.toString()
            MyQrcodeFG.familyName = family_name.text.toString()
            findNavController().navigate(R.id.action_register_to_myQrcodeFG)
        }
        change_name_zone.setOnClickListener {
            context?.inputNameAlert(getString(R.string.edit_name), family_name.text.toString(), given_name.text.toString()) {
                confirmButtonWithDoubleEdit("OK") { name, top, down ->
                    val awsAttributes = CognitoUserAttributes()
                    awsAttributes.addAttribute("family_name", top)
                    awsAttributes.addAttribute("given_name", down)
                    AWSMobileClient.getInstance().updateUserAttributes(awsAttributes.attributes, object : Callback<List<UserCodeDeliveryDetails>> {
                        override fun onResult(result: List<UserCodeDeliveryDetails>?) {
                            SharedPreferencesUtils.given_name = down
                            SharedPreferencesUtils.family_name = top
//                            CHAccountManager.updateMyProfile(SharedPreferencesUtils.given_name.toString(), SharedPreferencesUtils.family_name.toString()) {}
                            setName()
                        }

                        override fun onError(e: Exception?) {
                            L.d("hcia", "e:" + e)
                        }
                    })
                    dismiss()
                }
                cancelButton("Cancel")
            }?.show()
        }

        setName()

        version.setText(BuildConfig.VERSION_NAME + "-" + BuildConfig.GIT_HASH + "-" + BuildConfig.BUILD_TYPE)

    }//end onViewCreated

    //todo mvvm
    fun setName() {
        mail?.post {
            mail.text = SharedPreferencesUtils.mail_id
            family_name?.text = SharedPreferencesUtils.family_name
            given_name?.text = SharedPreferencesUtils.given_name
            head.setImageDrawable(avatatImagGenaroter(SharedPreferencesUtils.given_name))
        }
    }

    override fun onResume() {
        super.onResume()
//        L.d("hcia", "我 onResume:")

        if (MainActivity.nowTab == 2) {
            (activity as MainActivity).showMenu()
        }
    }
}
