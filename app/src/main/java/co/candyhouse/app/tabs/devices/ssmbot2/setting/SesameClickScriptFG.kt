package co.candyhouse.app.tabs.devices.ssmbot2.setting

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.app.databinding.FgSsmTpCardListBinding
import co.candyhouse.app.ext.BotScriptStore
import co.candyhouse.sesame.open.devices.Bot2Action
import co.candyhouse.sesame.open.devices.CHSesameBot2
import co.candyhouse.sesame.server.CHAPIClientBiz
import co.candyhouse.sesame.server.dto.BotScriptRequest
import co.candyhouse.sesame.utils.L
import co.candyhouse.sesame.utils.SharedPreferencesUtils
import co.candyhouse.sesame.utils.toHexString
import co.utils.alerts.ext.inputTextAlert
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
    private val tag = "SesameClickScriptFG"

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
        if (!isGetScript.get()) {
            return
        }
        val totalLength = mScriptList.size * 2 + 1
        val command = ByteArray(totalLength)
        command[0] = mScriptList.size.toByte()

        var index = 1
        for (script in mScriptList) {
            command[index++] = script.actionType.toByte()
            command[index++] = script.goTime.toByte()
        }

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

        val idx = SharedPreferencesUtils.preferences.getInt(bot2ScriptCurIndexKey, 0)
        if (aliasDirty) {
            BotScriptStore.putAlias(bot2.deviceId.toString(), idx, editedAlias.orEmpty())
        }

        val request = BotScriptRequest(
            deviceUUID = bot2.deviceId.toString().uppercase(),
            actionIndex = idx.toString(),
            alias = if (aliasDirty) (editedAlias ?: "") else null,
            isDefault = 1,
            actionData = combinedData.toHexString(),
            displayOrder = BotScriptStore.getDisplayOrder(bot2.deviceId.toString(), idx) ?: idx
        )

        CHAPIClientBiz.updateBotScript(request) { result ->
            result.onSuccess {
                L.d(tag, "upsertBotScriptAlias success")
                aliasDirty = false
            }
            result.onFailure { error ->
                L.e(tag, "upsertBotScriptAlias failed", error)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        sendMotorActions()
    }

    private var scriptIndex = ""
    private var editedAlias: String? = null
    private var aliasDirty: Boolean = false
    @SuppressLint("SetTextI18n")
    fun setMenuText(size: Int) {
        bind.menuTitle.post {
            try {
                val title = editedAlias ?: scriptIndex
                bind.menuTitle.text = "${title}\uD83D\uDD8A\uFE0F(${size}/20)"
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }
        }
    }

    private fun handleData(nameLen: Int, name: ByteArray, scriptCount: Int = 0, actions: List<Bot2Action>) {
        if (isAdded) {
            view?.post {
                mReceivedNameLen = nameLen
                mReceivedName = name
                scriptIndex = String(name, Charsets.UTF_8)
                val idx = SharedPreferencesUtils.preferences.getInt(bot2ScriptCurIndexKey, 0)
                BotScriptStore.getAlias(bot2.deviceId.toString(), idx)?.let { alias ->
                    editedAlias = alias
                    aliasDirty = false
                }
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
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private var isGetScript = AtomicBoolean(false)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)
        bind.emptyView.text = getString(R.string.clickaction)
        bind.swiperefresh.isEnabled = false
        bot2.getCurrentScript(getIndex()) {
            it.onSuccess { v ->
                activity?.runOnUiThread {
                    handleData(v.data.nameLength.toInt(), v.data.name, (v.data.actionLength ?: 0u).toInt(), v.data.actions ?: arrayListOf())
                }
            }
        }

        bind.imgModeVerify.visibility = if (ScriptReceive && ScriptNameListReceive) View.VISIBLE else View.GONE
        var scriptCount = 0
        bind.imgModeVerify.setOnClickListener {
            if (mScriptList.size < 20) {    // 最多20個動作
                mScriptList.add(ClickScript(scriptCount.toString(), 0, 0))
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
                        image.setImageResource(
                            when (actionType) {
                                0 -> R.drawable.click_forward   // 0
                                1 -> R.drawable.click_reverse   // 1
                                2 -> R.drawable.click_stop   // 2
                                else -> R.drawable.click_sleep  // 3
                            }
                        )
                        image.setOnClickListener {
                            data.actionType = (actionType + 1) % 4
                            notifyDataSetChanged()
                        }

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

        bind.menuTitle.setOnClickListener {
            val current = editedAlias ?: scriptIndex
            showBotAliasRenameDialog(stripBotAliasPrefix(current))
        }

        val callback = MyItemTouchHelperCallback(bind.leaderboardList.adapter as GenericAdapter<ClickScript>)
        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(bind.leaderboardList)
    }

    fun showWheelPickerDialog2(textView: TextView, clickScript: ClickScript) {
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

    private fun showBotAliasRenameDialog(currentName: String) {
        context?.inputTextAlert("", currentName, "") {
            confirmButtonWithText("OK") { _, newAlias ->
                editedAlias = normalizeBotAlias(newAlias)
                aliasDirty = true
                setMenuText(mScriptList.size)
                dismiss()
            }
            cancelButton(getString(R.string.cancel))
        }?.show()
    }

    private fun normalizeBotAlias(input: String): String {
        val trimmed = input.trim()
        val pure = if (trimmed.startsWith("🎬")) {
            trimmed.removePrefix("🎬").trim()
        } else {
            trimmed
        }
        return "🎬 $pure"
    }

    private fun stripBotAliasPrefix(input: String): String {
        return input.removePrefix("🎬").trim()
    }
}