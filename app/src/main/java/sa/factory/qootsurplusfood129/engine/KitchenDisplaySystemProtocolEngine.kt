package sa.factory.qootsurplusfood129.engine

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

enum class KdsStation(val stationCode: String, val stationArabic: String) {
    EXPEDITER("EXP", "محطة التجميع النهائي"),
    HOT_LINE("HOT", "خط الطهي الساخن"),
    GRILL("GRL", "قسم المشاوي"),
    COLD_PREP("CLD", "قسم السلطات والمقبلات"),
    DRINKS("DRK", "محطة المشروبات"),
    PACKAGING("PKG", "قسم التغليف والتعليب"),
    SURPLUS_DISPATCH("SRP", "محطة فائض الأطعمة والإنقاذ")
}

enum class KdsOrderPriority(val priorityLevel: Int, val tag: String) {
    NORMAL(1, "عادي"),
    RUSH(2, "مستعجل"),
    VIP(3, "أولوية قصوى"),
    SURPLUS_RESCUE(4, "إنقاذ فوري")
}

enum class KdsTicketStatus {
    QUEUED,
    COOKING,
    READY_FOR_PICKUP,
    COLLECTED,
    CANCELLED
}

data class KdsOrderItem(
    val itemId: String,
    val nameArabic: String,
    val quantity: Int,
    val station: KdsStation,
    val specialInstructions: String = "",
    val isSurplusRescue: Boolean = false
)

data class KdsOrderTicket(
    val ticketId: String,
    val orderNumber: Int,
    val tableOrPickupRef: String,
    val priority: KdsOrderPriority,
    val items: List<KdsOrderItem>,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val targetSlaMinutes: Int = 15,
    val status: KdsTicketStatus = KdsTicketStatus.QUEUED
)

data class KdsTcpSocketFrame(
    val magicHeader: Byte = 0x02, // STX
    val stationCode: String,
    val payloadLength: Short,
    val payload: ByteArray,
    val checksumXor: Byte,
    val magicTail: Byte = 0x03    // ETX
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as KdsTcpSocketFrame
        return stationCode == other.stationCode && payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = stationCode.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}

class KitchenDisplaySystemProtocolEngine {

    // ESC/POS Command Constants
    companion object {
        val CMD_INIT = byteArrayOf(0x1B, 0x40) // ESC @
        val CMD_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
        val CMD_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
        val CMD_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
        val CMD_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
        val CMD_DOUBLE_SIZE = byteArrayOf(0x1D, 0x21, 0x11) // GS ! 0x11
        val CMD_NORMAL_SIZE = byteArrayOf(0x1D, 0x21, 0x00)
        val CMD_FEED_CUT = byteArrayOf(0x1D, 0x56, 0x41, 0x03) // GS V A 3 (cut with feed)
    }

    private val activeTickets = mutableMapOf<String, KdsOrderTicket>()

    fun registerOrder(ticket: KdsOrderTicket): Boolean {
        if (activeTickets.containsKey(ticket.ticketId)) return false
        activeTickets[ticket.ticketId] = ticket
        return true
    }

    fun updateStatus(ticketId: String, newStatus: KdsTicketStatus): Boolean {
        val ticket = activeTickets[ticketId] ?: return false
        activeTickets[ticketId] = ticket.copy(status = newStatus)
        return true
    }

    fun getTicket(ticketId: String): KdsOrderTicket? = activeTickets[ticketId]

    fun getActiveTicketCount(): Int = activeTickets.size

    fun calculateElapsedMinutes(ticketId: String, currentEpochMillis: Long): Double {
        val ticket = activeTickets[ticketId] ?: return 0.0
        val elapsedMillis = (currentEpochMillis - ticket.createdAtMillis).coerceAtLeast(0)
        return elapsedMillis / 60000.0
    }

    fun isSlaExceeded(ticketId: String, currentEpochMillis: Long): Boolean {
        val ticket = activeTickets[ticketId] ?: return false
        val elapsed = calculateElapsedMinutes(ticketId, currentEpochMillis)
        return elapsed > ticket.targetSlaMinutes
    }

    fun filterItemsByStation(ticket: KdsOrderTicket, targetStation: KdsStation): List<KdsOrderItem> {
        return ticket.items.filter { it.station == targetStation }
    }

    fun renderEscPosKitchenTicket(ticket: KdsOrderTicket, station: KdsStation): ByteArray {
        val stationItems = filterItemsByStation(ticket, station)
        val stream = ByteArrayOutputStream()

        stream.write(CMD_INIT)
        stream.write(CMD_ALIGN_CENTER)
        stream.write(CMD_DOUBLE_SIZE)
        stream.write(CMD_BOLD_ON)

        // Ticket Header
        val header = "KDS: ${station.stationCode} - ${station.stationArabic}\n".toByteArray(StandardCharsets.UTF_8)
        stream.write(header)

        stream.write(CMD_NORMAL_SIZE)
        val orderRef = "TICKET #${ticket.orderNumber} | REF: ${ticket.tableOrPickupRef}\n".toByteArray(StandardCharsets.UTF_8)
        stream.write(orderRef)

        val prioText = "PRIORITY: [${ticket.priority.tag}] | SLA: ${ticket.targetSlaMinutes}m\n".toByteArray(StandardCharsets.UTF_8)
        stream.write(prioText)
        stream.write("--------------------------------\n".toByteArray(StandardCharsets.UTF_8))

        // Item List
        stream.write(CMD_ALIGN_LEFT)
        for (item in stationItems) {
            val surplusFlag = if (item.isSurplusRescue) "[SURPLUS RESCUE] " else ""
            val line = "${item.quantity}x $surplusFlag${item.nameArabic}\n".toByteArray(StandardCharsets.UTF_8)
            stream.write(line)
            if (item.specialInstructions.isNotBlank()) {
                val instr = "   * ملاحظة: ${item.specialInstructions}\n".toByteArray(StandardCharsets.UTF_8)
                stream.write(instr)
            }
        }

        stream.write("--------------------------------\n".toByteArray(StandardCharsets.UTF_8))
        stream.write(CMD_ALIGN_CENTER)
        val footer = "QOOT KDS SYSTEM | PORT 9100\n".toByteArray(StandardCharsets.UTF_8)
        stream.write(footer)
        stream.write(CMD_BOLD_OFF)
        stream.write(CMD_FEED_CUT)

        return stream.toByteArray()
    }

    fun packageTcpSocketFrame(stationCode: String, payload: ByteArray): KdsTcpSocketFrame {
        var xorSum: Byte = 0
        for (b in payload) {
            xorSum = (xorSum.toInt() xor b.toInt()).toByte()
        }
        return KdsTcpSocketFrame(
            magicHeader = 0x02,
            stationCode = stationCode,
            payloadLength = payload.size.toShort(),
            payload = payload,
            checksumXor = xorSum,
            magicTail = 0x03
        )
    }

    fun verifySocketFrame(frame: KdsTcpSocketFrame): Boolean {
        if (frame.magicHeader != 0x02.toByte() || frame.magicTail != 0x03.toByte()) {
            return false
        }
        if (frame.payload.size != frame.payloadLength.toInt()) {
            return false
        }
        var xorSum: Byte = 0
        for (b in frame.payload) {
            xorSum = (xorSum.toInt() xor b.toInt()).toByte()
        }
        return xorSum == frame.checksumXor
    }
}
