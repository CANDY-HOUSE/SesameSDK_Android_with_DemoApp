package co.candyhouse.app.tabs.devices.hub3.setting.ir.bean

object IROperation {
    val OPERATION_REMOTE_EMIT = "remoteEmit"
    val OPERATION_LEARN_EMIT = "learnEmit"
    val OPERATION_MODE_SET = "modeSet"
    val OPERATION_MODE_GET = "modeGet"

    val MODE_REGISTER = 0x01 // 自学习模式
    val MODE_CONTROL = 0x00  // 控制模式
}