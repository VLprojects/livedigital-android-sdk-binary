package space.livedigital.example

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.telecom.CallAttributesCompat
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import org.koin.core.component.KoinComponent
import space.livedigital.example.calls.internal.repository.CallRepository
import space.livedigital.example.calls.telecom.entities.CallFromPush
import space.livedigital.example.calls.telecom.repositories.TelecomCallRepository

class MainViewModel(
    private val callRepository: CallRepository,
    // TODO: Move to repository
    private val contentResolver: ContentResolver
) : ViewModel(), KoinComponent {

    private val eventChannel = Channel<Event>(Channel.UNLIMITED)

    val eventFlow: Flow<Event> = eventChannel.receiveAsFlow()

    private val observer = object : CallRepository.CallObserver {
        override fun onCallEnded(callAttributes: CallAttributesCompat) {
            if(!hasContact(  callAttributes.address.schemeSpecificPart)) {
                eventChannel.trySend(
                    Event.OnContactMissing(
                        callAttributes.displayName.toString(),
                        callAttributes.address.schemeSpecificPart
                    )
                )
            }
        }
    }

    private val telecomCallObserver = object : TelecomCallRepository.CallObserver {
        override fun onCallEnded(call: CallFromPush) {
            if(!hasContact(call.callerNumber)) {
                eventChannel.trySend(
                    Event.OnContactMissing(
                        call.caller,
                        call.callerNumber
                    )
                )
            }
        }
    }


    private fun hasContact(
        number: String
    ): Boolean {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            number
        )

        val projection = arrayOf(
            ContactsContract.PhoneLookup._ID
        )

        // TODO: Move to separate thread.
        contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            return cursor.moveToFirst()
        }
        return false
    }

    override fun onCleared() {
        super.onCleared()
    }

    sealed interface Event {
        data class OnContactMissing(val caller: String, val number: String): Event
    }
}