package com.exp.carconnect.base.fragment

import android.app.Application
import android.arch.lifecycle.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.os.Handler
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.transition.Slide
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.exp.carconnect.base.AppState
import com.exp.carconnect.base.BaseAppContract
import com.exp.carconnect.base.R
import com.exp.carconnect.base.asCustomObservable
import com.exp.carconnect.base.state.CommonAppAction
import com.exp.carconnect.base.state.DeviceManagementScreen
import com.exp.carconnect.base.state.DeviceManagementScreenState
import io.reactivex.disposables.Disposable
import redux.api.Reducer


class DeviceManagementView : Fragment() {

    companion object {
        const val TAG = "DeviceManagementView"
        fun getSharedElementTransitionName(): String {
            return "app_logo_transition"
        }
    }

    private lateinit var deviceManagementVM: DeviceManagementVM
    private lateinit var bondedDeviceContainer: View
    private lateinit var bondedDeviceList: RecyclerView
    private lateinit var containerLayout: ConstraintLayout
    private val constraintSet = ConstraintSet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deviceManagementVM = ViewModelProviders.of(this)
                .get(DeviceManagementVM::class.java)
        deviceManagementVM.getScreenStateLiveData()
                .observe(this, Observer {
                    onNewState(it!!)
                })
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.view_device_management, null)
        bondedDeviceContainer = rootView.findViewById(R.id.bonded_devices_container)
        bondedDeviceList = rootView.findViewById(R.id.bonded_devices_list)
        containerLayout = rootView.findViewById(R.id.container)
        constraintSet.clone(containerLayout)
        animateDeviceContainer { }
        return rootView
    }

    private fun onNewState(it: DeviceManagementScreenState) {
        Log.d(TAG, "Received new state : $it")
        when (it) {
            is DeviceManagementScreenState.ShowingDevices -> showDevices(it.devices)

            DeviceManagementScreenState.ShowingBluetoothUnAvailableError -> {

            }
        }

    }

    private fun showDevices(devices: Set<BluetoothDevice>) {
        animateDeviceContainer { }

    }


    private fun animateDeviceContainer(onAnimationComplete: () -> Unit) {
        Handler().postDelayed({
            constraintSet.setGuidelinePercent(R.id.vertical_guideline, 0.4f)
            TransitionManager.beginDelayedTransition(containerLayout)
            constraintSet.applyTo(containerLayout)
        }, 1000)

    }

    private fun onDeviceSelected(device: BluetoothDevice) {
        deviceManagementVM.onDeviceSelected(device)

    }


}


class DeviceManagementVM(app: Application) : AndroidViewModel(app) {


    private val deviceManagementViewLiveData = MutableLiveData<DeviceManagementScreenState>()
    private val store = (app as BaseAppContract).store
    private val storeSubscription: Disposable

    init {
        storeSubscription = store
                .asCustomObservable()
                .filter { it.uiState.currentView is DeviceManagementScreen }
                .map { (it.uiState.currentView as DeviceManagementScreen).screenState }
                .distinctUntilChanged()
                .subscribe {
                    deviceManagementViewLiveData.value = it
                }

        BluetoothAdapter
                .getDefaultAdapter()?.let {
                    store.dispatch(DeviceManagementViewScreenAction
                            .BondedDevicesAvailable(it
                                    .bondedDevices))
                } ?: store.dispatch(DeviceManagementViewScreenAction
                .BluetoothNotAvailable)

    }

    override fun onCleared() {
        storeSubscription.dispose()
    }

    fun getScreenStateLiveData(): LiveData<DeviceManagementScreenState> {
        return deviceManagementViewLiveData
    }

    fun onDeviceSelected(device: BluetoothDevice) {
        store.dispatch(CommonAppAction.BondedDeviceSelected(device))
    }
}


sealed class DeviceManagementViewScreenAction {
    data class BondedDevicesAvailable(val devices: Set<BluetoothDevice>) : DeviceManagementViewScreenAction()
    object BluetoothNotAvailable : DeviceManagementViewScreenAction()
}


class DeviceManagementScreenStateReducer : Reducer<AppState> {
    override fun reduce(state: AppState, action: Any): AppState {
        return when (action) {
            is DeviceManagementViewScreenAction.BondedDevicesAvailable -> {

                state.copy(uiState = state
                        .uiState
                        .copy(backStack = state
                                .uiState
                                .backStack
                                .subList(0, state.uiState.backStack.size - 1) +
                                DeviceManagementScreen(DeviceManagementScreenState.ShowingDevices(action.devices))))
            }

            is DeviceManagementViewScreenAction.BluetoothNotAvailable -> {

                state.copy(uiState = state
                        .uiState
                        .copy(backStack = state
                                .uiState
                                .backStack
                                .subList(0, state.uiState.backStack.size - 1) +
                                DeviceManagementScreen(DeviceManagementScreenState.ShowingBluetoothUnAvailableError)))
            }
            else -> {
                state
            }
        }
    }

}