package sa.factory.qootsurplusfood129.data.repository

import sa.factory.qootsurplusfood129.domain.repository.QootSurplusRepository
import sa.factory.qootsurplusfood129.hardware.HardwareSensorDriver
import sa.factory.qootsurplusfood129.model.QootSurplusItem
import sa.factory.qootsurplusfood129.model.QootSurplusOrder
import sa.factory.qootsurplusfood129.model.QootSurplusTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class QootSurplusRepositoryImpl(
    private val sensorDriver: HardwareSensorDriver
) : QootSurplusRepository {

    private val _catalog = MutableStateFlow<List<QootSurplusItem>>(listOf(
        QootSurplusItem("1", "الباقة التنفيذية المعتمدة", "تكامل مباشر مع Kitchen Display System (KDS) TCP/IP Print Server", 180.0, "معتمد", "الخدمات الرئيسية"),
        QootSurplusItem("2", "خدمة المعالجة الفورية", "أداء أصيل بسرعة 60 إطار في الثانية", 95.0, "فوري", "المراقبة الحية"),
        QootSurplusItem("3", "باقة الصيانة والدعم السحابي", "تشفير كامل للبيانات المحلية ومزامنة آمنة", 250.0, "شامل", "التقارير الضريبية")
    ))
    override val catalogFlow: Flow<List<QootSurplusItem>> = _catalog.asStateFlow()

    private val _cart = MutableStateFlow<List<QootSurplusItem>>(emptyList())
    override val activeCart: StateFlow<List<QootSurplusItem>> = _cart.asStateFlow()

    override val telemetryFlow: Flow<QootSurplusTelemetry> = sensorDriver.observePeripheralStream()
        .map { frame ->
            QootSurplusTelemetry(
                peripheralName = frame.peripheralName,
                status = frame.connectionState,
                liveReading = frame.livePayload,
                batteryPct = 96,
                signalDbm = -54
            )
        }

    override fun addToCart(item: QootSurplusItem) {
        val current = _cart.value.toMutableList()
        current.add(item)
        _cart.value = current
    }

    override fun removeFromCart(item: QootSurplusItem) {
        val current = _cart.value.toMutableList()
        current.remove(item)
        _cart.value = current
    }

    override fun clearCart() {
        _cart.value = emptyList()
    }

    override suspend fun checkout(paymentMethod: String): QootSurplusOrder {
        val currentItems = _cart.value.toList()
        val subtotal = currentItems.sumOf { it.priceSar }
        val vat = subtotal * 0.15
        val total = subtotal + vat
        val order = QootSurplusOrder(
            orderId = "ORD-${System.currentTimeMillis() % 100000}",
            items = currentItems,
            subtotalSar = subtotal,
            vatSar = vat,
            totalSar = total,
            paymentMethod = paymentMethod
        )
        clearCart()
        return order
    }

    override suspend fun refreshCatalog() {
        // Offline-first sync logic
    }
}
