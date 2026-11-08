package sa.factory.qootsurplusfood129.hardware

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import sa.factory.qootsurplusfood129.engine.*
import kotlin.random.Random

data class HardwareTelemetryFrame(
    val peripheralName: String,
    val connectionState: String,
    val livePayload: String,
    val timestamp: Long = System.currentTimeMillis()
)

class HardwareSensorDriver {
    private val peripheral = "Kitchen Display System (KDS) TCP/IP Print Server"

    val kdsEngine = KitchenDisplaySystemProtocolEngine()
    val surplusEngine = SurplusFoodInventoryEngine()

    init {
        // Seed default production batches and KDS tickets
        val now = System.currentTimeMillis()

        surplusEngine.registerBatch(
            SurplusBatch(
                batchId = "QOOT-B101",
                itemNameArabic = "وجبة كبسة دجاج شواية عائلية",
                category = FoodCategory.COOKED_MEALS,
                quantityUnits = 12,
                unitWeightGrams = 850,
                originalPriceSar = 48.0,
                storageRequirement = ColdChainRequirement.HOT_HOLDING,
                preparedAtMillis = now - (3600 * 1000 * 2), // 2h ago
                expiryAtMillis = now + (3600 * 1000 * 1),   // 1h left (CRITICAL)
                currentTempC = 64.5
            )
        )

        surplusEngine.registerBatch(
            SurplusBatch(
                batchId = "QOOT-B102",
                itemNameArabic = "سلة كرواسان ومعجنات مخبوزة يدوياً",
                category = FoodCategory.BAKERY_PASTRY,
                quantityUnits = 20,
                unitWeightGrams = 400,
                originalPriceSar = 35.0,
                storageRequirement = ColdChainRequirement.AMBIENT,
                preparedAtMillis = now - (3600 * 1000 * 4),
                expiryAtMillis = now + (3600 * 1000 * 4),   // 4h left (WARNING)
                currentTempC = 21.0
            )
        )

        val sampleTicket = KdsOrderTicket(
            ticketId = "TCK-9901",
            orderNumber = 142,
            tableOrPickupRef = "PICKUP-DOOR-A",
            priority = KdsOrderPriority.SURPLUS_RESCUE,
            items = listOf(
                KdsOrderItem("item-1", "وجبة كبسة دجاج شواية عائلية", 2, KdsStation.HOT_LINE, "إنقاذ فائض سريع", true),
                KdsOrderItem("item-2", "سلطة خضراء طازجة", 2, KdsStation.COLD_PREP, "تغليف محكم", true)
            ),
            targetSlaMinutes = 12
        )
        kdsEngine.registerOrder(sampleTicket)
    }

    fun observePeripheralStream(): Flow<HardwareTelemetryFrame> = flow {
        emit(HardwareTelemetryFrame(peripheral, "CONNECTING", "KDS Socket initializing on 192.168.1.180:9100..."))
        delay(300)
        emit(HardwareTelemetryFrame(peripheral, "ONLINE", "$peripheral: ONLINE | READY (Port 9100)"))

        var frameCounter = 0
        while (true) {
            delay(1500)
            frameCounter++
            val totalSavedKg = surplusEngine.calculateTotalFoodWasteDivertedKg()
            val co2SavedKg = surplusEngine.calculateCo2EmissionsAvoidedKg()
            val activeKds = kdsEngine.getActiveTicketCount()
            val jitter = Random.nextInt(1, 8)

            val payload = "KDS TCP:9100 [OK] | Active Tickets: $activeKds | Diverted Food: ${totalSavedKg}kg | CO2 Saved: ${co2SavedKg}kg (Jitter: +${jitter}ms) #$frameCounter"
            emit(HardwareTelemetryFrame(peripheral, "ACTIVE_STREAMING", payload))
        }
    }
}
