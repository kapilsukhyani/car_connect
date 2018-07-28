package com.exp.carconnect.donation.state

import com.exp.carconnect.base.AppState
import com.exp.carconnect.base.LoadableState
import com.exp.carconnect.base.state.BaseAppAction
import redux.api.Reducer

sealed class DonationAction {
    object LoadingDonationState : DonationAction()
    data class LoadedDonationPersistedState(val state: DonationPersistedState) : DonationAction()
    data class DonationStateLoadingError(val error: Throwable) : DonationAction()

    data class StoreDonation(val productId: String,
                             val orderId: String,
                             val purchaseTime: Long,
                             val purchaseToken: String) : DonationAction()

    object UpdatingDonationStatus : DonationAction()
    data class UpdateStateToDonated(val donated: DonationState.Donated) : DonationAction()
    data class UpdateDonationError(val error: Throwable) : DonationAction()


}

sealed class DonationViewAction {
    data class ShowProducts(val products: List<Product>) : DonationViewAction()
    data class ShowError(val message: String, val errorCode: Int) : DonationViewAction()
    data class StartPaymentFlow(val selectedProduct: Product) : DonationViewAction()
    object PaymentSuccessful : DonationViewAction()
    data class ErrorAcknowledged(val errorCode: Int) : DonationViewAction()
}

class DonationModuleStateReducer : Reducer<AppState> {
    override fun reduce(state: AppState, action: Any?): AppState {
        return when (action) {
            DonationAction.LoadingDonationState -> {
                state.copyAndReplaceDonationState(LoadableState.Loading)
            }
            is DonationAction.LoadedDonationPersistedState -> {
                state.copyAndReplaceDonationState(LoadableState.Loaded(DonationModuleState(action.state)))
            }

            is BaseAppAction.BaseAppStateLoadError -> {
                state.copyAndReplaceDonationState(LoadableState.LoadingError(action.error))
            }

            is DonationAction.UpdateStateToDonated -> {
                state.copyAndReplaceDonationState(LoadableState.Loaded(DonationModuleState(DonationPersistedState(action.donated))))
            }
            else -> {
                state
            }
        }
    }

}


fun AppState.copyAndReplaceDonationState(state: LoadableState<DonationModuleState, Throwable>): AppState {
    return this.copy(moduleStateMap = moduleStateMap + Pair(DonationModuleState.DONATION_STATE_KEY, state))
}