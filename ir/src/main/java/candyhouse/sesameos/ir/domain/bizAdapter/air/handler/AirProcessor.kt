package candyhouse.sesameos.ir.domain.bizAdapter.air.handler

import android.content.Context
import candyhouse.sesameos.ir.R
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.IrInterface
import candyhouse.sesameos.ir.domain.bizAdapter.bizBase.IRType
import co.candyhouse.sesame.utils.L
import com.google.gson.Gson

/**
 * 空调遥控器 指令处理
 */
class AirProcessor : IrInterface {
    var mPower: Int = 0x00
    var mTemperature: Int = 25
    var mWindRate: Int = 0x01 // 01 02 03 04
    var mWindDirection: Int = 0x02 // 01 02 03
    var mAutomaticWindDirection: Int = 0x01 // 00
    var mMode: Int = 0x01 // 0x01 ~ 0x05
    var mSleep: Int = 0x00
    var mHeat: Int = 0x00
    var mLight: Int = 0x01
    var mEco: Int = 0x00

    //	public int mCool = 0x01;
    //	public int mHot = 0x01;
    //	public int mMute = 0x00;
    //	public int mStrong = 0x00;
    //	public int mXx = 0x01;
    var mKey: Int = 0x01
    var mState: Int = 0x00
    var arcTable: MutableList<Array<UInt>> = mutableListOf()

    fun setupTable(context: Context) {
        val jsonString = context.resources.openRawResource(R.raw.air_table)
            .bufferedReader()
            .use { it.readText() }
        val intArray = Gson().fromJson(jsonString, Array<Array<Int>>::class.java)
        arcTable.addAll(
            intArray.map { row ->
                row.map { it.toUInt() }.toTypedArray()
            }.toTypedArray()
        )
    }

    fun GetTemp(): Byte {
        L.d("AIR", "GetTemp: $mTemperature")
        return mTemperature.toByte()
    }

    fun SetTemp(temp: Byte) {
        mTemperature = temp.toInt()
    }

    fun GetWindRate(): Byte {
        return mWindRate.toByte()
    }

    fun SetWindRate(rate: Byte) {
        mWindRate = rate.toInt()
    }

    fun GetWindDir(): Byte {
        return mWindDirection.toByte()
    }

    fun SetWindDir(dir: Byte) {
        mWindDirection = dir.toInt()
    }

    fun GetAutoWindDir(): Byte {
        return mAutomaticWindDirection.toByte()
    }

    fun SetAutoWindDir(dir: Byte) {
        mAutomaticWindDirection = dir.toInt()
    }

    fun GetMode(): Byte {
        return mMode.toByte()
    }

    fun SetMode(mode: Byte) {
        mMode = mode.toInt()
    }

    fun GetPower(): Byte {
        return mPower.toByte()
    }

    fun SetPower(power: Byte) {
        mPower = power.toInt()
    }

    fun GetSleep(): Byte {
        return mSleep.toByte()
    }

    fun SetSleep(sleep: Byte) {
        mSleep = sleep.toInt()
    }

    fun GetHeat(): Byte {
        return mHeat.toByte()
    }

    fun SetHeat(heat: Byte) {
        mHeat = heat.toInt()
    }

    fun GetLight(): Byte {
        return mLight.toByte()
    }

    fun SetLight(light: Byte) {
        mLight = light.toInt()
    }

    fun GetEco(): Byte {
        return mEco.toByte()
    }

    fun SetEco(eco: Byte) {
        mEco = eco.toInt()
    }


    //	public byte GetCool() {
    //		return (byte) mCool;
    //	}
    //	public void SetCool(byte cool) {
    //		mCool = cool;
    //	}
    //	public byte GetHot() {
    //		return (byte) mHot;
    //	}
    //	public void SetHot(byte hot) {
    //		mHot = hot;
    //	}
    //	public byte GetMute() {
    //		return (byte) mMute;
    //	}
    //	public void SetMute(byte mute) {
    //		mMute = mute;
    //	}
    //	public byte GetStrong() {
    //		return (byte) mStrong;
    //	}
    //	public void SetStrong(byte strong) {
    //		mStrong = strong;
    //	}
    //	public byte GetXx() {
    //		return (byte) mXx;
    //	}
    //	public void SetXx(byte xx) {
    //		mXx = xx;
    //	}
    fun SetState(state: Int) {
        mState = state
    }

    fun GetState(): Byte {
        return mState.toByte()
    }


    @Throws(Exception::class)
    override fun findType(typeIndex: Int, key: Int): ByteArray {
        return IrAirTool.searchKeyData(IRType.DEVICE_REMOTE_AIR, typeIndex,arcTable).toByteArray()
    }

    @Throws(Exception::class)
    override fun findBrand(brandIndex: Int, key: Int): ByteArray {
        return IrAirTool.searchKeyData(IRType.DEVICE_REMOTE_AIR, brandIndex,arcTable).toByteArray()
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Throws(Exception::class)
    override fun search(arrayIndex: Int): UByteArray {
        var i = 0
        var ck = 0
//        SetKey(key)
        val buf: UByteArray = IrAirTool.searchKeyData(IRType.DEVICE_REMOTE_AIR, arrayIndex,arcTable)
        buf[4] = GetTemp().toUByte() //(byte) AirData.mTemperature;
        L.d("search", "buf 4: ${buf.toUByteArray().toHexString()}")
        buf[5] = GetWindRate().toUByte() //(byte) AirData.mWindRate;
        L.d("search", "buf 5: ${buf.toUByteArray().toHexString()}")
        buf[6] = GetWindDir().toUByte() //(byte) AirData.mWindDirection;
        L.d("search", "buf 6: ${buf.toUByteArray().toHexString()}")
        buf[7] = GetAutoWindDir().toUByte() //(byte) AirData.mAutomaticWindDirection;
        L.d("search", "buf 7: ${buf.toUByteArray().toHexString()}")
        buf[8] = GetPower().toUByte() //(byte) AirData.mPower;
        L.d("search", "buf 8: ${buf.toUByteArray().toHexString()}")
        buf[9] = mKey.toByte().toUByte() //(byte) AirData.mKey;
        L.d("search", "buf 9: ${buf.toUByteArray().toHexString()}")
        buf[10] = GetMode().toUByte() //(byte) AirData.mMode;
        L.d("search", "buf A: ${buf.toUByteArray().toHexString()}")
//        buf[11] = GetSleep() //(byte) AirData.mSleep;
//        buf[12] = GetHeat() //(byte) AirData.mHeat;
//        buf[13] = GetLight() //(byte) AirData.mLight;
//        buf[14] = GetEco() //(byte) AirData.mEco;
        //buf[15] = GetCool();//(byte) AirData.mCool;
        //buf[16] = GetHot();//(byte) AirData.mHot;
        //buf[17] = GetMute();//(byte) AirData.mMute;
        //buf[18] = GetStrong();//(byte) AirData.mStrong;
        //buf[19] = GetXx();//(byte) AirData.mXx;
        // 设置倒数第二个字节为 0xFF
        buf[buf.size - 2] = 0xFF.toByte().toUByte()
        val checkSum = buf.dropLast(1).sumOf { it.toInt() }

        buf[buf.size - 1] = checkSum.toByte().toUByte()
        L.d("search", "buf B: ${buf.toUByteArray().toHexString()}")
        return buf
    }

    override fun getStudyData(data: ByteArray, len: Int): ByteArray {
        return IrAirTool.studyKeyCode(data, len)
    }

    override fun getBrandArray(brandIndex: Int): IntArray {
        return IrAirTool.getBrandArray(IRType.DEVICE_REMOTE_AIR, brandIndex)
    }

    override fun getTypeArray(typeIndex: Int): IntArray {
        return IrAirTool.getTypeArray(IRType.DEVICE_REMOTE_AIR, typeIndex)
    }

    override fun getTypeCount(typeIndex: Int): Int {
        return IrAirTool.getTypeCount(IRType.DEVICE_REMOTE_AIR, typeIndex)
    }

    override fun getBrandCount(brandIndex: Int): Int {
        return IrAirTool.getBrandCount(IRType.DEVICE_REMOTE_AIR, brandIndex)
    }

    override fun getTableCount(): Int {
        return IrAirTool.getTableCount(IRType.DEVICE_REMOTE_AIR)
    }

    fun buildParamsWithPower(power:Int): AirProcessor {
        this.mPower = power
        return this
    }
    fun buildParamsWithTemperature(temperature: Int): AirProcessor {
        this.mTemperature = temperature
        return this
    }
    fun buildParamsWithModel(model:Int): AirProcessor {
        this.mMode = model
        return this
    }
    fun buildParamsWithFanSpeed(windRate:Int): AirProcessor {
        this.mWindRate = windRate
        return this
    }
    fun buildParamsWithWindDirection(windDirection:Int): AirProcessor {
        this.mWindDirection = windDirection
        return this
    }
    fun buildParamsWithAutomaticWindDirection(automaticWindDirection:Int): AirProcessor {
        this.mAutomaticWindDirection = automaticWindDirection
        return this
    }

    fun parseAirData(state: String):Boolean {

        // 1. 基本格式验证
        if (state.isEmpty()) {
            L.d("","Error: Empty input")
            return false
        }

        // 3. 格式检查：确保都是有效的十六进制字符
        if (!state.matches("[0-9A-Fa-f]+".toRegex())) {
            L.d("","Error: Invalid hex characters in input")
            return false
        }

        // 1. 将十六进制字符串转换为 UByteArray
        val data = state.chunked(2).map { it.toInt(16).toUByte() }.toUByteArray()

        if (data.size <= 10){
            return false
        }
        try {
            // 2. 提取各个字段的值
            mTemperature = data[4].toInt()            // 1c -> 28
            mWindRate = data[5].toInt()               // 01 -> 1
            mWindDirection = data[6].toInt()          // 01 -> 1
            mAutomaticWindDirection = data[7].toInt() // 01 -> 1
            mPower = data[8].toInt()                 // 00 -> 0
            mMode = data[10].toInt()                 // 02 -> 2
            L.d("parseAirData", "mTemperature: $mTemperature, mWindRate: $mWindRate, mWindDirection: $mWindDirection, mAutomaticWindDirection: $mAutomaticWindDirection, mPower: $mPower, mMode: $mMode")
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

}