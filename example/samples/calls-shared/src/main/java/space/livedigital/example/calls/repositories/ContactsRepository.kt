package space.livedigital.example.calls.repositories

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ContactsRepository {

    suspend fun hasContact(number: String): HasContactResult
}

class AndroidContactsRepository(
    private val contentResolver: ContentResolver
) : ContactsRepository {

    override suspend fun hasContact(number: String): HasContactResult {
        return withContext(Dispatchers.IO) {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                number
            )

            val projection = arrayOf(
                ContactsContract.PhoneLookup._ID
            )

            try {
                return@withContext contentResolver.query(uri, projection, null, null, null)
                    ?.use { cursor ->
                        val isFound = cursor.moveToFirst()
                        if (isFound) HasContactResult.EXISTS else HasContactResult.MISSED
                    } ?: HasContactResult.MISSED
            } catch (_: SecurityException) {
                return@withContext HasContactResult.RESTRICTED
            } catch (_: Exception) {
                return@withContext HasContactResult.MISSED
            }
        }
    }
}

enum class HasContactResult {
    MISSED,
    EXISTS,
    RESTRICTED
}