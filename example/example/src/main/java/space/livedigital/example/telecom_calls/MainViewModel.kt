package space.livedigital.example.telecom_calls

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.telecom.CallAttributesCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import org.koin.core.component.KoinComponent
import space.livedigital.example.calls.utils.CallRepository
import space.livedigital.example.telecom_calls.utils.Call
import space.livedigital.example.telecom_calls.utils.TelecomCallRepository

class MainViewModel(
    savedStateHandle: SavedStateHandle,
    private val callRepository: CallRepository,
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
        override fun onCallEnded(call: Call) {
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

    init {
        callRepository.addObserver(observer)
        TelecomCallRepository.addObserver(telecomCallObserver)
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

        callRepository.removeObserver(observer)
        TelecomCallRepository.removeObserver(telecomCallObserver)
    }

    sealed interface Event {
        data class OnContactMissing(val caller: String, val number: String): Event
    }
}