package sa.factory.qootsurplusfood129.domain.usecase

import sa.factory.qootsurplusfood129.domain.repository.QootSurplusRepository
import sa.factory.qootsurplusfood129.model.QootSurplusItem
import sa.factory.qootsurplusfood129.model.QootSurplusOrder
import sa.factory.qootsurplusfood129.model.QootSurplusTelemetry
import kotlinx.coroutines.flow.Flow

class GetCatalogUseCase(private val repository: QootSurplusRepository) {
    operator fun invoke(): Flow<List<QootSurplusItem>> = repository.catalogFlow
}

class ManageCartUseCase(private val repository: QootSurplusRepository) {
    val cart = repository.activeCart
    fun add(item: QootSurplusItem) = repository.addToCart(item)
    fun remove(item: QootSurplusItem) = repository.removeFromCart(item)
    fun clear() = repository.clearCart()
}

class ProcessCheckoutUseCase(private val repository: QootSurplusRepository) {
    suspend operator fun invoke(paymentMethod: String = "Mada Contactless EMV"): QootSurplusOrder =
        repository.checkout(paymentMethod)
}

class ObserveHardwareTelemetryUseCase(private val repository: QootSurplusRepository) {
    operator fun invoke(): Flow<QootSurplusTelemetry> = repository.telemetryFlow
}
