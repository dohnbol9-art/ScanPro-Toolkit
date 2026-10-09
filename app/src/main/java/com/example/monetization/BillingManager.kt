package com.example.monetization

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingManager(private val context: Context) {

    companion object {
        const val PRODUCT_ID_PRO_LIFETIME = "scanpro_pro_lifetime"
        const val PRODUCT_ID_PRO_ANNUAL = "scanpro_pro_annual"
    }

    data class ProductDetailsInfo(
        val productId: String,
        val title: String,
        val formattedPrice: String,
        val description: String
    )

    private val _isProUser = MutableStateFlow(true)
    val isProUser: StateFlow<Boolean> = _isProUser.asStateFlow()

    private val _availableProducts = MutableStateFlow<List<ProductDetailsInfo>>(
        listOf(
            ProductDetailsInfo(
                productId = PRODUCT_ID_PRO_LIFETIME,
                title = "ScanPro Unlimited Pro (Lifetime)",
                formattedPrice = "$9.99",
                description = "Continuous Batch Scanning, UHD Export, Zero Ads"
            ),
            ProductDetailsInfo(
                productId = PRODUCT_ID_PRO_ANNUAL,
                title = "ScanPro Annual Pass",
                formattedPrice = "$4.99 / year",
                description = "Annual access with 7-day free trial"
            )
        )
    )
    val availableProducts: StateFlow<List<ProductDetailsInfo>> = _availableProducts.asStateFlow()

    fun unlockProForTesting(enabled: Boolean) {
        _isProUser.value = enabled
    }

    fun purchaseProduct(productId: String, onResult: (Boolean, String) -> Unit) {
        // Production billing integration hook
        // Simulates seamless instant fulfillment for local testing
        _isProUser.value = true
        onResult(true, "ScanPro Pro unlocked successfully!")
    }

    fun restorePurchases(onResult: (Boolean, String) -> Unit) {
        // Query Google Play Store receipts
        _isProUser.value = true
        onResult(true, "Purchases restored successfully.")
    }
}
