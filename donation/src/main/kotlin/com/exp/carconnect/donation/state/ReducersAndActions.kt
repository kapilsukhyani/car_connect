package com.exp.carconnect.donation.state

sealed class DonationAction {
    data class ShowProducts(val products: List<Product>) : DonationAction()
    data class ShowError(val message: String, val errorCode: Int) : DonationAction()
    data class StartPaymentFlow(val selectedProduct: Product) : DonationAction()
    object PaymentSuccessful : DonationAction()
    data class ErrorAcknowledged(val errorCode: Int) : DonationAction()

}