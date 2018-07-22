package com.exp.carconnect.app.fragment

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.app.ProgressDialog
import android.arch.lifecycle.*
import android.content.Context
import android.content.pm.ActivityInfo
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
import com.android.billingclient.api.BillingClient.BillingResponse
import com.exp.carconnect.app.R
import com.exp.carconnect.app.state.DonationAction
import com.exp.carconnect.app.state.DonationScreen
import com.exp.carconnect.app.state.DonationScreenState
import com.exp.carconnect.app.state.Product
import com.exp.carconnect.base.AppState
import com.exp.carconnect.base.BaseAppContract
import com.exp.carconnect.base.asCustomObservable
import com.exp.carconnect.base.state.CommonAppAction
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.donation_view.*
import redux.api.Reducer


internal class DonationView : Fragment() {
    private lateinit var viewModel: DonationVM
    private var progressDialog: ProgressDialog? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.donation_view, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(DonationVM::class.java)
        donate_button.setOnClickListener {
            viewModel.startDonationFlow(activity, product_list.selectedItem as Product)

        }
        viewModel.getScreenStateLiveData().observe(this, Observer {
            onNewState(it!!)
        })
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
                showError(it.errorMessage, it.errorCode)
            }

            is DonationScreenState.ShowDonatedMessage -> {
                hideLoading()
                startDonatedAnimation()
            }
        }
    }

    private fun startDonatedAnimation() {
        val postTransitionConstraintLayout = activity.layoutInflater.inflate(R.layout.donation_view_post_transition, null) as ConstraintLayout
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


    private fun showError(errorMessage: String, errorCode: Int) {
        AlertDialog
                .Builder(activity)
                .setTitle(getString(R.string.donation_error_title))
                .setMessage(errorMessage)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { dialog, which ->
                    viewModel.onErrorAcknowledged(errorCode)
                    dialog.dismiss()
                }
                .create()
                .show()
    }

    private fun showProducts(products: List<Product>) {
        product_list.adapter = ArrayAdapter<Product>(activity, R.layout.donation_item, products)
    }

    private fun showLoading() {
        progressDialog = ProgressDialog.show(activity, activity.getString(R.string.loading), activity.getString(R.string.please_wait))
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
                "carconnectdonationproduct2", "carconnectdonationproduct3")
    }

    init {
        disposables.add(store
                .asCustomObservable()
                .filter { it.uiState.currentView is DonationScreen }
                .map { (it.uiState.currentView as DonationScreen).screenState }
                .distinctUntilChanged()
                .subscribe {
                    donationViewScreenStateLiveData.value = it
                })

        setupBilling()
    }

    private fun setupBilling() {
        billingClient = BillingClient.newBuilder(getApplication()).setListener(this).build()
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(@BillingClient.BillingResponse billingResponseCode: Int) {
                if (billingResponseCode == BillingClient.BillingResponse.OK) {
                    // The billing client is ready. You can query purchases here.
                    queryProducts()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                handlePurchaseTerminated(getApplication<Application>()
                        .getString(R.string.donation_service_disconnected_message),
                        BillingResponse.SERVICE_DISCONNECTED)
            }
        })
    }

    override fun onPurchasesUpdated(responseCode: Int, purchases: MutableList<Purchase>?) {
        if (responseCode === BillingResponse.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else {
            // Handle any other error codes.
            handleUnSuccessfulResponse(responseCode)
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        store.dispatch(DonationAction.AddNewDonationState(DonationScreenState.ShowDonatedMessage))
    }

    private fun handlePurchaseTerminated(message: String, errorCode: Int) {
        store.dispatch(DonationAction.AddNewDonationState(DonationScreenState.ShowError(message, errorCode)))
    }

    private fun queryProducts() {
        val skuList = ArrayList<String>()
        skuList.addAll(DONATION_PRODUCT_IDs)
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)
        billingClient.querySkuDetailsAsync(params.build(), { responseCode, skuDetailsList ->
            if (responseCode == BillingResponse.OK && skuDetailsList != null) {
                val products = ArrayList<Product>()
                for (skuDetails in skuDetailsList) {
                    products.add(Product(skuDetails.price, skuDetails.priceCurrencyCode, skuDetails.title, skuDetails.sku))
                }

                store.dispatch(DonationAction.AddNewDonationState(DonationScreenState.ShowProducts(products)))
            }

        })
    }

    fun startDonationFlow(activity: Activity, product: Product) {
        val flowParams = BillingFlowParams.newBuilder()
                .setSku(product.id)
//                .setSku("android.test.purchased") // test product id. https://developer.android.com/google/play/billing/billing_testing
//                .setSku("android.test.canceled") // test product id. https://developer.android.com/google/play/billing/billing_testing
//                .setSku("android.test.item_unavailable") // test product id. https://developer.android.com/google/play/billing/billing_testing
                .setType(BillingClient.SkuType.INAPP) // SkuType.SUB for subscription
                .build()
        val responseCode = billingClient.launchBillingFlow(activity, flowParams)
        if (responseCode != BillingResponse.OK) {
            handleUnSuccessfulResponse(responseCode)
        }
    }

    //message uses unicode defined here http://wrttn.me/30dbfd/
    private fun handleUnSuccessfulResponse(responseCode: Int) {
        when (responseCode) {
            BillingResponse.BILLING_UNAVAILABLE -> {
                handlePurchaseTerminated(getApplication<Application>().getString(R.string.donation_feature_not_available_message), responseCode)
            }
            BillingResponse.DEVELOPER_ERROR -> {
                handlePurchaseTerminated(getApplication<Application>().getString(R.string.donation_feature_not_available_message), responseCode)
            }
            BillingResponse.ITEM_ALREADY_OWNED -> {
                handlePurchaseTerminated(getApplication<Application>().getString(R.string.donation_item_already_owned_message), responseCode)
            }
            BillingResponse.ITEM_NOT_OWNED -> {
                handlePurchaseTerminated(getApplication<Application>().getString(R.string.donation_item_not_owned_message), responseCode)
            }

            BillingResponse.ITEM_UNAVAILABLE -> {
                handlePurchaseTerminated(getApplication<Application>().getString(R.string.donation_item_unavailable_message), responseCode)
            }

            BillingResponse.FEATURE_NOT_SUPPORTED -> {
                handlePurchaseTerminated(getApplication<Application>().getString(R.string.donation_feature_not_available_message), responseCode)
            }

            BillingResponse.SERVICE_DISCONNECTED -> {
                handlePurchaseTerminated(getApplication<Application>().getString(R.string.donation_service_disconnected_message), responseCode)
            }
            BillingResponse.SERVICE_UNAVAILABLE -> {
                handlePurchaseTerminated(getApplication<Application>().getString(R.string.donation_network_error_message), responseCode)
            }
            BillingResponse.USER_CANCELED -> {
                handlePurchaseTerminated(getApplication<Application>().getString(R.string.donation_user_canceled_message), responseCode)
            }
            BillingResponse.ERROR -> {
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

    fun onErrorAcknowledged(errorCode: Int) {
        when (errorCode) {
            BillingResponse.BILLING_UNAVAILABLE,
            BillingResponse.DEVELOPER_ERROR,
            BillingResponse.ITEM_ALREADY_OWNED,
            BillingResponse.ITEM_NOT_OWNED,
            BillingResponse.FEATURE_NOT_SUPPORTED,
            BillingResponse.SERVICE_DISCONNECTED -> {
                store.dispatch(CommonAppAction.FinishCurrentView)
            }
        }
    }

}

class DonationScreenStateReducer : Reducer<AppState> {
    override fun reduce(state: AppState, action: Any): AppState {
        return when (action) {
            is DonationAction.AddNewDonationState -> {
                updateState(state, action.state)
            }
            else -> {
                state
            }
        }
    }

    private fun updateState(state: AppState, reportScreenState: DonationScreenState): AppState {
        //todo maintain products if available in state if changing state to error, this will be required once error is acknowledged and view is being restored from scratch
        return state.copy(uiState = state
                .uiState
                .copy(backStack = state
                        .uiState
                        .backStack
                        .subList(0, state.uiState.backStack.size - 1) +
                        DonationScreen(reportScreenState)))
    }

}