package co.candyhouse.app.tabs.devices.ssm2.setting.angle

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import co.candyhouse.app.base.BaseSSMFG
import co.candyhouse.app.tabs.account.login.toastMSG
import co.candyhouse.sesame.ble.*
import co.candyhouse.sesame.ble.Sesame2.CHSesame2Delegate
import co.candyhouse.sesame.deviceprotocol.CHSesame2Intention
import co.candyhouse.sesame.deviceprotocol.CHSesame2MechStatus
import co.candyhouse.app.R
import co.candyhouse.sesame.ble.Sesame2.CHSesame2
import co.utils.SharedPreferencesUtils
import kotlinx.android.synthetic.main.fg_set_angle.*
import java.lang.Math.abs


class SSM2SetAngleFG : BaseSSMFG() {
    var titleTextView: TextView? = null
    var ssmView: SesameView? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fg_set_angle, container, false)
        titleTextView = view.findViewById(R.id.titlec)
        ssmView = view.findViewById(R.id.ssmView)
        return view
    }

    @SuppressLint("SimpleDateFormat")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

       
        titleTextView?.text = SharedPreferencesUtils.preferences.getString(mSesame?.deviceId.toString(), mSesame?.deviceId.toString().toUpperCase())
        backicon.setOnClickListener {
            findNavController().navigateUp()
        }
        ssmView?.setLock(mSesame!!)
        ssmView?.setOnClickListener { mSesame?.toggle(){} }

        setunlock_zone?.setOnClickListener {
            if (mSesame?.deviceStatus?.value == CHDeviceLoginStatus.unlogined) {
                return@setOnClickListener
            }
            if(abs(mSesame?.mechStatus!!.position - mSesame?.mechSetting!!.lockPosition) < 50){
                toastMSG(getString(R.string.too_close))
                return@setOnClickListener
            }
            mSesame?.configureLockPosition(mSesame?.mechSetting!!.lockPosition, mSesame?.mechStatus!!.position){}
        }
        setlock_zone?.setOnClickListener {
            if (mSesame?.deviceStatus?.value == CHDeviceLoginStatus.unlogined) {
                return@setOnClickListener
            }
            if(abs(mSesame?.mechStatus!!.position - mSesame?.mechSetting!!.unlockPosition) < 50){
                toastMSG(getString(R.string.too_close))
                return@setOnClickListener
            }
            mSesame?.configureLockPosition(mSesame?.mechStatus!!.position, mSesame?.mechSetting!!.unlockPosition){}
        }

        mSesame?.delegate = object : CHSesame2Delegate {
            override fun onMechStatusChanged(device: CHSesame2, status: CHSesame2MechStatus, intention: CHSesame2Intention) {
                super.onMechStatusChanged(device, status, intention)
                ssmView?.setLock(device)
            }
        }


    }//end view created


}

