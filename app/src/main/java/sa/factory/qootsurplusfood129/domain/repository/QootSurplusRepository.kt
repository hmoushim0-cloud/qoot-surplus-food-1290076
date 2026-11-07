package sa.factory.qootsurplusfood129.domain.repository

import sa.factory.qootsurplusfood129.model.QootSurplusItem
import sa.factory.qootsurplusfood129.model.QootSurplusOrder
import sa.factory.qootsurplusfood129.model.QootSurplusTelemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface QootSurplusRepository {
    val catalogFlow: Flow<List<QootSurplusItem>>
    val activeCart: StateFlow<List<QootSurplusItem>>
    val telemetryFlow: Flow<QootSurplusTelemetry>
    
    fun addToCart(item: QootSurplusItem)
    fun removeFromCart(item: QootSurplusItem)
    fun clearCart()
    suspend fun checkout(paymentMethod: String): QootSurplusOrder
    suspend fun refreshCatalog()
}
