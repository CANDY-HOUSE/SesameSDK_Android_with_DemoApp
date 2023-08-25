SesameOs3 固件升级
=======================
1. 确认APP与设备正常连接
2. 固件路径 [app/src/main/res/raw](../../app/src/main/res/raw)，
固件位置如下图 :
   ![示例图片](firmware.png)
3. 固件对象 **CHProductModel**
4. 选用框架 [Dfu](https://github.com/NordicSemiconductor/Android-DFU-Library)
5. 示例代码位置[BaseDeviceSettingFG](../../app/src/main/java/co/candyhouse/app/base)
  ,获取deviceAddress传入DfuServiceInitiator，示例如下:
```
val starter = DfuServiceInitiator(it.data.address)
starter.setZip(targetDevice.getFirZip())
starter.setPacketsReceiptNotificationsEnabled(true)
starter.setPrepareDataObjectDelay(400)
starter.setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true)
starter.setDisableNotification(false)
starter.setForeground(false)
starter.start(requireActivity(), DfuService::class.java)
```

