package co.candyhouse.app.tabs.devices.ssm2.setting.angle

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.core.app.ActivityCompat
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.tabs.MainActivity
import co.candyhouse.app.tabs.devices.ssm2.*
import co.utils.L
import co.utils.getLastKnownLocation

import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import kotlinx.android.synthetic.main.fg_no_hand.*


class SSM2NoHandLockFG : BaseDeviceFG(R.layout.fg_no_hand) {



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)




        autolock_switch.isChecked = mDeviceModel.ssmLockLiveData.value!!.getIsNOHand()
        autolock_switch.setOnCheckedChangeListener { view, isChange ->
            L.d("hcia", "isChange:" + isChange)
            mDeviceModel.ssmLockLiveData.value!!.setIsNOHand(isChange)
            mDeviceModel.updateWidgets()
        }

        range_bar.setProgress(mDeviceModel.ssmLockLiveData.value!!.getNOHandRadius())
        range_bar.onSeekChangeListener = object : OnSeekChangeListener {
            override fun onSeeking(seekParams: SeekParams) {
            //    circle?.radius = seekParams.progressFloat.toDouble()
            }

            override fun onStartTrackingTouch(seekBar: IndicatorSeekBar) {}

            override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {
            //    circle?.radius = seekBar.progress.toDouble()
                mDeviceModel.ssmLockLiveData.value!!.setNOHandRadius(seekBar.progress.toFloat())
            }
        }
    }


}

