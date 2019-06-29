package com.exp.carconnect.base.fragment

import android.app.AlertDialog
import android.app.Application
import android.arch.lifecycle.*
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.v4.app.Fragment
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.crashlytics.android.answers.CustomEvent
import com.exp.carconnect.base.*
import com.exp.carconnect.base.R
import com.exp.carconnect.base.state.*
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber

class SplashView : Fragment() {
    companion object {
        const val TAG = "SplashView"
    }

    private lateinit var appLogo: View
    private lateinit var containerLayout: ConstraintLayout
    private lateinit var splashVM: SplashVM
    private var ignoreCreate = false
    private val constraintSet = ConstraintSet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (activity!!.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                && !ignoreCreate) {
            splashVM = ViewModelProviders
                    .of(this)
                    .get(SplashVM::class.java)
            splashVM.getAppLoadingStateLiveData()
                    .observe(this, Observer {
                        showStatus(it!!)
                    })

        }
    }

    private fun showStatus(it: SplashScreenState) {
        Timber.d("$TAG Received status $it")
        when (it) {
            SplashScreenState.ShowPrivacyPolicy -> {
                showPrivacyPolicyScreen()
            }
            is SplashScreenState.AppStateLoaded -> {
                splashVM.onAppStateLoaded(it.appState)
            }
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (activity!!.resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) {
            activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            ignoreCreate = true
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = layoutInflater.inflate(R.layout.view_splash, null)
        appLogo = view.findViewById(R.id.app_logo)
        containerLayout = view.findViewById(R.id.container)
        constraintSet.clone(containerLayout)
        return view
    }

    // policy generated with https://app-privacy-policy-generator.firebaseapp.com/#
    private fun showPrivacyPolicyScreen() {
        val dialog = AlertDialog
                .Builder(activity)
                .setTitle(getString(R.string.privacy_policy_title))
                //todo do this with spannable
                .setMessage(Html.fromHtml("Please accept <a href=\"https://github.com/kapilsukhyani/car_connect/blob/master/privacy_policy.md\">Policy</a> by clicking OK."))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                    splashVM.onPolicyAccepted()
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                    splashVM.onPolicyRejected()
                }
                .create()
        dialog.show()
        (dialog.findViewById(android.R.id.message) as TextView).movementMethod = LinkMovementMethod.getInstance()
    }

}

class SplashVM(app: Application) : AndroidViewModel(app) {

    private val splashScreenStateLiveData: MutableLiveData<SplashScreenState> = MutableLiveData()
    private val stateSubscription: CompositeDisposable = CompositeDisposable()

    private val store = (app as BaseAppContract).store

    init {
        val stateOb: Observable<SplashScreenState> = store
                .asCustomObservable()
                .map { it.moduleStateMap }
                .distinctUntilChanged()
                .map {
                    it.forEach { entry ->
                        when (entry.value) {
                            LoadableState.Loading,
                            LoadableState.NotLoaded -> {
                                return@map SplashScreenState.LoadingAppState
                            }
                            is LoadableState.LoadingError -> {
                                return@map SplashScreenState.ShowLoadingError
                            }
                        }
                    }


                    val baseAppState = (it[BaseAppState.STATE_KEY] as LoadableState.Loaded<BaseAppState>).savedState
                    if (!baseAppState.baseAppPersistedState.appSettings.privacyPolicyAccepted) {
                        return@map SplashScreenState.ShowPrivacyPolicy
                    } else {
                        return@map SplashScreenState.AppStateLoaded(baseAppState)
                    }

                }
                .distinctUntilChanged()


        stateSubscription.add(stateOb.subscribe({
            splashScreenStateLiveData.value = it

        },
                {
                    //todo log error
                }
        ))


    }

    fun onAppStateLoaded(baseAppState: BaseAppState) {
        val app = getApplication<Application>()
        if (baseAppState.baseAppPersistedState
                        .appSettings
                        .usageReportingEnabled) {
            app.enableReporting()
            app.logContentViewEvent("SplashView")
        }
        if (baseAppState.activeSession is UnAvailableAvailableData.Available) {
            //moving background session to foreground
            store.dispatch(BaseAppAction.MoveBackgroundSessionToForeground)
            app.onDataLoadingStartedFor((baseAppState.activeSession.data.vehicle as LoadableState.Loaded).savedState)
        } else {
            store.dispatch(CommonAppAction.ReplaceViewOnBackStackTop(DeviceManagementScreen(DeviceManagementScreenState.LoadingDevices)))
        }
    }

    fun getAppLoadingStateLiveData(): LiveData<SplashScreenState> {
        return splashScreenStateLiveData
    }

    override fun onCleared() {
        stateSubscription.dispose()
    }

    fun onPolicyAccepted() {
        getApplication<Application>().onPrivacyPolicyAccepted()
    }

    fun onPolicyRejected() {
        getApplication<Application>().logEvent(CustomEvent("Policy rejected"))
        store.dispatch(CommonAppAction.BackPressed)
    }
}