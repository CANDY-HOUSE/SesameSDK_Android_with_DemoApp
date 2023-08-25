/*
Copyright 2016 The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package co.receiver.widget

import android.app.IntentService;
import android.content.Intent;
import co.candyhouse.app.tabs.devices.ssm2.getIsWidget
import co.candyhouse.sesame.open.CHBleManager
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.device.*


class MessagingIntentService : IntentService("MyService") {

    override fun onHandleIntent(intent: Intent?) {
//        L.d("hcia", "intent:" + intent)

        CHDeviceManager.getCandyDevices {
            it.onSuccess {
                it.data.forEach { device ->
                    when (device) {
                        is CHSesameLock -> {
                            if (device.getIsWidget()) {
                                if (device.deviceShadowStatus == null) {
                                    CHBleManager.enableScan(true) { }
                                }
                                if (intent?.action?.contains("open_all") == true) {
                                    (device as? CHSesame5)?.lock { }
                                    (device as? CHSesame2)?.lock { }
                                    (device as? CHSesameBot)?.click { }
                                } else if (intent?.action?.contains("close_all") == true) {
                                    (device as? CHSesame5)?.unlock { }
                                    (device as? CHSesame2)?.unlock { }
                                    (device as? CHSesameBike)?.unlock { }
                                    (device as? CHSesameBike2)?.unlock { }
                                    (device as? CHSesameBot)?.click { }
                                } else if (intent?.action == "toggle_ssm" + device.deviceId.hashCode()) {
                                    (device as? CHSesame5)?.toggle { }
                                    (device as? CHSesame2)?.toggle { }
                                    (device as? CHSesameBike)?.unlock { }
                                    (device as? CHSesameBike2)?.unlock { }
                                    (device as? CHSesameBot)?.click { }
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}
