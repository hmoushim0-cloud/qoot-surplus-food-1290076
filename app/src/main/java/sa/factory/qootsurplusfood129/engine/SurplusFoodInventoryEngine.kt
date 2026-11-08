package sa.factory.qootsurplusfood129.engine

import java.util.UUID

enum class FoodCategory(val labelArabic: String) {
    COOKED_MEALS("وجبات ساخنة مطبوخة"),
    BAKERY_PASTRY("مخبوزات ومعجنات طازجة"),
    DAIRY_COLD("ألبان ومبردات"),
    FRESH_PRODUCE("فواكه وخضروات طازجة"),
    PREPARED_SALADS("سلطات ومقبلات جاهزة")
}

enum class ColdChainRequirement(
    val minSafeTempC: Double,
    val maxSafeTempC: Double,
    val descriptionArabic: String
) {
    HOT_HOLDING(60.0, 95.0, "حفظ ساخن فوق 60 درجة"),
    REFRIGERATED(0.0, 4.5, "تبريد هوائي بين 0 و 4.5 درجات"),
    FROZEN(-25.0, -18.0, "تجميد عميق تحت -18 درجة"),
    AMBIENT(15.0, 25.0, "درجة حرارة الغرفة المعتدلة")
}

enum class FreshnessUrgency(val level: Int, val labelArabic: String) {
    STABLE(1, "مستقر - استهلاك آمن"),
    WARNING(2, "تنبيه - أقل من 6 ساعات"),
    CRITICAL(3, "حرج - أقل من ساعتين"),
    EXPIRED(4, "منتهي الصلاحية - للإتلاف")
}

data class SurplusBatch(
    val batchId: String,
    val itemNameArabic: String,
    val category: FoodCategory,
    val quantityUnits: Int,
    val unitWeightGrams: Int,
    val originalPriceSar: Double,
    val storageRequirement: ColdChainRequirement,
    val preparedAtMillis: Long,
    val expiryAtMillis: Long,
    val currentTempC: Double
)

data class SurplusRescueBundle(
    val bundleId: String,
    val titleArabic: String,
    val includedBatchIds: List<String>,
    val originalTotalSar: Double,
    val rescuePriceSar: Double,
    val discountPercent: Int,
    val totalWeightKg: Double,
    val estimatedCo2SavedKg: Double
)

class SurplusFoodInventoryEngine {

    private val inventory = mutableMapOf<String, SurplusBatch>()

    fun registerBatch(batch: SurplusBatch): Boolean {
        if (inventory.containsKey(batch.batchId)) return false
        inventory[batch.batchId] = batch
        return true
    }

    fun getBatch(batchId: String): SurplusBatch? = inventory[batchId]

    fun getAllBatches(): List<SurplusBatch> = inventory.values.toList()

    fun evaluateUrgency(batchId: String, currentEpochMillis: Long): FreshnessUrgency {
        val batch = inventory[batchId] ?: return FreshnessUrgency.EXPIRED
        val remainingMillis = batch.expiryAtMillis - currentEpochMillis
        if (remainingMillis <= 0) return FreshnessUrgency.EXPIRED

        val remainingHours = remainingMillis / (1000.0 * 3600.0)
        return when {
            remainingHours <= 2.0 -> FreshnessUrgency.CRITICAL
            remainingHours <= 6.0 -> FreshnessUrgency.WARNING
            else -> FreshnessUrgency.STABLE
        }
    }

    fun calculateProgressiveDiscount(batchId: String, currentEpochMillis: Long): Int {
        val urgency = evaluateUrgency(batchId, currentEpochMillis)
        return when (urgency) {
            FreshnessUrgency.CRITICAL -> 70
            FreshnessUrgency.WARNING -> 50
            FreshnessUrgency.STABLE -> 30
            FreshnessUrgency.EXPIRED -> 0
        }
    }

    fun calculateDiscountedPrice(batchId: String, currentEpochMillis: Long): Double {
        val batch = inventory[batchId] ?: return 0.0
        val discount = calculateProgressiveDiscount(batchId, currentEpochMillis)
        val discounted = batch.originalPriceSar * (1.0 - (discount / 100.0))
        return Math.round(discounted * 100.0) / 100.0
    }

    fun validateColdChainSafety(batchId: String): Boolean {
        val batch = inventory[batchId] ?: return false
        val req = batch.storageRequirement
        return batch.currentTempC in req.minSafeTempC..req.maxSafeTempC
    }

    fun calculateTotalFoodWasteDivertedKg(): Double {
        val totalGrams = inventory.values.sumOf { it.quantityUnits * it.unitWeightGrams }
        return Math.round((totalGrams / 1000.0) * 100.0) / 100.0
    }

    fun calculateCo2EmissionsAvoidedKg(): Double {
        // SFDA / FAO standard: ~2.5 kg CO2e avoided per 1 kg food waste diverted
        val totalKg = calculateTotalFoodWasteDivertedKg()
        return Math.round(totalKg * 2.5 * 100.0) / 100.0
    }

    fun generateRescueBundle(
        bundleTitleArabic: String,
        targetBatchIds: List<String>,
        currentEpochMillis: Long
    ): SurplusRescueBundle {
        val selectedBatches = targetBatchIds.mapNotNull { inventory[it] }
        val originalTotal = selectedBatches.sumOf { it.originalPriceSar }
        val discountedTotal = targetBatchIds.sumOf { calculateDiscountedPrice(it, currentEpochMillis) }

        val totalWeightGrams = selectedBatches.sumOf { it.unitWeightGrams }
        val totalWeightKg = Math.round((totalWeightGrams / 1000.0) * 100.0) / 100.0
        val co2Saved = Math.round(totalWeightKg * 2.5 * 100.0) / 100.0

        val discountPct = if (originalTotal > 0) {
            Math.round(((originalTotal - discountedTotal) / originalTotal) * 100.0).toInt()
        } else 0

        return SurplusRescueBundle(
            bundleId = "BND-${UUID.randomUUID().toString().take(8).uppercase()}",
            titleArabic = bundleTitleArabic,
            includedBatchIds = targetBatchIds,
            originalTotalSar = Math.round(originalTotal * 100.0) / 100.0,
            rescuePriceSar = Math.round(discountedTotal * 100.0) / 100.0,
            discountPercent = discountPct,
            totalWeightKg = totalWeightKg,
            estimatedCo2SavedKg = co2Saved
        )
    }
}
