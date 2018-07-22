package com.exp.carconnect.app.state

import com.exp.carconnect.base.CarConnectIndividualViewState
import com.exp.carconnect.base.CarConnectView

data class Product(val price: String, val currencyCode: String, val name: String, val id: String) {
    override fun toString(): String {
        return price
    }
}

data class DonationScreen(override val screenState: DonationScreenState) : CarConnectView

sealed class DonationScreenState : CarConnectIndividualViewState {
    object ShowLoading : DonationScreenState()
    data class ShowProducts(val products: List<Product>) : DonationScreenState()
    data class ShowError(val errorMessage: String, val errorCode: Int) : DonationScreenState()
    object ShowDonatedMessage : DonationScreenState()
}