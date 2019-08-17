package com.exp.carconnect.base.fragment

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.app.Application
import android.arch.lifecycle.*
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.crashlytics.android.answers.CustomEvent
import com.exp.carconnect.base.*
import com.exp.carconnect.base.R
import com.exp.carconnect.base.state.*
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.view_device_connection.*
import redux.api.Reducer


class DeviceConnectionView : Fragment(), BackInterceptor {
    companion object {
        const val TAG = "DeviceConnectionView"
    }

    private lateinit var deviceConnectionVM: DeviceConnectionVM
    private lateinit var trackAnimator: ObjectAnimator

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.view_device_connection, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        deviceConnectionVM = ViewModelProviders.of(this).get(DeviceConnectionVM::class.java)
        deviceConnectionVM
                .getScreenStateLiveData()
                .observe(this, Observer {
                    onNewState(it!!)
                })
    }

    override fun onStart() {
        super.onStart()
        animateTrack()
    }

    override fun interceptBack(): Boolean {
        deviceConnectionVM.onBackPressed()
        return false
    }

    private fun animateTrack() {
        trackAnimator = ObjectAnimator()
        trackAnimator.setFloatValues(0.toFloat(), 360.toFloat())
        trackAnimator.repeatCount = ObjectAnimator.INFINITE
        trackAnimator.repeatMode = ObjectAnimator.RESTART
        trackAnimator.start()
        trackAnimator.addUpdateListener {
            track.rotation = it.animatedValue as Float
        }
    }

    override fun onStop() {
        super.onStop()
        trackAnimator.cancel()

    }


    private fun onNewState(it: ConnectionScreenState) {

        when (it) {
            is ConnectionScreenState.ShowStatus -> {
                setStatus(it.status)
            }

            is ConnectionScreenState.ShowSetupError -> {
                showSetupError(it.error)
            }

        }
    }


    private fun showSetupError(error: String) {
        AlertDialog
                .Builder(activity)
                .setTitle(getString(R.string.setup_error_title))
                .setMessage(error)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { dialog, which ->
                    deviceConnectionVM.onSetupErrorAcknowledged()
                    dialog.dismiss()
                }
                .create()
                .show()
    }

    private fun setStatus(statusText: String) {
        status.text = statusText
    }


}

class DeviceConnectionVM(app: Application) : AndroidViewModel(app) {
    private val deviceConnectionViewLiveData = MutableLiveData<ConnectionScreenState>()
    private val store = (app as BaseAppContract).store
    private val storeSubscription: CompositeDisposable = CompositeDisposable()


    init {
        app.logContentViewEvent("DeviceConnectionView")
        storeSubscription.add(store
                .asCustomObservable()
                .filter { it.uiState.currentView is ConnectionScreen }
                .map { (it.uiState.currentView as ConnectionScreen).screenState }
                .distinctUntilChanged()
                .subscribe {
                    deviceConnectionViewLiveData.value = it
                    if (it is ConnectionScreenState.ShowSetupError) {
                        app.logContentViewEvent("SetupErrorDialog")
                    }
                })

        storeSubscription.add(store
                .asCustomObservable()
                .filter { it.isVehicleInfoLoaded() }
                .take(1)
                .subscribe {
                    (getApplication<Application>() as BaseAppContract)
                            .onDataLoadingStartedFor(it.getCurrentVehicleInfo())
                })
    }

    override fun onCleared() {
        storeSubscription.dispose()
    }

    fun getScreenStateLiveData(): LiveData<ConnectionScreenState> {
        return deviceConnectionViewLiveData
    }

    fun onSetupErrorAcknowledged() {
        getApplication<Application>().logEvent(CustomEvent("connection_setup_error_acknowledged"))
        store.dispatch(CommonAppAction.FinishCurrentView)
    }

    fun onBackPressed() {
        store.dispatch(BaseAppAction.StartBackgroundOrKillActiveSession)
    }

}


class DeviceConnectionScreenStateReducer(val context: Context) : Reducer<AppState> {
    override fun reduce(state: AppState, action: Any): AppState {
        return when (action) {
            is BaseAppAction.DeviceConnectionFailed -> {
                updateState(state, ConnectionScreenState
                        .ShowSetupError(context.getString(R.string.connection_error_message, action.device.name, action.error.localizedMessage)))
            }
            is BaseAppAction.SetupFailed -> {
                updateState(state, ConnectionScreenState
                        .ShowSetupError(context.getString(R.string.setup_error_message, action.device.name, action.error.localizedMessage)))
            }

            //todo update baseapp state to reflect vehicle info loading failed and then update the Connectionview state
            is BaseAppAction.VehicleInfoLoadingFailed -> {
                updateState(state, ConnectionScreenState
                        .ShowSetupError(context.getString(R.string.vehicle_info_loading_error_message, action.device.name, action.error.localizedMessage)))
            }


            is BaseAppAction.RunningSetup -> {
                updateState(state, ConnectionScreenState.ShowStatus(context.getString(R.string.running_setup_message, action.device.name)))
            }


            is BaseAppAction.LoadingVehicleInfo -> {
                updateState(state, ConnectionScreenState.ShowStatus(context.getString(R.string.loading_vehicle_info_message, action.device.name)))
            }

            else -> {
                state
            }
        }
    }


    private fun updateState(state: AppState, connectionScreenState: ConnectionScreenState): AppState {
        return state.copy(uiState = state
                .uiState
                .copy(backStack = state
                        .uiState
                        .backStack
                        .subList(0, state.uiState.backStack.size - 1) +
                        ConnectionScreen(connectionScreenState)))
    }

}
