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

import android.app.IntentService
import android.content.Intent
import co.candyhouse.app.tabs.devices.ssm2.getIsWidget
import co.candyhouse.sesame.open.CHBleManager
import co.candyhouse.sesame.open.CHDeviceManager
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHSesame2
import co.candyhouse.sesame.open.device.CHSesame5
import co.candyhouse.sesame.open.device.CHSesameBike
import co.candyhouse.sesame.open.device.CHSesameBike2
import co.candyhouse.sesame.open.device.CHSesameBot
import co.candyhouse.sesame.open.device.CHSesameBot2
import co.candyhouse.sesame.open.device.CHSesameLock
import co.candyhouse.sesame.utils.L
import co.utils.UserUtils
import org.checkerframework.checker.units.qual.s

class MessagingIntentService : IntentService("MyService") {

    private fun actionOpenAll(device: CHDevices, open: Boolean) {
        var isPerforme = false
        when (device) {
            is CHSesame5 -> {
                if (open) {
                    device.lock(historytag = UserUtils.getUserIdWithByte()) { }
                } else {
                    device.unlock(historytag = UserUtils.getUserIdWithByte()) { }
                }
                isPerforme = true
            }
            is CHSesame2 -> {
                if (open) {
                    device.lock() { }
                } else {
                    device.unlock() { }
                }
                isPerforme = true
            }
            is CHSesameBot -> {
                device.click { }
                isPerforme = true
            }
            is CHSesameBot2 -> {
                device.click { }
                isPerforme = true
            }
            is CHSesameBike -> {
                if (!open) {
                    device.unlock { }
                    isPerforme = true
                }
            }
            is CHSesameBike2 -> {
                if (!open) {
                    device.unlock(historytag = UserUtils.getUserIdWithByte()) { }
                    isPerforme = true
                }
            }
        }
        if (isPerforme) {
            L.d("onHandleIntent", device.deviceId.toString() + "--睡觉-")

            try {
                Thread.sleep(500)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        CHDeviceManager.getCandyDevices {
            it.onSuccess {
                it.data.forEach { device ->
                    L.d(
                        "onHandleIntent",
                        device.deviceId.toString() + "---" + it.data.size + "-----" + device.getIsWidget() + "---" + intent?.action
                    )
                    when (device) {
                        is CHSesameLock -> {
                            if (device.getIsWidget()) {
                                if (device.deviceShadowStatus == null) {
                                    CHBleManager.enableScan(true) { }
                                }
                                if (intent?.action?.contains("open_all") == true) {
                                    actionOpenAll(device, true)
                                } else if (intent?.action?.contains("close_all") == true) {
                                    actionOpenAll(device, false)
                                } else if (intent?.action == "toggle_ssm" + device.deviceId.hashCode()) {
                                    (device as? CHSesame5)?.toggle(historytag = UserUtils.getUserIdWithByte()) { }
                                    (device as? CHSesame2)?.toggle() { }
                                    (device as? CHSesameBike)?.unlock { }
                                    (device as? CHSesameBike2)?.unlock(historytag = UserUtils.getUserIdWithByte()) { }
                                    (device as? CHSesameBot)?.click { }
                                    (device as? CHSesameBot2)?.click { }
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}
