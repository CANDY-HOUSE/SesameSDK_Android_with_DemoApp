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
import co.candyhouse.sesame.server.CHAccountManager
import co.candyhouse.app.R
import co.utils.SharedPreferencesUtils
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertStyle
import co.utils.L
import co.utils.alertview.AlertView
import co.utils.alertview.objects.AlertAction
import co.utils.textdrawable.util.avatatImagGenaroter
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

        qrcode.setOnClickListener {
            MyQrcodeFG.mailStr = mail.text.toString()
            MyQrcodeFG.givenName = given_name.text.toString()
            MyQrcodeFG.familyName = family_name.text.toString()
            findNavController().navigate(R.id.action_register_to_myQrcodeFG)
        }


        version.setText(BuildConfig.VERSION_NAME + "-" + BuildConfig.GIT_HASH + "-" + BuildConfig.BUILD_TYPE)

    }//end onViewCreated


    override fun onResume() {
        super.onResume()
//        L.d("hcia", "我 onResume:")

        if (MainActivity.nowTab == 1) {
            (activity as MainActivity).showMenu()
        }
    }
}
