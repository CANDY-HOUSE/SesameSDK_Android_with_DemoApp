package co.candyhouse.app.tabs.devices.ssmtouchpro.setting

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.sesame.open.device.CHSesameConnector
import co.candyhouse.sesame.open.device.CHSesameTouchPro
import co.candyhouse.sesame.open.device.CHSesameTouchProDelegate
import co.utils.L
import co.utils.SharedPreferencesUtils
import co.utils.alerts.ext.inputTextAlert
import co.utils.alertview.AlertView
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertStyle
import co.utils.alertview.objects.AlertAction
import co.utils.noHashtoUUID
import co.utils.recycle.GenericAdapter
import kotlinx.android.synthetic.main.fg_ssm_tp_card_list.*

data class SuiCard(var id: String, var name: String, val cardType: Byte)

fun SuiCard.setCardType(level: Int) {
    SharedPreferencesUtils.preferences.edit().putInt(this.id, level).apply()
}

fun SuiCard.getCardType(level: Int): Int {
    return SharedPreferencesUtils.preferences.getInt(this.id, level)
}

class SesameKeyboardCards : BaseDeviceFG(R.layout.fg_ssm_tp_card_list), CHSesameTouchProDelegate {
    var mCardList = ArrayList<SuiCard>()
    override fun onDestroyView() {
        super.onDestroyView()
        (mDeviceModel.ssmLockLiveData.value as CHSesameTouchPro).cardModeSet(0) {}
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        leaderboard_list.setEmptyView(empty_view)
        swiperefresh.isEnabled = false

        img_mode_add.setOnClickListener {
            (mDeviceModel.ssmLockLiveData.value as CHSesameTouchPro).cardModeSet(0) {
                it.onSuccess {
                    view.post {
                        img_mode_add.visibility = View.GONE
                        img_mode_verify.visibility = View.VISIBLE
                    }
                }
            }
        }
        img_mode_verify.setOnClickListener {
            (mDeviceModel.ssmLockLiveData.value as CHSesameTouchPro).cardModeSet(1) {
                it.onSuccess {
                    view.post {
                        img_mode_verify.visibility = View.GONE
                        img_mode_add.visibility = View.VISIBLE
                    }
                }
            }
        }
        (mDeviceModel.ssmLockLiveData.value as CHSesameTouchPro).cardModeGet {
            it.onSuccess {
                if (it.data.toInt() == 0) {
                    view.post {
                        img_mode_verify.visibility = View.VISIBLE
                        img_mode_add.visibility = View.GONE
                    }
                } else {
                    view.post {
                        img_mode_add.visibility = View.VISIBLE
                        img_mode_verify.visibility = View.GONE
                    }
                }

            }
        }
        (mDeviceModel.ssmLockLiveData.value as CHSesameTouchPro).cards {}
        mDeviceModel.ssmosLockDelegates[(mDeviceModel.ssmLockLiveData.value!! as CHSesameTouchPro)] = object : CHSesameTouchProDelegate {

            override fun onCardReceive(device: CHSesameConnector, ID: String, name: String, type: Byte) {
                mCardList.add(0, SuiCard(ID, name, type))
                leaderboard_list?.adapter?.notifyDataSetChanged()
                menu_title.text = "${mCardList.size}/100"
            }

            override fun onCardReceiveEnd(device: CHSesameConnector) {
                leaderboard_list?.adapter?.notifyDataSetChanged()
                menu_title.text = "${mCardList.size}/100"
                swiperefresh?.isRefreshing = false
            }

            override fun onCardReceiveStart(device: CHSesameConnector) {
                mCardList.clear()
                swiperefresh?.isRefreshing = true
            }

            override fun onCardChanged(device: CHSesameConnector, ID: String, name: String, type: Byte) {
                mCardList.add(0, SuiCard(ID, name, type))
                leaderboard_list?.adapter?.notifyDataSetChanged()
                menu_title.text = "${mCardList.size}/100"

            }
        }

        leaderboard_list.adapter = object : GenericAdapter<SuiCard>(mCardList) {
            override fun getLayoutId(position: Int, obj: SuiCard): Int = R.layout.cell_suica
            override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder =
                object : RecyclerView.ViewHolder(view), Binder<SuiCard> {
                    override fun bind(data: SuiCard, pos: Int) {
                        val image = itemView.findViewById<ImageView>(R.id.image)
                        val title = itemView.findViewById<TextView>(R.id.title)
                        val name = itemView.findViewById<TextView>(R.id.sub_title)
                        val uuidStr = data.id.padEnd(32, 'F').noHashtoUUID().toString()



                        val cardType = data.getCardType(data.cardType.toInt())
                        image.setImageResource(when (cardType) {
                            1 -> R.drawable.suica
                            2 -> R.drawable.pasmo
                            else -> R.drawable.small_icon
                        })
                        image.setOnClickListener {
                            data.setCardType((cardType + 1) % 3)
                            notifyDataSetChanged()
                        }
//                        L.d("hcia", "cardtype:" + cardtype)
                        title.text = uuidStr
                        name.text = if (data.name == "") getString(R.string.default_card_name) else data.name
                        itemView.setOnClickListener {
                            AlertView(title.text.toString(), "", AlertStyle.IOS).apply {
                                addAction(AlertAction(getString(R.string.TouchProCardModify), AlertActionStyle.DEFAULT) { action ->
                                    view.context?.inputTextAlert("", name.text.toString(), data.name) {
                                        confirmButtonWithText("OK") { alert, name ->
                                            dismiss()
                                            mCardList.remove(data)
                                            (mDeviceModel.ssmLockLiveData.value as CHSesameTouchPro).cardChange(data.id, name) {}
                                        }
                                        cancelButton(getString(R.string.cancel))
                                    }?.show()
                                })
                                addAction(AlertAction(getString(R.string.TouchProCardDelete), AlertActionStyle.NEGATIVE) { action ->
                                    (mDeviceModel.ssmLockLiveData.value as CHSesameTouchPro).cardDelete(data.id) {
                                        view.post {
                                            mCardList.remove(data)
                                            leaderboard_list.adapter?.notifyDataSetChanged()
                                            menu_title.text = "${mCardList.size}/100"
                                        }
                                    }
                                })
                                show(activity as AppCompatActivity)
                            }
                        }
                    }
                }
        }
    }
}
