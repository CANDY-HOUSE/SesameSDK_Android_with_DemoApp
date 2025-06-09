package candyhouse.sesameos.ir.domain.bizAdapter.bizBase

import android.content.Context
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.handleBase.HandlerConfigAdapter
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.uiBase.UIConfigAdapter

/**
 * RemoteAdapterFactory
 * 适配器工厂，用于创建远程适配器，如空调、电视等，由此实现对应的配置和操作，达到后续不停扩展新设备时不修改旧有设备的目的
 * 开闭原则，新种设备适配时，对修改关闭，对扩展开放
 */

interface RemoteAdapterFactory {
    // 创建界面配置适配器
    fun createUIConfigAdapter(context: Context): UIConfigAdapter
    // 创建业务处理适配器
    fun createHandlerConfigAdapter(context: Context, uiConfigAdapter: UIConfigAdapter): HandlerConfigAdapter
}