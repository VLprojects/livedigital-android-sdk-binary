package space.livedigital.example

import android.content.Context
import androidx.core.content.edit
import java.util.UUID

internal class AuthStorage(context: Context) {

    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var phoneNumber: String
        get() = preferences.getString(KEY_PHONE_NUMBER, null).orEmpty()
        set(value) {
            preferences.edit {
                putString(KEY_PHONE_NUMBER, value)
            }
        }

    val deviceId: String
        get() = preferences.getString(KEY_DEVICE_ID, null)
            ?: UUID.randomUUID().toString().also { generated ->
                preferences.edit { putString(KEY_DEVICE_ID, generated) }
            }

    companion object {
        var instance: AuthStorage? = null
            private set

        /**
         * This does not illustrate best practices for instantiating classes in Android but for
         * simplicity we use this create method to create a singleton.
         */
        fun create(context: Context): AuthStorage {
            check(instance == null) {
                "AuthStorage instance already created"
            }

            return AuthStorage(context).also {
                instance = it
            }
        }

        private const val PREFERENCES_NAME = "auth_preferences"
        private const val KEY_PHONE_NUMBER = "phone_number"
        private const val KEY_DEVICE_ID = "device_id"
    }
}
