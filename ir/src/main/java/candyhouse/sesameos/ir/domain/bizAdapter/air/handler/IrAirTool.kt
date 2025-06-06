package candyhouse.sesameos.ir.domain.bizAdapter.air.handler

import candyhouse.sesameos.ir.base.IrRemote
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.IrInterface
import candyhouse.sesameos.ir.ext.IRDeviceType
import co.candyhouse.sesame.utils.L

object IrAirTool {

    // Builder 函数，返回 IR 类型的实例
    fun builder(type: Int): IrInterface? {
        return when (type) {
            IRDeviceType.DEVICE_REMOTE_AIR -> AirProcessor()
            else -> null
        }
    }

    fun studyKeyCode(data: ByteArray, len: Int): ByteArray {
        return byteArrayOf()
    }

    fun decimalToTwoHexInts(number: Int): ByteArray {
        val firstPart = number / (0xff + 1)
        val secondPart = number % (0xff + 1)
        return byteArrayOf(firstPart.toByte(), secondPart.toByte())
    }

/**
    static func searchKeyData(type: Int, arrayIndex: Int, key: Int) -> [UInt8]? {
        var buf = [UInt8]()
        if type == IRDeviceType.DEVICE_REMOTE_AIR {
            let MaxIndex = arcTable.count - 1
            let searchIndex = min(arrayIndex, MaxIndex)
            buf.append(contentsOf: AirPrefixCode)
            buf.append(contentsOf: decimalToTwoHexInts(arrayIndex))
            buf.append(contentsOf: Array(repeating: 0, count: 7))
            var indexsets = arcTable[searchIndex]
            indexsets[0] = indexsets[0] + 1
            buf.append(contentsOf: indexsets.map{ UInt8($0) })
            buf.append(0xff)
            buf.append(0)
        } else {
            buf.append(contentsOf: NonAirPrefixCode)
        }
        return buf
    }

  **/
    private val AirPrefixCode: ByteArray = byteArrayOf(0x30, 0x01)

    private val NonAirPrefixCode: ByteArray = byteArrayOf(0x30, 0x00)

    @OptIn(ExperimentalStdlibApi::class)
    fun searchKeyData(type: Int, arrayIndex: Int,arcTable:MutableList<Array<UInt>>): UByteArray {
        val buf = mutableListOf<UByte>()
        if (type == IRDeviceType.DEVICE_REMOTE_AIR) {
            val maxIndex = arcTable.size - 1
            val searchIndex = minOf(arrayIndex, maxIndex)
            L.d("searchKeyData", "arrayIndex: $arrayIndex, searchIndex: $searchIndex")
            buf.addAll(AirPrefixCode.map { it.toUByte() })
            L.d("searchKeyData", "buf header: ${buf.toUByteArray().toHexString()}")
            buf.addAll(decimalToTwoHexInts(arrayIndex).map { it.toUByte() })
            L.d("searchKeyData", "buf arrayIndex: ${buf.toUByteArray().toHexString()}")
            buf.addAll(UByteArray(7) { 0u })
            L.d("searchKeyData", "buf UByteArray7: ${buf.toUByteArray().toHexString()}")
            val indexsets = arcTable[searchIndex].toMutableList()

            val sb = StringBuilder()
            indexsets.forEach { sb.append(it.toUByte().toHexString()+"   ") }
            indexsets[0] = indexsets[0] + 1u
            buf.addAll(indexsets.map { it.toUByte() })
            L.d("searchKeyData", "buf indexsets: ${buf.toUByteArray().toHexString()}")
            buf.add(0xFF.toUByte())
            buf.add(0u)
            L.d("searchKeyData", "buf: ${buf.toUByteArray().toHexString()}")
        } else {
            buf.addAll(NonAirPrefixCode.map { it.toUByte() })
        }
        return buf.toUByteArray()
    }

    fun getTableCount(type: Int): Int {
        // 对应的实际实现
        return 0
    }

    fun getBrandCount(type: Int, brandIndex: Int): Int {
        return 0
    }

    fun getTypeCount(type: Int, typeIndex: Int): Int {
        return 0
    }

    fun getBrandArray(type: Int, brandIndex: Int): IntArray {
        return IntArray(0)
    }

    fun getTypeArray(type: Int, typeIndex: Int): IntArray {
        return if (type == IRDeviceType.DEVICE_REMOTE_AIR) {
            intArrayOf(typeIndex)
        } else {
            IntArray(0)
        }
    }

    /**
     * 过滤IR遥控器列表，只保留code在支持列表中的设备
     * @param remotes IR遥控器列表
     * @param airSupport 支持的设备代码二维数组
     * @return 过滤后的IR遥控器列表
     */
    fun filterIrRemotesByCode(remotes: List<IrRemote>, airSupport: Array<IntArray>): List<IrRemote> {
        val result= remotes.filter { remote ->
            // 对于每个遥控器，检查其code是否在任意一个支持列表中
            airSupport.any { array ->
                // 从数组的第二个元素开始匹配 (index = 1)
                array.slice(1 until array.size).contains(remote.code)
            }
        }
        L.d("filterIrRemotesByCode", "result: ${result.size}  remotes: ${remotes.size}")
        return result
    }

    /**
     * 过滤IR遥控器列表，只保留code在支持列表中的设备
     * @param remotes IR遥控器列表
     * @param airSupport 支持的设备代码数组
     * @return 过滤后的IR遥控器列表
     */
    fun filterIrRemotesByMode(remotes: List<IrRemote>, airSupport: Array<String>): List<IrRemote> {
        // 创建一个Map来存储每个支持型号的匹配数量
        val matchCounts = mutableMapOf<String, Int>()

        val result = remotes.filter { remote ->
            // 对于每个遥控器，检查其code是否在任意一个支持列表中
            airSupport.any { item ->
                val isMatch = if (remote.model.isNullOrEmpty()) {
                    false
                } else {
                    remote.model!!.lowercase().contains(item.lowercase()) || item.lowercase()
                        .contains(remote.model.toString().lowercase())
                }
                if (isMatch) {
                    matchCounts[item] = (matchCounts[item] ?: 0) + 1
                }
                isMatch
            }
        }

        // 打印匹配统计信息
        L.d("filterIrRemotesByMode", "总过滤结果 - 过滤后: ${result.size}, 过滤前: ${remotes.size}")
        matchCounts.forEach { (model, count) ->
            L.d("filterIrRemotesByMode", "匹配型号: $model, 匹配数量: $count")
        }

        // 打印未匹配的支持型号
        val unmatchedModels = airSupport.filter { it !in matchCounts.keys }
        if (unmatchedModels.isNotEmpty()) {
            L.d("filterIrRemotesByMode", "未匹配的支持型号: ${unmatchedModels.joinToString(", ")}")
        }

        return result
    }

}
