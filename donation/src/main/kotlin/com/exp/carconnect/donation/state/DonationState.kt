package com.exp.carconnect.donation.state

import com.android.billingclient.api.BillingClient
import com.exp.carconnect.base.CarConnectIndividualViewState
import com.exp.carconnect.base.CarConnectView
import com.exp.carconnect.base.ModuleState

data class DonationModuleState(val donated: Boolean = false) : ModuleState

data class Product(val price: String, val currencyCode: String, val name: String, val id: String) {
    override fun toString(): String {
        return price
    }
}

data class DonationScreen(override val screenState: DonationScreenState) : CarConnectView

sealed class DonationScreenState : CarConnectIndividualViewState {
    abstract fun handleAction(action: DonationAction): DonationScreenState

    object ShowLoading : DonationScreenState() {
        override fun handleAction(action: DonationAction): DonationScreenState {
            return when (action) {
                is DonationAction.ShowProducts -> {
                    ShowProducts(action.products)
                }
                is DonationAction.ShowError -> {
                    ShowError(action.message, action.errorCode)
                }
                else -> {
                    this
                }
            }
        }
    }

    data class ShowProducts(val products: List<Product>) : DonationScreenState() {
        override fun handleAction(action: DonationAction): DonationScreenState {
            return when (action) {
                is DonationAction.StartPaymentFlow -> {
                    StartPaymentFlow(action.selectedProduct, products)
                }
                else -> {
                    this
                }
            }
        }
    }

    data class StartPaymentFlow(val selectedProduct: Product, val allProducts: List<Product>) : DonationScreenState() {
        override fun handleAction(action: DonationAction): DonationScreenState {
            return when (action) {
                is DonationAction.PaymentSuccessful -> {
                    ShowDonatedMessage
                }
                is DonationAction.ShowError -> {
                    ShowError(action.message, action.errorCode, allProducts)
                }
                else -> {
                    this
                }
            }
        }
    }

    data class ShowError(val errorMessage: String, val errorCode: Int, val products: List<Product>? = null) : DonationScreenState() {
        override fun handleAction(action: DonationAction): DonationScreenState {
            return when (action) {
                is DonationAction.ErrorAcknowledged -> {
                    when (action.errorCode) {
                        BillingClient.BillingResponse.BILLING_UNAVAILABLE,
                        BillingClient.BillingResponse.DEVELOPER_ERROR,
                        BillingClient.BillingResponse.ITEM_ALREADY_OWNED,
                        BillingClient.BillingResponse.ITEM_NOT_OWNED,
                        BillingClient.BillingResponse.FEATURE_NOT_SUPPORTED,
                        BillingClient.BillingResponse.SERVICE_DISCONNECTED -> {
                            FinishDonationView
                        }
                        else -> {
                            if (null != products) {
                                ShowProducts(products)
                            } else {
                                FinishDonationView
                            }
                        }
                    }
                }
                else -> {
                    this
                }
            }
        }
    }

    object ShowDonatedMessage : DonationScreenState() {
        override fun handleAction(action: DonationAction): DonationScreenState {
            return this
        }
    }

    object FinishDonationView : DonationScreenState() {
        override fun handleAction(action: DonationAction): DonationScreenState {
            return this
        }
    }
}