package com.lakony.commandcenter.absa

import android.nfc.Tag
import android.nfc.tech.IsoDep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

sealed interface NfcCardState {
    data object Idle : NfcCardState
    data object Reading : NfcCardState
    data class Success(val card: NfcCardInfo) : NfcCardState
    data class Error(val message: String) : NfcCardState
}

data class NfcCardInfo(
    val paymentNetwork: String,
    val applicationLabel: String,
    val aid: String,
    val maskedPan: String?,
    val expiry: String?,
    val scannedAt: String,
)

object AbsaNfcCardReader {
    private val _state = MutableStateFlow<NfcCardState>(NfcCardState.Idle)
    val state: StateFlow<NfcCardState> = _state.asStateFlow()

    fun reset() {
        _state.value = NfcCardState.Idle
    }

    fun read(tag: Tag) {
        val isoDep = IsoDep.get(tag)
        if (isoDep == null) {
            _state.value = NfcCardState.Error("This NFC tag is not an ISO-DEP payment card.")
            return
        }

        _state.value = NfcCardState.Reading
        try {
            isoDep.connect()
            isoDep.timeout = 5000
            val card = readEmvCard(isoDep)
            _state.value = NfcCardState.Success(card)
        } catch (_: SecurityException) {
            _state.value = NfcCardState.Error("Android blocked access to this NFC card.")
        } catch (_: IOException) {
            _state.value = NfcCardState.Error("Card communication failed. Hold the card still against the NFC area and try again.")
        } catch (e: Exception) {
            _state.value = NfcCardState.Error(e.message ?: "Unable to read this card.")
        } finally {
            try { isoDep.close() } catch (_: Exception) { }
        }
    }

    private fun readEmvCard(isoDep: IsoDep): NfcCardInfo {
        val ppse = selectByName(isoDep, "2PAY.SYS.DDF01".toByteArray(Charsets.US_ASCII))
        requireSuccess(ppse, "No contactless EMV payment application was found.")

        val aids = findAll(ppse.data, 0x4F).distinctBy { it.toHex() }
        if (aids.isEmpty()) throw IllegalStateException("No payment application was exposed by this card.")

        var selectedAid = aids.first()
        var appResponse: ByteArray? = null
        var appLabel = "Payment card"

        for (aid in aids) {
            val response = selectByName(isoDep, aid)
            if (response.isSuccess) {
                selectedAid = aid
                appResponse = response.data
                appLabel = findFirst(response.data, 0x50)?.decodeText()
                    ?: findFirst(response.data, 0x9F12)?.decodeText()
                    ?: appLabel
                break
            }
        }

        val selectData = appResponse ?: throw IllegalStateException("The card did not allow a payment application to be selected.")
        val recordData = mutableListOf<ByteArray>()

        try {
            val pdol = findFirst(selectData, 0x9F38)
            val gpoData = buildGpoData(pdol)
            val gpo = transceive(isoDep, byteArrayOf(0x80.toByte(), 0xA8.toByte(), 0x00, 0x00, gpoData.size.toByte()) + gpoData + byteArrayOf(0x00))
            if (gpo.isSuccess) {
                val afl = extractAfl(gpo.data)
                if (afl != null) {
                    var i = 0
                    while (i + 3 < afl.size) {
                        val sfi = (afl[i].toInt() and 0xF8) shr 3
                        val firstRecord = afl[i + 1].toInt() and 0xFF
                        val lastRecord = afl[i + 2].toInt() and 0xFF
                        for (record in firstRecord..lastRecord) {
                            val p2 = ((sfi shl 3) or 4).toByte()
                            val rr = transceive(isoDep, byteArrayOf(0x00, 0xB2.toByte(), record.toByte(), p2, 0x00))
                            if (rr.isSuccess) recordData += rr.data
                        }
                        i += 4
                    }
                }
            }
        } catch (_: Exception) {
            // Some issuers reject a zero-filled PDOL. Basic card identification still works.
        }

        val sources = listOf(selectData) + recordData
        val pan = sources.firstNotNullOfOrNull { findFirst(it, 0x5A)?.toBcdDigits()?.trimEnd('F') }
            ?: sources.firstNotNullOfOrNull { source ->
                findFirst(source, 0x57)?.toBcdDigits()?.substringBefore('D')?.trimEnd('F')
            }
        val expiryRaw = sources.firstNotNullOfOrNull { findFirst(it, 0x5F24)?.toBcdDigits() }
            ?: sources.firstNotNullOfOrNull { source ->
                val track2 = findFirst(source, 0x57)?.toBcdDigits() ?: return@firstNotNullOfOrNull null
                val separator = track2.indexOf('D')
                if (separator >= 0 && track2.length >= separator + 5) track2.substring(separator + 1, separator + 5) else null
            }

        val aidHex = selectedAid.toHex()
        return NfcCardInfo(
            paymentNetwork = networkForAid(aidHex),
            applicationLabel = appLabel.ifBlank { "Payment card" },
            aid = aidHex,
            maskedPan = pan?.let(::maskPan),
            expiry = expiryRaw?.let(::formatExpiry),
            scannedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")),
        )
    }

    private fun selectByName(isoDep: IsoDep, name: ByteArray): ApduResponse {
        val command = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, name.size.toByte()) + name + byteArrayOf(0x00)
        return transceive(isoDep, command)
    }

    private fun transceive(isoDep: IsoDep, command: ByteArray): ApduResponse {
        val raw = isoDep.transceive(command)
        if (raw.size < 2) return ApduResponse(raw, 0)
        val sw = ((raw[raw.size - 2].toInt() and 0xFF) shl 8) or (raw.last().toInt() and 0xFF)
        return ApduResponse(raw.copyOf(raw.size - 2), sw)
    }

    private fun requireSuccess(response: ApduResponse, message: String) {
        if (!response.isSuccess) throw IllegalStateException(message)
    }

    private data class ApduResponse(val data: ByteArray, val statusWord: Int) {
        val isSuccess: Boolean get() = statusWord == 0x9000
    }

    private fun buildGpoData(pdol: ByteArray?): ByteArray {
        if (pdol == null || pdol.isEmpty()) return byteArrayOf(0x83.toByte(), 0x00)
        var offset = 0
        var totalLength = 0
        while (offset < pdol.size) {
            val first = pdol[offset].toInt() and 0xFF
            offset++
            if (first and 0x1F == 0x1F) {
                while (offset < pdol.size) {
                    val b = pdol[offset].toInt() and 0xFF
                    offset++
                    if (b and 0x80 == 0) break
                }
            }
            if (offset >= pdol.size) break
            totalLength += pdol[offset].toInt() and 0xFF
            offset++
        }
        require(totalLength <= 255) { "Card PDOL is too large." }
        return byteArrayOf(0x83.toByte(), totalLength.toByte()) + ByteArray(totalLength)
    }

    private fun extractAfl(gpo: ByteArray): ByteArray? {
        findFirst(gpo, 0x94)?.let { return it }
        if (gpo.isNotEmpty() && (gpo[0].toInt() and 0xFF) == 0x80) {
            val node = parseOne(gpo, 0) ?: return null
            return if (node.value.size > 2) node.value.copyOfRange(2, node.value.size) else null
        }
        return null
    }

    private data class Tlv(val tag: Int, val value: ByteArray, val constructed: Boolean)

    private fun parseOne(data: ByteArray, start: Int): Tlv? {
        if (start >= data.size) return null
        var offset = start
        val first = data[offset].toInt() and 0xFF
        val constructed = first and 0x20 != 0
        var tag = first
        offset++
        if (first and 0x1F == 0x1F) {
            tag = 0
            var count = 0
            do {
                if (offset >= data.size || count++ > 3) return null
                val b = data[offset].toInt() and 0xFF
                tag = (tag shl 8) or b
                offset++
            } while (b and 0x80 != 0)
            tag = (first shl (8 * count)) or tag
        }
        if (offset >= data.size) return null
        var length = data[offset].toInt() and 0xFF
        offset++
        if (length and 0x80 != 0) {
            val bytes = length and 0x7F
            if (bytes == 0 || bytes > 3 || offset + bytes > data.size) return null
            length = 0
            repeat(bytes) {
                length = (length shl 8) or (data[offset].toInt() and 0xFF)
                offset++
            }
        }
        if (length < 0 || offset + length > data.size) return null
        return Tlv(tag, data.copyOfRange(offset, offset + length), constructed)
    }

    private fun findAll(data: ByteArray, targetTag: Int): List<ByteArray> {
        val found = mutableListOf<ByteArray>()
        fun scan(bytes: ByteArray) {
            var offset = 0
            while (offset < bytes.size) {
                val parsed = parseWithConsumed(bytes, offset) ?: break
                if (parsed.first.tag == targetTag) found += parsed.first.value
                if (parsed.first.constructed) scan(parsed.first.value)
                offset += parsed.second
            }
        }
        scan(data)
        return found
    }

    private fun findFirst(data: ByteArray, targetTag: Int): ByteArray? = findAll(data, targetTag).firstOrNull()

    private fun parseWithConsumed(data: ByteArray, start: Int): Pair<Tlv, Int>? {
        if (start >= data.size) return null
        var offset = start
        val first = data[offset].toInt() and 0xFF
        val constructed = first and 0x20 != 0
        var tag = first
        offset++
        if (first and 0x1F == 0x1F) {
            tag = 0
            var count = 0
            do {
                if (offset >= data.size || count++ > 3) return null
                val b = data[offset].toInt() and 0xFF
                tag = (tag shl 8) or b
                offset++
            } while (b and 0x80 != 0)
            tag = (first shl (8 * count)) or tag
        }
        if (offset >= data.size) return null
        var length = data[offset].toInt() and 0xFF
        offset++
        if (length and 0x80 != 0) {
            val bytes = length and 0x7F
            if (bytes == 0 || bytes > 3 || offset + bytes > data.size) return null
            length = 0
            repeat(bytes) {
                length = (length shl 8) or (data[offset].toInt() and 0xFF)
                offset++
            }
        }
        if (offset + length > data.size) return null
        val tlv = Tlv(tag, data.copyOfRange(offset, offset + length), constructed)
        return tlv to ((offset + length) - start)
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it.toInt() and 0xFF) }
    private fun ByteArray.toBcdDigits(): String = joinToString("") { "%02X".format(it.toInt() and 0xFF) }
    private fun ByteArray.decodeText(): String = toString(Charsets.ISO_8859_1).trim().filter { it.isLetterOrDigit() || it == ' ' || it == '-' }

    private fun maskPan(pan: String): String {
        if (pan.length < 8) return "•••• ${pan.takeLast(4)}"
        return "${pan.take(4)} •••• •••• ${pan.takeLast(4)}"
    }

    private fun formatExpiry(raw: String): String? {
        if (raw.length < 4) return null
        val yy = raw.substring(0, 2)
        val mm = raw.substring(2, 4)
        val month = mm.toIntOrNull() ?: return null
        if (month !in 1..12) return null
        return "$mm/$yy"
    }

    private fun networkForAid(aid: String): String = when {
        aid.startsWith("A000000003") -> "Visa"
        aid.startsWith("A000000004") -> "Mastercard"
        aid.startsWith("A000000025") -> "American Express"
        aid.startsWith("A000000065") -> "JCB"
        aid.startsWith("A000000152") -> "Discover"
        aid.startsWith("A000000333") -> "UnionPay"
        else -> "EMV contactless"
    }
}
