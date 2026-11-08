package sa.factory.qootsurplusfood129

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import sa.factory.qootsurplusfood129.data.repository.QootSurplusRepositoryImpl
import sa.factory.qootsurplusfood129.engine.*
import sa.factory.qootsurplusfood129.hardware.HardwareSensorDriver
import sa.factory.qootsurplusfood129.model.QootSurplusItem

class QootSurplusRegressionTest {

    @Test
    fun testCheckoutCalculationAndVat15() = runBlocking {
        val sensorDriver = HardwareSensorDriver()
        val repository = QootSurplusRepositoryImpl(sensorDriver)

        val item1 = QootSurplusItem("t1", "وجبة إنقاذ دجاج شواية", "طازجة من خط الطهي", 100.0, "معتمد", "وجبات جاهزة")
        val item2 = QootSurplusItem("t2", "سلة معجنات متنوعة", "فائض مخابز الصباح", 200.0, "معتمد", "فائض المخابز")

        repository.addToCart(item1)
        repository.addToCart(item2)

        val order = repository.checkout("Mada Contactless EMV")

        assertEquals(300.0, order.subtotalSar, 0.01)
        assertEquals(45.0, order.vatSar, 0.01)
        assertEquals(345.0, order.totalSar, 0.01)
        assertEquals(0, repository.activeCart.value.size)
        assertTrue(order.orderId.startsWith("ORD-"))
    }

    @Test
    fun testKdsOrderRegistrationAndLookup() {
        val engine = KitchenDisplaySystemProtocolEngine()
        val ticket = KdsOrderTicket(
            ticketId = "TCK-101",
            orderNumber = 10,
            tableOrPickupRef = "TABLE-04",
            priority = KdsOrderPriority.NORMAL,
            items = listOf(
                KdsOrderItem("it1", "شاورما لحم عربي", 1, KdsStation.HOT_LINE),
                KdsOrderItem("it2", "عصير برتقال طازج", 1, KdsStation.DRINKS)
            )
        )

        assertTrue(engine.registerOrder(ticket))
        assertFalse("Duplicate ticket ID registration should fail", engine.registerOrder(ticket))

        val retrieved = engine.getTicket("TCK-101")
        assertNotNull(retrieved)
        assertEquals(10, retrieved?.orderNumber)
        assertEquals(2, retrieved?.items?.size)
    }

    @Test
    fun testKdsStatusTransition() {
        val engine = KitchenDisplaySystemProtocolEngine()
        val ticket = KdsOrderTicket(
            ticketId = "TCK-102",
            orderNumber = 11,
            tableOrPickupRef = "DRIVE-THRU-1",
            priority = KdsOrderPriority.RUSH,
            items = listOf(KdsOrderItem("it1", "برجر حاشي دبل", 2, KdsStation.GRILL))
        )
        engine.registerOrder(ticket)
        assertEquals(KdsTicketStatus.QUEUED, engine.getTicket("TCK-102")?.status)

        assertTrue(engine.updateStatus("TCK-102", KdsTicketStatus.COOKING))
        assertEquals(KdsTicketStatus.COOKING, engine.getTicket("TCK-102")?.status)

        assertTrue(engine.updateStatus("TCK-102", KdsTicketStatus.READY_FOR_PICKUP))
        assertEquals(KdsTicketStatus.READY_FOR_PICKUP, engine.getTicket("TCK-102")?.status)
    }

    @Test
    fun testKdsStationFiltering() {
        val engine = KitchenDisplaySystemProtocolEngine()
        val ticket = KdsOrderTicket(
            ticketId = "TCK-103",
            orderNumber = 12,
            tableOrPickupRef = "TABLE-09",
            priority = KdsOrderPriority.NORMAL,
            items = listOf(
                KdsOrderItem("it1", "مشاوي مشكلة عائلية", 1, KdsStation.GRILL),
                KdsOrderItem("it2", "سلطة فتوش بدبس الرمان", 2, KdsStation.COLD_PREP),
                KdsOrderItem("it3", "متبل باذنجان مشوي", 1, KdsStation.COLD_PREP)
            )
        )

        val grillItems = engine.filterItemsByStation(ticket, KdsStation.GRILL)
        val coldItems = engine.filterItemsByStation(ticket, KdsStation.COLD_PREP)
        val drinkItems = engine.filterItemsByStation(ticket, KdsStation.DRINKS)

        assertEquals(1, grillItems.size)
        assertEquals("مشاوي مشكلة عائلية", grillItems[0].nameArabic)
        assertEquals(2, coldItems.size)
        assertEquals(0, drinkItems.size)
    }

    @Test
    fun testKdsEscPosTicketRendering() {
        val engine = KitchenDisplaySystemProtocolEngine()
        val ticket = KdsOrderTicket(
            ticketId = "TCK-104",
            orderNumber = 77,
            tableOrPickupRef = "PICKUP-COUNTER",
            priority = KdsOrderPriority.SURPLUS_RESCUE,
            items = listOf(
                KdsOrderItem("it1", "كبسة حاشي مع الأرز", 1, KdsStation.HOT_LINE, "فائض طازج ساخن", true)
            )
        )

        val bytes = engine.renderEscPosKitchenTicket(ticket, KdsStation.HOT_LINE)
        assertTrue("ESC/POS ticket bytes should not be empty", bytes.isNotEmpty())

        // Check ESC @ init byte header (0x1B, 0x40)
        assertEquals(0x1B.toByte(), bytes[0])
        assertEquals(0x40.toByte(), bytes[1])

        // Check GS V cut bytes at the end
        val lastIdx = bytes.size - 1
        assertEquals(0x03.toByte(), bytes[lastIdx])     // feed lines count
        assertEquals(0x41.toByte(), bytes[lastIdx - 1]) // Mode A
        assertEquals(0x56.toByte(), bytes[lastIdx - 2]) // 'V'
        assertEquals(0x1D.toByte(), bytes[lastIdx - 3]) // GS
    }

    @Test
    fun testKdsTcpSocketFramePackagingAndChecksum() {
        val engine = KitchenDisplaySystemProtocolEngine()
        val dummyPayload = "KDS-HEARTBEAT-PING".toByteArray()

        val frame = engine.packageTcpSocketFrame("HOT", dummyPayload)
        assertEquals(0x02.toByte(), frame.magicHeader)
        assertEquals(0x03.toByte(), frame.magicTail)
        assertEquals("HOT", frame.stationCode)
        assertEquals(dummyPayload.size.toShort(), frame.payloadLength)

        assertTrue("Generated socket frame must pass verification", engine.verifySocketFrame(frame))

        // Corrupted frame verification
        val corruptedFrame = frame.copy(checksumXor = (frame.checksumXor + 1).toByte())
        assertFalse("Tampered checksum must fail verification", engine.verifySocketFrame(corruptedFrame))
    }

    @Test
    fun testSurplusFreshnessUrgencyEvaluation() {
        val engine = SurplusFoodInventoryEngine()
        val now = 1000000000L

        // Batch expiring in 1 hour -> CRITICAL
        engine.registerBatch(
            SurplusBatch("b1", "وجبة أرز ولحم", FoodCategory.COOKED_MEALS, 5, 500, 30.0,
                ColdChainRequirement.HOT_HOLDING, now - 3600000, now + 3600000, 65.0)
        )
        // Batch expiring in 4 hours -> WARNING
        engine.registerBatch(
            SurplusBatch("b2", "معجنات جبن وفطائر", FoodCategory.BAKERY_PASTRY, 10, 300, 20.0,
                ColdChainRequirement.AMBIENT, now - 3600000, now + (4 * 3600000), 22.0)
        )
        // Batch expiring in 12 hours -> STABLE
        engine.registerBatch(
            SurplusBatch("b3", "زبادي طازج عضوي", FoodCategory.DAIRY_COLD, 8, 200, 15.0,
                ColdChainRequirement.REFRIGERATED, now - 3600000, now + (12 * 3600000), 3.5)
        )
        // Expired batch
        engine.registerBatch(
            SurplusBatch("b4", "سلطة يونانية", FoodCategory.PREPARED_SALADS, 2, 250, 18.0,
                ColdChainRequirement.REFRIGERATED, now - 7200000, now - 1000, 4.0)
        )

        assertEquals(FreshnessUrgency.CRITICAL, engine.evaluateUrgency("b1", now))
        assertEquals(FreshnessUrgency.WARNING, engine.evaluateUrgency("b2", now))
        assertEquals(FreshnessUrgency.STABLE, engine.evaluateUrgency("b3", now))
        assertEquals(FreshnessUrgency.EXPIRED, engine.evaluateUrgency("b4", now))
    }

    @Test
    fun testProgressiveDiscountModel() {
        val engine = SurplusFoodInventoryEngine()
        val now = 2000000000L

        engine.registerBatch(
            SurplusBatch("crit", "وجبة برياني دجاج", FoodCategory.COOKED_MEALS, 4, 600, 50.0,
                ColdChainRequirement.HOT_HOLDING, now - 3600000, now + 3600000, 68.0)
        )
        engine.registerBatch(
            SurplusBatch("warn", "خبز تميس بالسمسم", FoodCategory.BAKERY_PASTRY, 6, 250, 10.0,
                ColdChainRequirement.AMBIENT, now - 3600000, now + (5 * 3600000), 20.0)
        )

        assertEquals(70, engine.calculateProgressiveDiscount("crit", now))
        assertEquals(15.0, engine.calculateDiscountedPrice("crit", now), 0.01)

        assertEquals(50, engine.calculateProgressiveDiscount("warn", now))
        assertEquals(5.0, engine.calculateDiscountedPrice("warn", now), 0.01)
    }

    @Test
    fun testColdChainTemperatureValidation() {
        val engine = SurplusFoodInventoryEngine()
        val now = 3000000000L

        // Valid Hot holding (64.0°C within 60.0 - 95.0°C)
        engine.registerBatch(
            SurplusBatch("hot-ok", "مضغوط لحم حاشي", FoodCategory.COOKED_MEALS, 3, 700, 45.0,
                ColdChainRequirement.HOT_HOLDING, now, now + 10000, 64.0)
        )
        // Dangerous Hot holding (48.0°C < 60.0°C -> Danger zone)
        engine.registerBatch(
            SurplusBatch("hot-bad", "مضغوط لحم بارد", FoodCategory.COOKED_MEALS, 3, 700, 45.0,
                ColdChainRequirement.HOT_HOLDING, now, now + 10000, 48.0)
        )
        // Valid Refrigeration (3.2°C within 0.0 - 4.5°C)
        engine.registerBatch(
            SurplusBatch("cold-ok", "لبن عيران بارد", FoodCategory.DAIRY_COLD, 10, 330, 8.0,
                ColdChainRequirement.REFRIGERATED, now, now + 10000, 3.2)
        )
        // Dangerous Refrigeration (8.5°C > 4.5°C -> Spoiled)
        engine.registerBatch(
            SurplusBatch("cold-bad", "لبن دافئ", FoodCategory.DAIRY_COLD, 10, 330, 8.0,
                ColdChainRequirement.REFRIGERATED, now, now + 10000, 8.5)
        )

        assertTrue("Safe hot food must validate", engine.validateColdChainSafety("hot-ok"))
        assertFalse("Dangerous warm food must fail validation", engine.validateColdChainSafety("hot-bad"))
        assertTrue("Properly refrigerated food must validate", engine.validateColdChainSafety("cold-ok"))
        assertFalse("Broken cold chain must fail validation", engine.validateColdChainSafety("cold-bad"))
    }

    @Test
    fun testSurplusRescueBundleAndImpactMetrics() = runBlocking {
        val driver = HardwareSensorDriver()
        val engine = driver.surplusEngine
        val now = System.currentTimeMillis()

        val bundle = engine.generateRescueBundle(
            bundleTitleArabic = "صندوق إنقاذ العشاء المميز",
            targetBatchIds = listOf("QOOT-B101", "QOOT-B102"),
            currentEpochMillis = now
        )

        assertNotNull(bundle)
        assertTrue(bundle.bundleId.startsWith("BND-"))
        assertEquals("صندوق إنقاذ العشاء المميز", bundle.titleArabic)
        assertEquals(2, bundle.includedBatchIds.size)
        assertEquals(83.0, bundle.originalTotalSar, 0.01) // 48.0 + 35.0
        assertTrue("Rescue price must be discounted", bundle.rescuePriceSar < bundle.originalTotalSar)
        assertTrue("Discount percentage should be > 0", bundle.discountPercent > 0)
        assertEquals(1.25, bundle.totalWeightKg, 0.01) // (850g + 400g) / 1000
        assertEquals(3.13, bundle.estimatedCo2SavedKg, 0.01) // 1.25 * 2.5

        // Verify driver flow emissions
        val initialFrame = driver.observePeripheralStream().first()
        assertEquals("Kitchen Display System (KDS) TCP/IP Print Server", initialFrame.peripheralName)
        assertEquals("CONNECTING", initialFrame.connectionState)
    }
}
