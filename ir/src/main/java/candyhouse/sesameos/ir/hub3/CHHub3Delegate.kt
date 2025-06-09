package candyhouse.sesameos.ir.hub3

import candyhouse.sesameos.ir.ext.CHHub3IRCode
import co.candyhouse.sesame.open.device.CHHub3

interface  CHHub3Delegate {
    /// IR 指令改變
    /// - Parameters:
    ///   - device: 設備對象
    ///   - id: ir id
    ///   - name: ir name
    fun onIRCodeChanged(device: CHHub3, ir: CHHub3IRCode)

    /// IR 接受
    /// - Parameters:
    ///   - device: 設備對象
    ///   - id: ir id
    ///   - name: ir name
    fun onIRCodeReceive(device: CHHub3, ir: CHHub3IRCode)

    /// IR 開始接受
    /// - Parameter device: 設備對象
    fun onIRCodeReceiveStart(device: CHHub3)

    /// IR 完成接受
    /// - Parameter device: 設備對象
    fun onIRCodeReceiveEnd(device: CHHub3)

    /// 接收到 mode 改變
    /// - Parameters:
    ///   - device: 設備對象
    ///   - mode: 模式 ，0 正常模式 1 錄入模式
    fun onIRModeReceive(device: CHHub3, mode: Byte)
}