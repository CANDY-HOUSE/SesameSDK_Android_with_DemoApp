package co.candyhouse.app.tabs.devices.ssmbike.setting

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.databinding.FgSsmTpCardListBinding
import co.candyhouse.sesame.open.device.Bot2Action
import co.candyhouse.sesame.open.device.CHDeviceLoginStatus
import co.candyhouse.sesame.open.device.CHSesameBot2
import co.utils.SharedPreferencesUtils
import co.utils.recycle.GenericAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.weigan.loopview.LoopView
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

class MyItemTouchHelperCallback(val adapter: GenericAdapter<ClickScript>) : ItemTouchHelper.Callback() {
    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN  // 支持向上和向下的拖動
        val swipeFlags = ItemTouchHelper.LEFT  // 支持向左滑動刪除
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        adapter.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        adapter.removeItem(viewHolder.adapterPosition) // 當項目向左滑動時，從數據集中刪除該項目
    }
}

data class ClickScript(var id: String, var actionType: Int, var goTime: Int)

class SesameClickScriptFG : BaseDeviceFG<FgSsmTpCardListBinding>() {
    private var ScriptReceive: Boolean = false
    private var ScriptNameListReceive: Boolean = false
    override fun getViewBinder() = FgSsmTpCardListBinding.inflate(layoutInflater)

    var mScriptList = ArrayList<ClickScript>()
    private var mReceivedName: ByteArray? = null
    private var mReceivedNameLen: Int = 0
    private val bot2 by lazy {
        mDeviceModel.ssmLockLiveData.value as CHSesameBot2
    }
    private val bot2ScriptCurIndexKey by lazy {
        bot2.deviceId.toString() + "_ScriptIndex"
    }

    private fun getIndex(): UByte {
        return SharedPreferencesUtils.preferences.getInt(bot2ScriptCurIndexKey, 0).toUByte()
    }

    private fun sendMotorActions() {
        if (!isGetScript.get()){
            return
        }
        val totalLength = mScriptList.size * 2 + 1
        val command = ByteArray(totalLength)
        command[0] = mScriptList.size.toByte()

        var index = 1
        for (script in mScriptList) {
            // L.d("hcia", "sendMotorActions:" + script.actionType + " " + script.goTime)
            command[index++] = script.actionType.toByte()
            command[index++] = script.goTime.toByte()
        }

        // L.d("hcia", "command:" + command.toHexString())
        val nameLength = (mReceivedNameLen.coerceAtLeast(0) ?: 0).toByte() // 確保 scriptNameLen 不為負數
        val scriptNamePart = if (mReceivedName != null) {
            val actualNameLength = minOf(mReceivedName!!.size, 20)
            mReceivedName!!.copyOf(20).also {
                if (actualNameLength < 20) {
                    it.fill(0, actualNameLength, 20)
                }
            }
        } else {
            ByteArray(20) // 若 scriptName 為 null，填充 20 個 0
        }
        val combinedData = ByteArray(nameLength + scriptNamePart.size + command.size).apply {
            this[0] = nameLength
            System.arraycopy(scriptNamePart, 0, this, 1, 20)
            System.arraycopy(command, 0, this, 20 + nameLength, command.size)
        }
        bot2.sendClickScript(getIndex(), combinedData) {}
    }

    override fun onStop() {
        super.onStop()
        sendMotorActions()
    }

    private var scriptIndex = ""
    fun setMenuText(size: Int) {
        bind.menuTitle.post {
            try {
                bind.menuTitle.text = "${scriptIndex}(${size}/20)"
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
        }
    }

    private fun handleData(nameLen: Int, name: ByteArray, scriptCount: Int = 0, actions: List<Bot2Action>) {
        if (isAdded){
            view?.post {
                mReceivedNameLen = nameLen
                mReceivedName = name
                scriptIndex = String(name, Charsets.UTF_8)
                mScriptList.clear()
                try {
                    actions.forEachIndexed { index, bot2Action ->
                        val adjustedGoTime = bot2Action.time  // 转换为无符号
                        mScriptList.add(ClickScript(index.toString(), bot2Action.action.value.toInt(), adjustedGoTime.toInt()))
                    }
                    setMenuText(mScriptList.size)
                    bind.leaderboardList.adapter?.notifyDataSetChanged()
                    ScriptReceive = true
                    ScriptNameListReceive = true
                    bind.imgModeVerify.visibility = if (ScriptReceive && ScriptNameListReceive) View.VISIBLE else View.GONE
                    isGetScript.set(true)
                }catch (e:NullPointerException){
                    e.printStackTrace()
                }catch (e:Exception){
                    e.printStackTrace()
                }

            }
        }
    }
    private var isGetScript=AtomicBoolean(false)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)
        //   bind.leaderboardList.setEmptyView(empty_view)
        bind.emptyView.text = getString(R.string.clickaction)
        bind.swiperefresh.isEnabled = false
        bot2.getCurrentScript(getIndex()) {
            it.onSuccess { v ->
                activity?.runOnUiThread {
                    handleData(v.data.nameLength.toInt(), v.data.name, (v.data.actionLength?: 0u).toInt(), v.data.actions?: arrayListOf())
                }
            }
        }

        bind.imgModeVerify.visibility = if (ScriptReceive && ScriptNameListReceive) View.VISIBLE else View.GONE
        var scriptCount = 0
        bind.imgModeVerify.setOnClickListener {
            // L.d("hcia", "img_mode_verify.setOnClickListener")
            if (mScriptList.size < 20) {    // 最多20個動作
                mScriptList.add(ClickScript(scriptCount.toString(), 0, 0))
                // bind.menuTitle.text = "${mScriptList.size}/20"
                setMenuText(mScriptList.size)
                bind.leaderboardList.adapter?.notifyDataSetChanged()
                scriptCount++
            }
        }

        bind.leaderboardList.adapter = object : GenericAdapter<ClickScript>(mScriptList) {
            override fun getLayoutId(position: Int, obj: ClickScript): Int = R.layout.cell_suica
            override fun onItemMove(fromPosition: Int, toPosition: Int) {
                Collections.swap(mScriptList, fromPosition, toPosition)
                notifyItemMoved(fromPosition, toPosition)
            }

            override fun removeItem(position: Int) {
                mScriptList.removeAt(position)
                notifyItemRemoved(position)
                setMenuText(mScriptList.size)
                // bind.menuTitle.text = "${mScriptList.size}/20"
            }


            override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder =
                    object : RecyclerView.ViewHolder(view), Binder<ClickScript> {
                        override fun bind(data: ClickScript, pos: Int) {
                            val image = itemView.findViewById<ImageView>(R.id.image)
                            val goTime = itemView.findViewById<TextView>(R.id.sub_title)
                            val imgDel = itemView.findViewById<ImageView>(R.id.imgDel)
                            imgDel.visibility = View.VISIBLE
                            imgDel.setOnClickListener {
                                if (pos >= 0 && pos < mScriptList.size) {
                                    mScriptList.removeAt(pos)
                                    notifyItemRemoved(pos)
                                    notifyItemRangeChanged(pos, mScriptList.size)
                                    setMenuText(mScriptList.size)
                                }
                            }
                            val actionType = data.actionType
                            // L.d("hcia", "actionType:" + actionType)
                            image.setImageResource(when (actionType) {
                                0 -> R.drawable.click_forward   // 0
                                1 -> R.drawable.click_reverse   // 1
                                2 -> R.drawable.click_stop   // 2
                                else -> R.drawable.click_sleep  // 3
                            })
                            image.setOnClickListener {
                                data.actionType = (actionType + 1) % 4
                                notifyDataSetChanged()
                            }

                            /* val displayTexts = arrayOf("1秒", "2秒", "3秒", "4秒", "5秒", "永遠")
                            val timeValues = arrayOf(1, 2, 3, 4, 5, 255) */
                            val displayTexts = (1..254).map {
                                val value = it / 10.0
                                "$value ${getString(R.string.second2)}"

                            }.toMutableList().apply { add(getString(R.string.forever)) }.toTypedArray()
                            val timeValues = (1..254).toMutableList().apply { add(255) }.toTypedArray()

                            goTime.text = timeValues.indexOf(data.goTime).takeIf { it >= 0 }
                                ?.let { displayTexts[it] }
                                ?: ("0" + getString(R.string.second2))  // 0秒時顯示請選擇時間，輸入數值有在陣列裡顯示文字，否則顯示"請選擇時間"
                            itemView.setOnClickListener {

                                showWheelPickerDialog2(goTime, data)
                            }
                        }
                    }
        }
        val callback = MyItemTouchHelperCallback(bind.leaderboardList.adapter as GenericAdapter<ClickScript>)
        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(bind.leaderboardList)
    }


    fun showWheelPickerDialog2(textView: TextView, clickScript: ClickScript) {
        // val dialog = Dialog(requireContext())
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = bottomSheetDialog.layoutInflater.inflate(R.layout.dialog_wheel_picker, null)
        bottomSheetDialog.setContentView(view)

        val wheelPicker: LoopView = view.findViewById(R.id.wheelview)

        val displayTexts = (1..254).map {
            val value = it / 10.0
            "$value ${getString(R.string.second2)}"
        }.toMutableList().apply { add(getString(R.string.forever)) }
        val timeValues = (1..254).toMutableList().apply { add(255) } // 如果需要

        wheelPicker.setItems(displayTexts)
        wheelPicker.setInitPosition(0)
        if (clickScript.goTime === 0) {
            textView.text = displayTexts[0]
            clickScript.goTime = timeValues[0]
        }
        val curPosition = clickScript.goTime - 1
        wheelPicker.setCurrentPosition(curPosition)
        wheelPicker.setListener { select ->
            textView.text = displayTexts[select]
            clickScript.goTime = timeValues[select]
        }
        bottomSheetDialog.show()
    }

    fun listItem(): List<String> {
        return (0..254).map { it.toString() + getString(R.string.second2) }
    }
}
