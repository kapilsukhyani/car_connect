package com.exp.carconnect.base.fragment

import android.animation.ObjectAnimator
import android.app.Application
import android.arch.lifecycle.*
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.exp.carconnect.base.AppState
import com.exp.carconnect.base.BaseAppContract
import com.exp.carconnect.base.R
import com.exp.carconnect.base.asCustomObservable
import com.exp.carconnect.base.state.*
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.view_device_connection.*
import redux.api.Reducer


class DeviceConnectionView : Fragment() {
    companion object {
        const val TAG = "DeviceConnectionView"
    }

    private lateinit var deviceConnectionVM: DeviceConnectionVM
    private lateinit var trackAnimator: ObjectAnimator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.view_device_connection, null)
        return rootView
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        deviceConnectionVM = ViewModelProviders.of(this).get(DeviceConnectionVM::class.java)
        deviceConnectionVM
                .getScreenStateLiveData()
                .observe(this, Observer {
                    onNewState(it!!)
                })

        animateTrack()
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
            is ConnectionScreenState.Connecting -> {
                setStatus("Connecting to ${it.device.name}")
            }

            is ConnectionScreenState.ShowingConnectionError -> {
                setStatus("Connection error")
            }

            is ConnectionScreenState.SetupCompleted -> {
                setStatus("Setup Completed")
            }

            is ConnectionScreenState.RunningSetup -> {
                setStatus("Running setup")
            }

            is ConnectionScreenState.ShwowingSetupError -> {
                setStatus("Setup error")
            }

            is ConnectionScreenState.LoadinVehicleInfo -> {
                setStatus("Loading Vehicle Info")
            }

            is ConnectionScreenState.VehicleInfoLoaded -> {
                setStatus("Loaded Vehicle Info")
            }

            is ConnectionScreenState.VehicleLoadingFailed -> {
                setStatus("Loaded Vehicle Failed")
            }
        }
    }

    private fun setStatus(statusText: String) {
        status.text = statusText
    }


}

class DeviceConnectionVM(app: Application) : AndroidViewModel(app) {
    private val deviceConnectionViewLiveData = MutableLiveData<ConnectionScreenState>()
    private val store = (app as BaseAppContract).store
    private val storeSubscription: Disposable

    init {
        storeSubscription = store
                .asCustomObservable()
                .filter { it.uiState.currentView is ConnectionScreen }
                .map { (it.uiState.currentView as ConnectionScreen).screenState }
                .distinctUntilChanged()
                .subscribe {
                    deviceConnectionViewLiveData.value = it
                }
    }

    override fun onCleared() {
        storeSubscription.dispose()
    }

    fun getScreenStateLiveData(): LiveData<ConnectionScreenState> {
        return deviceConnectionViewLiveData
    }


}


class DeviceConnectionScreenStateReducer : Reducer<AppState> {
    override fun reduce(state: AppState, action: Any): AppState {
        return when (action) {
            is BaseAppActions.DeviceConnectionFailed -> {
                updateState(state, ConnectionScreenState
                        .ShowingConnectionError(ConnectionError.UnkownError(action.error)))
            }

            is BaseAppActions.RunningSetup -> {
                updateState(state, ConnectionScreenState.RunningSetup)
            }

            is BaseAppActions.SetupFailed -> {
                updateState(state, ConnectionScreenState.ShwowingSetupError(SetupError.UnkownError(action.error)))
            }

            is BaseAppActions.VehicleInfoLoaded -> {
                updateState(state, ConnectionScreenState.VehicleInfoLoaded(action.info))
            }
            is BaseAppActions.VehicleInfoLoadingFailed -> {
                updateState(state, ConnectionScreenState.VehicleLoadingFailed(OBDDataLoadError.UnkownError(action.error)))
            }
            is BaseAppActions.LoadingVehicleInfo -> {
                updateState(state, ConnectionScreenState.LoadinVehicleInfo)
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
