package com.exp.carconnect.donation.fragment

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.app.ProgressDialog
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.constraint.ConstraintSet
import android.support.transition.TransitionManager
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.android.billingclient.api.*
import com.crashlytics.android.answers.AddToCartEvent
import com.crashlytics.android.answers.CustomEvent
import com.crashlytics.android.answers.PurchaseEvent
import com.exp.carconnect.base.*
import com.exp.carconnect.base.state.CommonAppAction
import com.exp.carconnect.donation.R
import com.exp.carconnect.donation.state.*
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.donation_view.*
import redux.api.Reducer
import java.util.*

class DonationView : Fragment() {
    private lateinit var viewModel: DonationVM
    private var progressDialog: ProgressDialog? = null
    private var ignoreCreate = false

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (activity!!.resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT) {
            activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            ignoreCreate = true
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.donation_view, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (view.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                && !ignoreCreate) {
            viewModel = ViewModelProviders.of(this).get(DonationVM::class.java)
            donate_button.setOnClickListener {
                viewModel.startDonationFlow(activity!!, product_list.selectedItem as Product)

            }
            viewModel.getScreenStateLiveData()
                    .observe(this, Observer {
                        onNewState(it!!)
                    })
        }
    }

    private fun onNewState(it: DonationScreenState) {
        when (it) {
            is DonationScreenState.ShowLoading -> {
                showLoading()
            }

            is DonationScreenState.ShowProducts -> {
                hideLoading()
                showProducts(it.products)
            }

            is DonationScreenState.ShowError -> {
                hideLoading()
                showError(it.errorMessage, it.errorCode, it.products)
            }

            is DonationScreenState.ShowDonatedMessage -> {
                hideLoading()
                startDonatedAnimation()
            }
        }
    }

    private fun startDonatedAnimation() {
        val postTransitionConstraintLayout = activity!!
                .layoutInflater
                .inflate(R.layout.donation_view_post_transition, null)
                as ConstraintLayout

        val constraintSet = ConstraintSet()
        constraintSet.clone(postTransitionConstraintLayout)
        TransitionManager.beginDelayedTransition(constraint_container)
        constraintSet.applyTo(constraint_container)
        awesome_text.postDelayed(Runnable {
            center_heart.visibility = View.INVISIBLE
            spark_button.visibility = View.VISIBLE
            awesome_text.visibility = View.VISIBLE
            spark_button.playAnimation()
        }, 300)

    }


    private fun showError(errorMessage: String, errorCode: Int, products: List<Product>?) {
        AlertDialog
                .Builder(activity)
                .setTitle(getString(R.string.donation_error_title))
                .setMessage(errorMessage)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    viewModel.onErrorAcknowledged(errorCode, products)
                    dialog.dismiss()
                }
                .create()
                .show()
    }

    private fun showProducts(products: List<Product>) {
        product_list.adapter = ArrayAdapter<Product>(activity, R.layout.donation_item, products)
    }

    private fun showLoading() {
        progressDialog = ProgressDialog.show(activity,
                activity!!.getString(R.string.loading),
                activity!!.getString(com.exp.carconnect.base.R.string.please_wait))
    }

    private fun hideLoading() {
        progressDialog?.dismiss()
        progressDialog = null
    }

}


internal class DonationVM(app: Application) : AndroidViewModel(app), PurchasesUpdatedListener {

    private lateinit var billingClient: BillingClient
    private val donationViewScreenStateLiveData = MutableLiveData<DonationScreenState>()
    private val disposables = CompositeDisposable()
    private val store = (app as BaseAppContract).store

    companion object {
        private val DONATION_PRODUCT_IDs = arrayOf("carconnectdonationproduct",
                "carconnectdonationproduct2",
                "carconnectdonationproduct3")
    }

    init {
        app.logContentViewEvent("DonationView")
        disposables.add(store
                .asCustomObservable()
                .filter { it.uiState.currentView is DonationScreen }
                .map { (it.uiState.currentView as DonationScreen).screenState }
                .distinctUntilChanged()
                .subscribe {
                    if (it == DonationScreenState.FinishDonationView) {
                        store.dispatch(CommonAppAction.FinishCurrentView)
                    } else {
                        donationViewScreenStateLiveData.value = it
                    }
                })

        setupBilling()
    }

    private fun setupBilling() {
        billingClient = BillingClient
                .newBuilder(getApplication())
                .enablePendingPurchases()
                .setListener(this)
                .build()
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult?) {
                if (billingResult?.responseCode == BillingClient.BillingResponseCode.OK) {
                    // The billing client is ready. You can query purchases here.
                    queryProducts()
                } else {
                    getApplication<Application>().logNonFatal(IllegalStateException("Billing Setup Failed [${billingResult?.responseCode}]"))
                    handleUnSuccessfulResponse(billingResult?.responseCode
                            ?: BillingClient.BillingResponseCode.ERROR)
                }
            }


            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                handlePurchaseTerminated(getApplication<Application>()
                        .getString(R.string.donation_service_disconnected_message),
                        BillingClient.BillingResponseCode.SERVICE_DISCONNECTED)
            }
        })
    }

    override fun onPurchasesUpdated(result: BillingResult?, purchases: MutableList<Purchase>?) {
        if (result?.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else {
            if (purchases != null) {
                for (purchase in purchases) {
                    getApplication<Application>().logEvent(CustomEvent("purchase_failed")
                            .putCustomAttribute("response_code", result?.responseCode))
                }
            } else {
                getApplication<Application>().logEvent(CustomEvent("purchase_failed")
                        .putCustomAttribute("response_code", result?.responseCode))
            }
            // Handle any other error codes.
            handleUnSuccessfulResponse(result?.responseCode
                    ?: BillingClient.BillingResponseCode.ERROR)
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        getApplication<Application>().logEvent(PurchaseEvent()
                .putItemId(purchase.sku)
                .putSuccess(true))
        store.dispatch(DonationAction.StoreDonation(purchase.sku,
                purchase.orderId,
                purchase.purchaseTime,
                purchase.purchaseToken))
        store.dispatch(DonationViewAction.PaymentSuccessful)
    }

    private fun handlePurchaseTerminated(message: String, errorCode: Int) {
        store.dispatch(DonationViewAction.ShowError(message, errorCode))
    }

    private fun queryProducts() {
        val skuList = ArrayList<String>()
        skuList.addAll(DONATION_PRODUCT_IDs)
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)
        billingClient.querySkuDetailsAsync(params.build()) { result, skuDetailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                val products = ArrayList<Product>()
                for (skuDetails in skuDetailsList) {
                    products.add(Product(skuDetails.price,
                            skuDetails.priceCurrencyCode,
                            skuDetails.title,
                            skuDetails.sku,
                            skuDetails))
                }

                store.dispatch(DonationViewAction.ShowProducts(products))
            } else {
                getApplication<Application>().logNonFatal(IllegalStateException("Querying products failed [$result]"))
                handleUnSuccessfulResponse(result.responseCode)
            }
        }
    }

    fun startDonationFlow(activity: Activity, product: Product) {
        getApplication<Application>().logEvent(AddToCartEvent()
                .putItemId(product.id)
                .putCurrency(Currency.getInstance(product.currencyCode)))
        store.dispatch(DonationViewAction.StartPaymentFlow(product))
        val flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(product.derivedFrom)
//                .setSku("android.test.purchased") // test product id. https://developer.android.com/google/play/billing/billing_testing
//                .setSku("android.test.canceled") // test product id. https://developer.android.com/google/play/billing/billing_testing
//                .setSku("android.test.item_unavailable") // test product id. https://developer.android.com/google/play/billing/billing_testing
//                .setType(BillingClient.SkuType.INAPP) // SkuType.SUB for subscription
                .build()
        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            handleUnSuccessfulResponse(result.responseCode)
        }
    }

    //message uses unicode defined here http://wrttn.me/30dbfd/
    private fun handleUnSuccessfulResponse(responseCode: Int) {
        when (responseCode) {
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                handlePurchaseTerminated(getApplication<Application>().getString(R.string.donation_feature_not_available_message), responseCode)
            }
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {
                handlePurchaseTerminated(getApplication<Application>().getString(R.string.donation_feature_not_available_message), responseCode)
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                handlePurchaseTerminated(getApplication<Application>().getString(R.string.donation_item_already_owned_message), responseCode)
            }
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> {
                handlePurchaseTerminated(getApplication<Application>().getString(R.string.donation_item_not_owned_message), responseCode)
            }

            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> {
                handlePurchaseTerminated(getApplication<Application>().getString(R.string.donation_item_unavailable_message), responseCode)
            }

            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> {
                handlePurchaseTerminated(getApplication<Application>().getString(R.string.donation_feature_not_available_message), responseCode)
            }

            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                handlePurchaseTerminated(getApplication<Application>().getString(R.string.donation_service_disconnected_message), responseCode)
            }
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> {
                handlePurchaseTerminated(getApplication<Application>().getString(R.string.donation_network_error_message), responseCode)
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                handlePurchaseTerminated(getApplication<Application>().getString(R.string.donation_user_canceled_message), responseCode)
            }
            BillingClient.BillingResponseCode.ERROR -> {
                handlePurchaseTerminated(getApplication<Application>().getString(R.string.donation_something_went_wrong_message), responseCode)
            }
        }
    }

    fun getScreenStateLiveData(): LiveData<DonationScreenState> {
        return donationViewScreenStateLiveData
    }

    override fun onCleared() {
        disposables.dispose()
        disposables.clear()
    }

    fun onErrorAcknowledged(errorCode: Int,
                            products: List<Product>?) {
        getApplication<Application>().logEvent(CustomEvent("donation_error_acknowledged"))
        store.dispatch(DonationViewAction.ErrorAcknowledged(errorCode))
    }

}

class DonationScreenStateReducer : Reducer<AppState> {
    override fun reduce(state: AppState, action: Any): AppState {
        return when (action) {
            is DonationViewAction -> {
                updateState(state, action)
            }
            else -> {
                state
            }
        }
    }

    private fun updateState(state: AppState, action: DonationViewAction): AppState {
        val screenState = (state.uiState.currentView as DonationScreen).screenState
        return state.copy(uiState = state
                .uiState
                .copy(backStack = state
                        .uiState
                        .backStack
                        .subList(0, state.uiState.backStack.size - 1) +
                        DonationScreen(screenState.handleAction(action))))
    }

}