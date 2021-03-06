package com.exp.carconnect.app.activity

import android.annotation.SuppressLint
import android.app.Application
import android.arch.lifecycle.*
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import android.transition.ChangeBounds
import android.transition.ChangeTransform
import android.transition.Fade
import android.transition.TransitionSet
import android.view.View
import com.exp.carconnect.app.CarConnectApp
import com.exp.carconnect.app.R
import com.exp.carconnect.app.state.ReportScreen
import com.exp.carconnect.base.*
import com.exp.carconnect.base.fragment.DeviceConnectionView
import com.exp.carconnect.base.fragment.DeviceManagementView
import com.exp.carconnect.base.fragment.SettingsView
import com.exp.carconnect.base.fragment.SplashView
import com.exp.carconnect.base.state.*
import com.exp.carconnect.dashboard.fragment.DashboardView
import com.exp.carconnect.dashboard.state.DashboardScreen
import com.exp.carconnect.donation.fragment.DonationView
import com.exp.carconnect.donation.state.DonationScreen
import com.exp.carconnect.report.fragment.ReportView
import com.kobakei.ratethisapp.RateThisApp
import io.reactivex.disposables.Disposable
import redux.api.Store


class CarConnectWindow : AppCompatActivity() {
    companion object {
        private const val CURRENT_FRAGMENT_TAG = "CURRENT_FRAGMENT_TAG"
    }

    private lateinit var windowContainer: View
    private lateinit var windowVM: WindowVM
    private lateinit var usageBanner: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.window)
        windowContainer = findViewById(R.id.window_container)
        usageBanner = findViewById(R.id.usage_report_banner)
        windowVM = ViewModelProviders.of(this)
                .get(WindowVM::class.java)
        windowVM.init(savedInstanceState)
        windowVM.getCurrentViewLiveData()
                .observe(this, Observer {
                    showView(it)
                })
        windowVM.getUsageBannerLiveData().observe(this, Observer {
            if (it == true) {
                showUsageBanner()
            }
        })
        if (savedInstanceState == null) {
            trySeekingRating()
        }
    }

    override fun onStop() {
        super.onStop()
        usageBanner
                .removeCallbacks(hideBannerRunnable)
    }

    private fun trySeekingRating() {
        RateThisApp.onCreate(this)
        RateThisApp.showRateDialogIfNeeded(this)
    }

    private val hideBannerRunnable = Runnable {
        usageBanner
                .animate()
                .translationYBy(-usageBanner.height.toFloat())
                .alpha(0.toFloat()).start()
    }

    private fun showUsageBanner() {
        usageBanner.visibility = View.VISIBLE
        usageBanner.postDelayed(hideBannerRunnable, 3500)
    }

    private fun showView(it: CarConnectView?) {
        when (it) {
            null -> {
                finish()
            }
            is SplashScreen -> {
                replaceFragment(SplashView())
            }

            is DeviceManagementScreen -> {
                replaceFragment(DeviceManagementView(), viewTransition = false)
                windowVM.showDonationBottomSheet(this)
            }

            is ConnectionScreen -> {
                replaceFragment(DeviceConnectionView())
            }

            is DashboardScreen -> {
                replaceFragment(DashboardView())
            }

            is SettingsScreen -> {
                replaceFragment(fragment = SettingsView(), viewTransition = false)
            }

            is ReportScreen -> {
                replaceFragment(fragment = ReportView(), viewTransition = false)
            }

            is DonationScreen -> {
                replaceFragment(fragment = DonationView(), viewTransition = false)
            }
        }
    }


    private fun FragmentTransaction.addShareElementTransitionIfNotEmpty(sharedElement: View? = null,
                                                                        sharedElementTransitionName: String? = null): FragmentTransaction {
        return if (sharedElement != null && null != sharedElementTransitionName) {
            this.addSharedElement(sharedElement, sharedElementTransitionName)
        } else {
            return this
        }
    }

    private fun replaceFragment(fragment: Fragment,
                                sharedElement: View? = null,
                                sharedElementTransitionName: String? = null,
                                viewTransition: Boolean = true) {

        if (getCurrentFragment()?.javaClass == fragment.javaClass) {
            return
        }
        if (viewTransition) {
            fragment.enterTransition = Fade()
            fragment.exitTransition = Fade()
        }
        if (null != sharedElement && null != sharedElementTransitionName) {
            fragment.sharedElementEnterTransition = SharedElementTransition()
            fragment.sharedElementReturnTransition = SharedElementTransition()
        }
        supportFragmentManager
                .beginTransaction()
                .addShareElementTransitionIfNotEmpty(sharedElement, sharedElementTransitionName)
                .replace(R.id.window_container, fragment, CURRENT_FRAGMENT_TAG)
                .commitNow()
    }

    private fun getCurrentFragment(): Fragment? {
        return supportFragmentManager.findFragmentByTag(CURRENT_FRAGMENT_TAG)
    }

    override fun onBackPressed() {
        val currentFragment = getCurrentFragment()
        if (currentFragment is BackInterceptor && currentFragment.interceptBack()) {
            return
        }
        val location = if (currentFragment != null) {
            currentFragment::class.java.simpleName
        } else {
            "unkown"
        }
        application.logBackPressedEvent(location)
        windowVM.onBackPressed()
    }

}

class SharedElementTransition : TransitionSet() {
    init {
        ordering = ORDERING_TOGETHER
        addTransition(ChangeBounds())
                .addTransition(ChangeTransform())
    }
}

class WindowVM(app: Application) : AndroidViewModel(app) {
    companion object {
        const val TAG = "WindowVM"
    }

    private val currentViewLiveData: MutableLiveData<CarConnectView> = MutableLiveData()
    private val stateSubscription: Disposable
    private val store: Store<AppState> = getApplication<CarConnectApp>()
            .store
    private var donationBottomSheetAttempted = false
    private var usageBannerShown = false

    init {
        stateSubscription = store
                .asCustomObservable()
                .map { it.uiState }
                .distinctUntilChanged { carConnectUIState: CarConnectUIState, carConnectUIState1: CarConnectUIState ->
                    carConnectUIState.currentView?.let { it::class.java } == carConnectUIState1.currentView?.let { it::class.java }

                }
                .subscribe({ uiState ->
                    currentViewLiveData.value = uiState.currentView
                }, { _ ->
                    //todo, think what should be done if subscribing to store errors out
                })
    }

    override fun onCleared() {
        stateSubscription.dispose()
    }

    fun getCurrentViewLiveData(): LiveData<CarConnectView> {
        return currentViewLiveData
    }

    @SuppressLint("CheckResult")
    fun getUsageBannerLiveData(): LiveData<Boolean> {
        val liveData = MutableLiveData<Boolean>()
        store
                .asCustomObservable()
                .filter { it.isBaseStateLoaded() }
                .take(1)
                .map { it.getBaseAppState().baseAppPersistedState.appSettings.usageReportingEnabled }
                .subscribe {
                    liveData.value = it && !usageBannerShown
                    usageBannerShown = true
                }
        return liveData
    }

    fun onBackPressed() {
        store.dispatch(CommonAppAction.BackPressed)
    }

    fun showDonationBottomSheet(activity: AppCompatActivity) {
        if (!donationBottomSheetAttempted) {
            com.exp.carconnect.donation.showDonationBottomSheet(activity, store, getApplication())
            donationBottomSheetAttempted = true
        }
    }

    fun init(savedInstanceState: Bundle?) {
        if (null == savedInstanceState) {
            store.dispatch(CommonAppAction.PushViewToBackStack(SplashScreen(SplashScreenState.LoadingAppState)))
        }
    }

}