package com.exp.carconnect.base.fragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.app.Application
import android.arch.lifecycle.*
import android.bluetooth.BluetoothAdapter
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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import com.crashlytics.android.answers.CustomEvent
import com.exp.carconnect.base.*
import com.exp.carconnect.base.R
import com.exp.carconnect.base.state.*
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.view_device_management.*
import redux.api.Reducer
import timber.log.Timber


class DeviceManagementView : Fragment() {

    companion object {
        const val TAG = "DeviceManagementView"
    }

    private lateinit var deviceManagementVM: DeviceManagementVM
    private lateinit var bondedDeviceContainer: View
    private lateinit var appLogo: ImageView
    private lateinit var bondedDeviceList: RecyclerView
    private lateinit var containerLayout: ConstraintLayout
    private lateinit var settingsIcon: View
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
        settingsIcon = rootView.findViewById(R.id.settings_icon)

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
        settingsIcon.setOnClickListener {
            deviceManagementVM.onSettingsIconClicked()
        }
    }

    private fun onNewState(it: DeviceManagementScreenState) {
        Timber.d("$TAG Received new state : $it")
        when (it) {
            is DeviceManagementScreenState.ShowingDevices -> {
                showDevices(it.devices)
                if (it.showUsageReportBanner) {
                    showUsageBanner()
                }
            }

            is DeviceManagementScreenState.ShowingError -> {
                showError(it.title, it.message)
            }
        }

    }

    private val hideBannerRunnable = Runnable {
        usage_report_banner
                .animate()
                .translationYBy(-usage_report_banner.height.toFloat())
                .alpha(0.toFloat()).start()
    }

    private fun showUsageBanner() {
        usage_report_banner.visibility = View.VISIBLE
        usage_report_banner.postDelayed(hideBannerRunnable, 3500)
    }

    private fun showError(title: String, message: String) {
        AlertDialog
                .Builder(activity)
                .setTitle(getString(R.string.bluetooth_error_title))
                .setMessage(getString(R.string.bluetooth_not_available_message))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                    deviceManagementVM.onBluetoothErrorAcknowledged()
                }
                .create()
                .show()
    }

    override fun onStop() {
        super.onStop()
        usage_report_banner
                .removeCallbacks(hideBannerRunnable)
    }

    private fun showDevices(devices: Set<OBDDongle>) {
        animateDeviceContainer {
        }
        bondedDeviceList.adapter = DeviceListAdapter(DeviceManagementView@ this.activity, devices.toList()) {
            onDeviceSelected(it)
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

    private fun onDeviceSelected(device: OBDDongle) {
        deviceManagementVM.onDeviceSelected(device)
    }

}


class DeviceManagementVM(app: Application) : AndroidViewModel(app) {

    private val dongleLoader = OBDDongleLoader()
    private val deviceManagementViewLiveData = MutableLiveData<DeviceManagementScreenState>()
    private val store = (app as BaseAppContract).store
    private val storeSubscription: CompositeDisposable = CompositeDisposable()
    private var backgroundConnectionEnabled = false

    init {
        app.logContentViewEvent("DeviceManagementView")
        storeSubscription.add(store
                .asCustomObservable()
                .filter { it.uiState.currentView is DeviceManagementScreen }
                .map {
                    (it.uiState.currentView as DeviceManagementScreen).screenState
                }
                .distinctUntilChanged()
                .subscribe {
                    deviceManagementViewLiveData.value = it
                    if (it is DeviceManagementScreenState.ShowingError) {
                        app.logContentViewEvent("BluetoothErrorDialog")
                    }
                })

        //kill the screen if an active session is already available, this is required if bg connection was enabled being on dashboard.
        storeSubscription.add(store
                .asCustomObservable()
                .filter { it.isBaseStateLoaded() }
                .map { Pair(it.getAppSettings().backgroundConnectionEnabled, it.isAnActiveSessionAvailable()) }
                .distinctUntilChanged()
                .subscribe {
                    backgroundConnectionEnabled = it.first
                    //finish if session is still alive, this happened because background session setting was enabled being on dashboard
                    if (it.second) {
                        store.dispatch(CommonAppAction.FinishCurrentView)
                    } else {
                        loadBluetoothBondedDevices()
                    }
                })
    }

    private fun loadBluetoothBondedDevices() {
        storeSubscription.add(store.asCustomObservable()
                .take(1)
                .map {
                    Pair(it.getBaseAppState().baseAppPersistedState, it.uiState.isRestoringCurrentViewFromBackStack())
                }
                .subscribe {
                    val persistedState = it.first
                    val isViewRestoredFromBackStack = it.second
                    if (persistedState.lastConnectedDongle != null &&
                            !isViewRestoredFromBackStack &&
                            persistedState.appSettings.autoConnectToLastConnectedDongleOnLaunch) {
                        val adapter = BluetoothAdapter.getDefaultAdapter()
                        if (adapter?.isEnabled != true) {
                            store.dispatch(DeviceManagementViewAction
                                    .ShowError(getString(R.string.bluetooth_error_title),
                                            getString(R.string.last_device_auto_connection_bt_error,
                                                    persistedState.lastConnectedDongle.name)))
                        } else {
                            onDeviceSelected(OBDDongle(adapter.getRemoteDevice(persistedState.lastConnectedDongle.address)))

                        }
                    } else {
                        store.dispatch(DeviceManagementViewAction
                                .ShowDonglesAndUsageReportBanner(dongleLoader.loadDevices(includeSimulator = true),
                                        persistedState.appSettings.usageReportingEnabled))
                    }
                })
    }

    override fun onCleared() {
        storeSubscription.dispose()
    }

    fun getScreenStateLiveData(): LiveData<DeviceManagementScreenState> {
        return deviceManagementViewLiveData
    }

    fun onDeviceSelected(device: OBDDongle) {
        getApplication<Application>().logEvent(CustomEvent("bonded_device_selected")
                .putCustomAttribute("Device", "[${device.name} - ${device.address}]"))

        if (backgroundConnectionEnabled) {
            store.dispatch(CommonAppAction.ReplaceViewOnBackStackTop(ConnectionScreen(ConnectionScreenState
                    .ShowStatus(getApplication<Application>()
                            .getString(R.string.connecting_message, device.name)))))
        } else {
            store.dispatch(CommonAppAction.PushViewToBackStack(ConnectionScreen(ConnectionScreenState
                    .ShowStatus(getApplication<Application>()
                            .getString(R.string.connecting_message, device.name)))))
        }
        store.dispatch(BaseAppAction.StartNewSession(device))
    }

    fun onBluetoothErrorAcknowledged() {
        getApplication<Application>().logEvent(CustomEvent("bluetooth_error_acknowledged"))
        store.dispatch(CommonAppAction.BackPressed)
    }

    fun onSettingsIconClicked() {
        getApplication<Application>().logSettingsClickedEvent("DeviceManagementView")
        store.dispatch(CommonAppAction.PushViewToBackStack(SettingsScreen(SettingsScreenState.ShowingSettings)))
    }
}


sealed class DeviceManagementViewAction {
    data class ShowDonglesAndUsageReportBanner(val devices: Set<OBDDongle>, val showBanner: Boolean = false) : DeviceManagementViewAction()
    data class ShowError(val title: String,
                         val message: String) : DeviceManagementViewAction()
}


class DeviceManagementScreenStateReducer : Reducer<AppState> {
    override fun reduce(state: AppState, action: Any): AppState {
        return when (action) {
            is DeviceManagementViewAction.ShowDonglesAndUsageReportBanner -> {
                state.copy(uiState = state
                        .uiState
                        .copy(backStack = state
                                .uiState
                                .backStack
                                .subList(0, state.uiState.backStack.size - 1) +
                                DeviceManagementScreen(DeviceManagementScreenState.ShowingDevices(action.devices, action.showBanner))))
            }

            is DeviceManagementViewAction.ShowError -> {
                state.copy(uiState = state
                        .uiState
                        .copy(backStack = state
                                .uiState
                                .backStack
                                .subList(0, state.uiState.backStack.size - 1) +
                                DeviceManagementScreen(DeviceManagementScreenState.ShowingError(action.title, action.message))))
            }
            else -> {
                state
            }
        }
    }

}


private class DeviceListAdapter(val context: Context, val dongles: List<OBDDongle>, val itemClickListener: (OBDDongle) -> Unit) : RecyclerView.Adapter<BondedDeviceRowViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): BondedDeviceRowViewHolder {
        return BondedDeviceRowViewHolder(LayoutInflater.from(context).inflate(R.layout.view_available_devices_row, null) as TextView, itemClickListener)
    }

    override fun getItemCount(): Int {
        return dongles.size
    }

    override fun onBindViewHolder(holder: BondedDeviceRowViewHolder, position: Int) {
        if (!dongles[position].connectable()) {
            holder.textView.isEnabled = false
        }
        holder.textView.text = dongles[position].name
        holder.textView.tag = dongles[position]
    }

}

private class BondedDeviceRowViewHolder(val textView: TextView,
                                        itemClickListener: (OBDDongle) -> Unit) : RecyclerView.ViewHolder(textView) {
    init {
        textView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        textView.setOnClickListener {
            itemClickListener.invoke(textView.tag as OBDDongle)
        }
    }
}