package co.receiver.widget

import android.app.Service
import android.content.Intent
import android.location.Location
import androidx.core.app.NotificationManagerCompat
import co.candyhouse.app.tabs.devices.ssm2.*
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.device.*
import co.utils.L
import co.utils.getLastKnownLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SesameForegroundService : Service() {

    companion object {
        var isLive: Boolean = false
    }

    override fun onCreate() {
        super.onCreate()
        isLive = true
        CoroutineScope(IO).launch {
            while (isLive) {
                delay(1 * 1000)
//                L.d("hcia", "[GPS] isLive:" + isLive)
                CHDeviceManager.getCandyDevices {
                    it.onSuccess {
                        it.data.forEach { device ->
                            if (device.getIsNOHand()) {//有設定自動開鎖
                                if (!device.getIsNOHandG() && device.deviceStatus.value == CHDeviceLoginStatus.UnLogin) {//判斷離開圈外

                                    getLastKnownLocation(baseContext.applicationContext) { locationState ->
                                        locationState.getOrNull()?.data?.let { location ->
//                                                        L.d("hcia", "location:" + location)
                                            val dist = FloatArray(1)
                                            Location.distanceBetween(device.getNOHandLeft().toDouble(), device.getNOHandRight().toDouble(), location.latitude, location.longitude, dist)
//                                                    L.d("hcia", device.getNickname() + " 離ssm的距離:" + dist.firstOrNull() + "ssm 感應半徑:" + device.getNOHandRadius() + " 經:" + location.latitude.toInt() + " 緯:" + location.longitude.toInt())
                                            dist.firstOrNull()?.let { distance ->
                                                if (distance > device.getNOHandRadius()) {
//                                                            L.d("hcia", device.getNickname() + "離開圈位準備開鎖！ 離ssm的距離:" + dist.firstOrNull() + "ssm 感應半徑:" + device.getNOHandRadius() + " 經:" + location.latitude.toInt() + " 緯:" + location.longitude.toInt())
                                                    device.setIsNOHandG(true)
                                                    NotificationManagerCompat.from(baseContext.applicationContext).notify(device.deviceId.hashCode(), CHServiceManager.WigetLock(device, baseContext.applicationContext))
                                                }
                                            }
                                        }
                                    }
                                }
                                if (device.deviceStatus.value == CHDeviceLoginStatus.Login) {
                                    device.rssi?.let {
                                        if (device.getIsNOHandG()) {//遇到鎖有連線就開

                                            if (device.deviceStatus == CHDeviceStatus.Unlocked) {
                                                device.setIsNOHandG(false)
                                            } else if (device.deviceStatus != CHDeviceStatus.Unlocked) {
                                                L.d("hcia", "下開===>:")
                                                device.setIsNOHandG(false)
                                                (device as? CHSesame5)?.unlock { }
                                                (device as? CHSesame2)?.unlock { }
                                            }
                                        }
                                    }
                                }
                            }
                        }
//                L.d("hcia", "weget 變暗")
                    }
                }

            }
        }
//        L.d("hcia", "[widget] SesameForegroundService onCreate:")
    }

    override fun onDestroy() {
        super.onDestroy()
        isLive = false
//        L.d("hcia", "[widget] onDestroy isLive:" + isLive)
        CHDeviceManager.getCandyDevices {
            it.onSuccess {
                it.data.forEach {
                    when (it) {
                        is CHSesameLock -> {
                            NotificationManagerCompat.from(baseContext.applicationContext).cancel(it.deviceId.hashCode())
                        }
                    }
                }
            }
        }
        NotificationManagerCompat.from(baseContext.applicationContext).cancel("all".hashCode())
        stopForeground(true)
    }

    override fun onBind(intent: Intent) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

//        L.d("hcia", "[widget] onStartCommand ")
        CHDeviceManager.getCandyDevices {
            var count = 0
            it.onSuccess {

                it.data.forEach {
                    when (it) {
                        is CHSesameLock -> {
                            if (it.getIsWidget()) {
                                count++
                                if (count == 1) {
//                                        L.d("hcia", "我是主角 it:" + it.getNickname())
                                    startForeground(it.deviceId.hashCode(), CHServiceManager.WigetLock(it, baseContext.applicationContext))
                                }
                            }
                        }
                    }
                }


                it.data.sortedWith(compareBy({ it.uiPriority() }, { it.deviceId })).reversed().forEach {
                    when (it) {
                        is CHSesameLock -> {
                            if (it.getIsWidget()) {
//                                L.d("hcia", "打開:" + it.getNickname() + " " + it.deviceId.hashCode())
                                NotificationManagerCompat.from(baseContext.applicationContext).notify(it.deviceId.hashCode(), CHServiceManager.WigetLock(it, baseContext.applicationContext))
                            } else {
                                NotificationManagerCompat.from(baseContext.applicationContext).cancel(it.deviceId.hashCode())
                            }
                        }
                    }
                }

//                L.d("hcia", "打開: count:" + count + " all".hashCode())
                if (count > 1) {
//                    L.d("hcia", "打開: count:" + count + " all".hashCode())
                    NotificationManagerCompat.from(baseContext.applicationContext).notify("all".hashCode(), CHServiceManager.connectedNotification("all".hashCode(), baseContext.applicationContext))
                } else {
                    NotificationManagerCompat.from(baseContext.applicationContext).cancel("all".hashCode())
                }
//                    L.d("hcia", "[widget] open count:" + count)
            }
        }

        return START_STICKY
    }
}

