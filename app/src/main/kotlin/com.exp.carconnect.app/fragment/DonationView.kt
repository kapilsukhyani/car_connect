package com.exp.carconnect.app.fragment

import android.app.Activity
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
import com.exp.carconnect.Logger
import com.exp.carconnect.app.R
import com.exp.carconnect.app.state.DonationScreen
import com.exp.carconnect.app.state.DonationScreenState
import com.exp.carconnect.app.state.Product
import com.exp.carconnect.base.BaseAppContract
import com.exp.carconnect.base.asCustomObservable
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.donation_view.*


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
                showError(it.errorMessage)
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


    private fun showError(errorMessage: String) {
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
        private const val DONATION_PRODUCT_ID = "carconnectdonationproduct"
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
            }
        })
    }

    override fun onPurchasesUpdated(responseCode: Int, purchases: MutableList<Purchase>?) {
        if (responseCode === BillingResponse.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (responseCode === BillingResponse.USER_CANCELED) {
            // Handle an error caused by a user canceling the purchase flow.
            handlePurchaseCanceled()
        } else {
            // Handle any other error codes.
            handlePurchaseCanceled()
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        donationViewScreenStateLiveData.value = DonationScreenState.ShowDonatedMessage
    }

    private fun handlePurchaseCanceled() {
        donationViewScreenStateLiveData.value = DonationScreenState.ShowError("Could not complete transaction")
    }

    private fun queryProducts() {
        val skuList = ArrayList<String>()
        skuList.add(DONATION_PRODUCT_ID)
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)
        billingClient.querySkuDetailsAsync(params.build(), { responseCode, skuDetailsList ->
            if (responseCode == BillingResponse.OK && skuDetailsList != null) {
                val products = ArrayList<Product>()
                for (skuDetails in skuDetailsList) {
                    products.add(Product(skuDetails.price, skuDetails.priceCurrencyCode, skuDetails.title, skuDetails.sku))
                }
                donationViewScreenStateLiveData.value = DonationScreenState.ShowProducts(products)
            }

        })
    }

    fun startDonationFlow(activity: Activity, product: Product) {
        val flowParams = BillingFlowParams.newBuilder()
                .setSku(product.id)
                .setType(BillingClient.SkuType.INAPP) // SkuType.SUB for subscription
                .build()
        val responseCode = billingClient.launchBillingFlow(activity, flowParams)
        if (responseCode != BillingResponse.OK) {
            handlePurchaseCanceled()
        }
    }

    fun getScreenStateLiveData(): LiveData<DonationScreenState> {
        return donationViewScreenStateLiveData
    }

    override fun onCleared() {
        disposables.dispose()
        disposables.clear()
    }

}