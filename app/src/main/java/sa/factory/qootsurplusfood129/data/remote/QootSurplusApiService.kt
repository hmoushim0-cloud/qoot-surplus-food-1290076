package sa.factory.qootsurplusfood129.data.remote

import sa.factory.qootsurplusfood129.model.QootSurplusItem
import sa.factory.qootsurplusfood129.model.QootSurplusOrder

interface QootSurplusApiService {
    suspend fun fetchCatalog(): List<QootSurplusItem>
    suspend fun submitOrder(order: QootSurplusOrder): Boolean
}
