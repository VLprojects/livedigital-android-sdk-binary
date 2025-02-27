package space.livedigital.example.bson

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

/**
 * The class for generating BSON ObjectId is based on library:
 *
 * [https://github.com/mongodb/kbson/blob/main/src/commonMain/kotlin/org/mongodb/kbson/BsonObjectId.kt]
 *
 * A representation of the BSON ObjectId type
 * A globally unique identifier for objects.
 */
@Suppress("MagicNumber")
internal object BSONObjectIdGenerator {

    private const val OBJECT_ID_LENGTH: Int = 12
    private const val LOW_ORDER_THREE_BYTES = 0x00ffffff
    private const val FIRST_RANDOM_MAX_VALUE = 0x01000000
    private const val SECOND_RANDOM_MAX_VALUE = 0x00008000

    private val random = Random(getCurrentTimeInSeconds())
    private val firstRandomValue = random.nextInt(FIRST_RANDOM_MAX_VALUE)
    private val secondRandomValue = random.nextInt(SECOND_RANDOM_MAX_VALUE).toShort()
    private val counter = AtomicInteger(random.nextInt())

    fun generateBSONObjectId(): String {
        val byteArray = createByteArray()
        return createHexString(byteArray)
    }

    /**
     * The method is based on:
     *
     * [https://github.com/mongodb/kbson/blob/main/src/commonMain/kotlin/org/mongodb/kbson/internal/HexUtils.kt]
     *
     * Converts this instance into a 24-byte hexadecimal string representation.
     * @return a string representation of the ObjectId in hexadecimal format
     */
    private fun createHexString(byteArray: ByteArray): String {
        return byteArray.joinToString("") {
            (0xFF and it.toInt()).toString(radix = 16)
                .padStart(length = 2, padChar = '0')
                .lowercase()
        }
    }

    private fun createByteArray(): ByteArray {
        val timestamp = getCurrentTimeInSeconds().toInt()
        val counter = getCounter()

        val bytes = ByteArray(OBJECT_ID_LENGTH)
        bytes[0] = (timestamp shr 24).toByte()
        bytes[1] = (timestamp shr 16).toByte()
        bytes[2] = (timestamp shr 8).toByte()
        bytes[3] = timestamp.toByte()
        bytes[4] = (firstRandomValue shr 16).toByte()
        bytes[5] = (firstRandomValue shr 8).toByte()
        bytes[6] = firstRandomValue.toByte()
        bytes[7] = (secondRandomValue.toInt() shr 8).toByte()
        bytes[8] = secondRandomValue.toByte()
        bytes[9] = (counter shr 16).toByte()
        bytes[10] = (counter shr 8).toByte()
        bytes[11] = counter.toByte()
        return bytes
    }

    private fun getCurrentTimeInSeconds(): Long {
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
    }

    private fun getCounter(): Int = counter.addAndGet(1) and LOW_ORDER_THREE_BYTES
}