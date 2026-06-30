package space.livedigital.example

import android.content.Context
import androidx.core.content.edit

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
    }
}
