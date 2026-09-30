package co.candyhouse.app.util

import co.candyhouse.sesame.ble.CHDevice
import org.json.JSONObject

fun CHDevice.toKeyJson(): JSONObject = JSONObject().put("deviceUUID", deviceUUID.uppercase())
    .put("deviceModel", deviceModel).put("keyIndex", keyIndex).put("secretKey", secretKey)
    .put("sesame2PublicKey", sesame2PublicKey)
