package co.candyhouse.app.ext

import android.content.Context
import androidx.annotation.StringRes
import co.candyhouse.app.R
import no.nordicsemi.android.dfu.DfuBaseService
import no.nordicsemi.android.dfu.DfuProgressListener
import no.nordicsemi.android.dfu.DfuServiceInitiator
import no.nordicsemi.android.dfu.DfuServiceListenerHelper
import java.lang.ref.WeakReference

/**
 * 
 *
 * @author frey on 2026/4/13
 */
object DfuCenter {

    interface Delegate {
        fun onDfuState(@StringRes resId: Int)
        fun onDfuProgress(percent: Int)
        fun onDfuError(message: String?)
    }

    sealed class StartResult {
        object Started : StartResult()
        object AlreadyRunningSameDevice : StartResult()
        object Busy : StartResult()
    }

    private sealed class Snapshot {
        data class State(@StringRes val resId: Int) : Snapshot()
        data class Progress(val percent: Int) : Snapshot()
        data class Error(val message: String?) : Snapshot()
    }

    private data class Session(
        val deviceKey: String,
        val address: String,
        var delegateRef: WeakReference<Delegate>? = null,
        var lastSnapshot: Snapshot? = null,
        val listener: DfuProgressListener
    )

    private var currentSession: Session? = null

    @Synchronized
    fun attachDelegate(deviceKey: String, delegate: Delegate) {
        val session = currentSession ?: return
        if (session.deviceKey != deviceKey) return
        session.delegateRef = WeakReference(delegate)
        replay(session)
    }

    @Synchronized
    fun detachDelegate(deviceKey: String) {
        val session = currentSession ?: return
        if (session.deviceKey != deviceKey) return
        session.delegateRef = null
    }

    @Synchronized
    fun startDfu(
        context: Context,
        deviceKey: String,
        deviceAddress: String,
        firmwarePath: String,
        delegate: Delegate?,
        serviceClass: Class<out DfuBaseService>
    ): StartResult {
        val existing = currentSession
        if (existing != null) {
            return if (existing.deviceKey == deviceKey) {
                delegate?.let { existing.delegateRef = WeakReference(it) }
                replay(existing)
                StartResult.AlreadyRunningSameDevice
            } else {
                StartResult.Busy
            }
        }

        val appContext = context.applicationContext

        lateinit var session: Session

        val listener = object : DfuProgressListener {
            override fun onDeviceConnecting(deviceAddress: String) {
                updateState(session, R.string.onDeviceConnecting)
            }

            override fun onDeviceConnected(deviceAddress: String) {
                updateState(session, R.string.onDeviceConnected)
            }

            override fun onDfuProcessStarting(deviceAddress: String) {
                updateState(session, R.string.onDfuProcessStarting)
            }

            override fun onDfuProcessStarted(deviceAddress: String) {
                updateState(session, R.string.onDfuProcessStarted)
            }

            override fun onEnablingDfuMode(deviceAddress: String) {
                updateState(session, R.string.onEnablingDfuMode)
            }

            override fun onProgressChanged(
                deviceAddress: String,
                percent: Int,
                speed: Float,
                avgSpeed: Float,
                currentPart: Int,
                partsTotal: Int
            ) {
                session.lastSnapshot = Snapshot.Progress(percent)
                session.delegateRef?.get()?.onDfuProgress(percent)
            }

            override fun onFirmwareValidating(deviceAddress: String) {
                updateState(session, R.string.onFirmwareValidating)
            }

            override fun onDeviceDisconnecting(deviceAddress: String?) {
                updateState(session, R.string.onDeviceDisconnecting)
            }

            override fun onDeviceDisconnected(deviceAddress: String) {
                updateState(session, R.string.onDeviceDisconnected)
            }

            override fun onDfuCompleted(deviceAddress: String) {
                updateState(session, R.string.onDfuCompleted)
                finishSession(appContext)
            }

            override fun onDfuAborted(deviceAddress: String) {
                updateState(session, R.string.onDfuAborted)
                finishSession(appContext)
            }

            override fun onError(
                deviceAddress: String,
                error: Int,
                errorType: Int,
                message: String?
            ) {
                session.lastSnapshot = Snapshot.Error(message)
                session.delegateRef?.get()?.onDfuError(message)
                finishSession(appContext)
            }
        }

        session = Session(
            deviceKey = deviceKey,
            address = deviceAddress,
            delegateRef = delegate?.let { WeakReference(it) },
            listener = listener
        )

        currentSession = session

        DfuServiceListenerHelper.registerProgressListener(
            appContext,
            listener,
            deviceAddress
        )

        val starter = DfuServiceInitiator(deviceAddress)
        starter.setZip(firmwarePath)
        starter.setPacketsReceiptNotificationsEnabled(true)
        starter.setPrepareDataObjectDelay(400)
        starter.setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true)
        starter.setDisableNotification(false)
        starter.setForeground(false)
        starter.start(appContext, serviceClass)

        return StartResult.Started
    }

    private fun updateState(session: Session, @StringRes resId: Int) {
        session.lastSnapshot = Snapshot.State(resId)
        session.delegateRef?.get()?.onDfuState(resId)
    }

    private fun replay(session: Session) {
        val delegate = session.delegateRef?.get() ?: return
        when (val snapshot = session.lastSnapshot) {
            is Snapshot.State -> delegate.onDfuState(snapshot.resId)
            is Snapshot.Progress -> delegate.onDfuProgress(snapshot.percent)
            is Snapshot.Error -> delegate.onDfuError(snapshot.message)
            null -> {}
        }
    }

    @Synchronized
    private fun finishSession(context: Context) {
        val session = currentSession ?: return
        DfuServiceListenerHelper.unregisterProgressListener(context, session.listener)
        currentSession = null
    }
}