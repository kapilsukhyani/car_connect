package com.exp.carconnect.base.fragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.app.Application
import android.arch.lifecycle.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v4.app.Fragment
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageView
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
    private lateinit var appLogo: ImageView
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

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        animateDeviceContainer { }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.view_device_management, null)
        bondedDeviceContainer = rootView.findViewById(R.id.bonded_devices_container)
        bondedDeviceList = rootView.findViewById(R.id.bonded_devices_list)
        containerLayout = rootView.findViewById(R.id.container)
        appLogo = rootView.findViewById(R.id.app_logo)

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


        val guidelineAnimator = ValueAnimator.ofFloat(1f, .465f)
        guidelineAnimator.startDelay = 1000
        guidelineAnimator.addUpdateListener { it ->
            constraintSet.clone(containerLayout)
            constraintSet.setGuidelinePercent(R.id.vertical_guideline, it.animatedValue as Float)
            constraintSet.applyTo(containerLayout)
        }
        guidelineAnimator.interpolator = LinearInterpolator()
        guidelineAnimator.duration = 300

        val appLogoAnimator = ValueAnimator.ofFloat(.92f, .70f)
        appLogoAnimator.addUpdateListener { it ->
            constraintSet.clone(containerLayout)
            constraintSet.setVerticalBias(R.id.app_logo, it.animatedValue as Float)
            constraintSet.applyTo(containerLayout)
        }
        appLogoAnimator.interpolator = LinearInterpolator()

        appLogoAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                appLogo.pivotX = appLogo.width / 2.toFloat()
                appLogo.pivotY = appLogo.height / 2.toFloat()
                appLogo
                        .animate()
                        .rotationBy(-90f)
                        .setDuration(200)
                        .setInterpolator(FastOutSlowInInterpolator())
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator?) {
                                appLogo
                                        .animate()
                                        .setListener(null)
                                        .setInterpolator(LinearInterpolator())
                                        .translationXBy(-500f)
                                        .start()
                            }
                        })
                        .start()

            }
        })


        val animatorSet = AnimatorSet()
        animatorSet.playSequentially(guidelineAnimator, appLogoAnimator)
        animatorSet.start()

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