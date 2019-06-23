package com.exp.carconnect.donation.state

import com.android.billingclient.api.BillingClient
import com.exp.carconnect.base.*
import com.exp.carconnect.donation.state.DonationModuleState.Companion.DONATION_STATE_KEY


fun AppState.copyAndReplaceDonationState(state: LoadableState<DonationModuleState, Throwable>): AppState {
    return this.copy(moduleStateMap = moduleStateMap + Pair(DONATION_STATE_KEY, state))
}

fun AppState.isDonationStateLoaded(): Boolean {
    return this.moduleStateMap[DONATION_STATE_KEY] is LoadableState.Loaded
}


fun AppState.getDonationState(): DonationState {
    return ((this.moduleStateMap[DONATION_STATE_KEY] as LoadableState.Loaded).savedState as DonationModuleState)
            .donationPersistedState.state
}

fun AppState.hasUserDonated(): Boolean {
    return getDonationState() is DonationState.Donated
}

data class DonationModuleState(val donationPersistedState: DonationPersistedState) : ModuleState {
    companion object {
        const val DONATION_STATE_KEY = "DONATION_STATE_KEY"
    }
}

data class DonationPersistedState(val state: DonationState = DonationState.NotDonated)

sealed class DonationState {
    object NotDonated : DonationState()
    data class Donated(val productId: String,
                       val orderId: String,
                       val purchaseTime: Long,
                       val purchaseToken: String) : DonationState()
}

data class Product(val price: String, val currencyCode: String, val name: String, val id: String) {
    override fun toString(): String {
        return price
    }
}

data class DonationScreen(override val screenState: DonationScreenState) : CarConnectView

sealed class DonationScreenState : CarConnectIndividualViewState {
    abstract fun handleAction(action: DonationViewAction): DonationScreenState

    object ShowLoading : DonationScreenState() {
        override fun handleAction(action: DonationViewAction): DonationScreenState {
            return when (action) {
                is DonationViewAction.ShowProducts -> {
                    ShowProducts(action.products)
                }
                is DonationViewAction.ShowError -> {
                    ShowError(action.message, action.errorCode)
                }
                else -> {
                    this
                }
            }
        }
    }

    data class ShowProducts(val products: List<Product>) : DonationScreenState() {
        override fun handleAction(action: DonationViewAction): DonationScreenState {
            return when (action) {
                is DonationViewAction.StartPaymentFlow -> {
                    StartPaymentFlow(action.selectedProduct, products)
                }
                else -> {
                    this
                }
            }
        }
    }

    data class StartPaymentFlow(val selectedProduct: Product, val allProducts: List<Product>) : DonationScreenState() {
        override fun handleAction(action: DonationViewAction): DonationScreenState {
            return when (action) {
                is DonationViewAction.PaymentSuccessful -> {
                    ShowDonatedMessage
                }
                is DonationViewAction.ShowError -> {
                    ShowError(action.message, action.errorCode, allProducts)
                }
                else -> {
                    this
                }
            }
        }
    }

    data class ShowError(val errorMessage: String, val errorCode: Int, val products: List<Product>? = null) : DonationScreenState() {
        override fun handleAction(action: DonationViewAction): DonationScreenState {
            return when (action) {
                is DonationViewAction.ErrorAcknowledged -> {
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
        override fun handleAction(action: DonationViewAction): DonationScreenState {
            return this
        }
    }

    object FinishDonationView : DonationScreenState() {
        override fun handleAction(action: DonationViewAction): DonationScreenState {
            return this
        }
    }
}