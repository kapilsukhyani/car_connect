package com.exp.carconnect.base.fragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.app.Application
import android.arch.lifecycle.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v4.app.Fragment
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import com.exp.carconnect.base.AppState
import com.exp.carconnect.base.BaseAppContract
import com.exp.carconnect.base.R
import com.exp.carconnect.base.asCustomObservable
import com.exp.carconnect.base.state.*
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

    }


    override fun onAttach(context: Context?) {
        super.onAttach(context)
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.view_device_management, null)
        bondedDeviceContainer = rootView.findViewById(R.id.bonded_devices_container)
        bondedDeviceList = rootView.findViewById(R.id.bonded_devices_list)
        containerLayout = rootView.findViewById(R.id.container)
        appLogo = rootView.findViewById(R.id.app_logo)

        bondedDeviceList.layoutManager = LinearLayoutManager(activity)
        bondedDeviceList.itemAnimator = DefaultItemAnimator()
        bondedDeviceList.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
        return rootView
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        deviceManagementVM = ViewModelProviders.of(this)
                .get(DeviceManagementVM::class.java)
        deviceManagementVM.getScreenStateLiveData()
                .observe(this, Observer {
                    onNewState(it!!)
                })
    }

    private fun onNewState(it: DeviceManagementScreenState) {
        Log.d(TAG, "Received new state : $it")
        when (it) {
            is DeviceManagementScreenState.ShowingDevices -> showDevices(it.devices)

            DeviceManagementScreenState.ShowingBluetoothUnAvailableError -> {
                showBluetoothNotAvailableError()
            }
        }

    }

    private fun showBluetoothNotAvailableError() {
        AlertDialog
                .Builder(activity)
                .setTitle(getString(R.string.bluetooth_error_title))
                .setMessage(getString(R.string.bluetooth_not_available_message))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { dialog, which ->
                    deviceManagementVM.onBluetoothErrorAcknowledged()
                    dialog.dismiss()
                }
                .create()
                .show()
    }

    private fun showDevices(devices: Set<BluetoothDevice>) {
        animateDeviceContainer {
        }
        bondedDeviceList.adapter = BondedDeviceAdapter(DeviceManagementView@ this.activity, devices.toList()) {
            deviceManagementVM.onDeviceSelected(it)
        }
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
                                        .setListener(object : AnimatorListenerAdapter() {
                                            override fun onAnimationEnd(animation: Animator?) {
                                                onAnimationComplete()
                                            }
                                        })
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
                    if (it.isEnabled) {
                        store.dispatch(DeviceManagementViewAction
                                .ShowBondedDevices(it
                                        .bondedDevices))
                    } else {
                        store.dispatch(DeviceManagementViewAction
                                .ShowBluetoothError)
                    }
                } ?: store.dispatch(DeviceManagementViewAction
                .ShowBluetoothError)

    }

    override fun onCleared() {
        storeSubscription.dispose()
    }

    fun getScreenStateLiveData(): LiveData<DeviceManagementScreenState> {
        return deviceManagementViewLiveData
    }

    fun onDeviceSelected(device: BluetoothDevice) {
        store.dispatch(CommonAppAction.PushViewToBackStack(ConnectionScreen(ConnectionScreenState
                .ShowStatus(getApplication<Application>()
                        .getString(R.string.connecting_message, device.name)))))
        store.dispatch(BaseAppAction.StartNewSession(device))
    }

    fun onBluetoothErrorAcknowledged() {
        store.dispatch(CommonAppAction.BackPressed)
    }
}


sealed class DeviceManagementViewAction {
    data class ShowBondedDevices(val devices: Set<BluetoothDevice>) : DeviceManagementViewAction()
    object ShowBluetoothError : DeviceManagementViewAction()
}


class DeviceManagementScreenStateReducer : Reducer<AppState> {
    override fun reduce(state: AppState, action: Any): AppState {
        return when (action) {
            is DeviceManagementViewAction.ShowBondedDevices -> {

                state.copy(uiState = state
                        .uiState
                        .copy(backStack = state
                                .uiState
                                .backStack
                                .subList(0, state.uiState.backStack.size - 1) +
                                DeviceManagementScreen(DeviceManagementScreenState.ShowingDevices(action.devices))))
            }

            is DeviceManagementViewAction.ShowBluetoothError -> {

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


private class BondedDeviceAdapter(val context: Context, val bondedDevices: List<BluetoothDevice>, val itemClickListener: (BluetoothDevice) -> Unit) : RecyclerView.Adapter<BondedDeviceRowViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): BondedDeviceRowViewHolder {
        return BondedDeviceRowViewHolder(LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, null) as TextView, itemClickListener)
    }

    override fun getItemCount(): Int {
        return bondedDevices.size
    }

    override fun onBindViewHolder(holder: BondedDeviceRowViewHolder, position: Int) {
        holder.textView.text = bondedDevices[position].name
        holder.textView.tag = bondedDevices[position]
    }

}

private class BondedDeviceRowViewHolder(val textView: TextView, itemClickListener: (BluetoothDevice) -> Unit) : RecyclerView.ViewHolder(textView) {
    init {
        textView.setOnClickListener {
            itemClickListener.invoke(textView.tag as BluetoothDevice)
        }
    }
}