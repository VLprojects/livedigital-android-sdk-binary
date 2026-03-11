package space.livedigital.example

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.messaging.FirebaseMessaging
import com.sequenia.permissionchecker.check
import com.sequenia.permissionchecker.registerPermissionChecker
import space.livedigital.example.ui.screens.MainScreen
import space.livedigital.example.ui.theme.AppTheme

class MainActivity : AppCompatActivity() {

    private val permissionChecker = registerPermissionChecker()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                checkPermissions(
                    listOf(
                        Manifest.permission.POST_NOTIFICATIONS,
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.WRITE_CONTACTS,
                        Manifest.permission.READ_CONTACTS
                    ),
                    {}
                )
            } else {
                checkPermissions(
                    listOf(
                        Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.WRITE_CONTACTS,
                        Manifest.permission.READ_CONTACTS
                    ),
                    {}
                )
            }
        }

        setContent {
            AppTheme {
                MainScreen(
                    onCopyButtonClicked = ::copyPushTokenToClipboards,
                    onOpenCallAccountSettingsButtonClicked = ::openPhoneAccountsSettings
                )
            }
        }
    }

    private fun copyPushTokenToClipboards() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                copyToClipboard(this@MainActivity, token)
            } else {
                Log.d(
                    "FCM",
                    "Fetching FCM registration token failed", task.exception
                )
            }
        }
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("label", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun openPhoneAccountsSettings() {
        startActivity(Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS))
    }

    private fun checkPermissions(
        permissions: List<String>,
        permissionGrantedAction: () -> Unit,
        permissionDeniedAction: (() -> Unit)? = null,
    ) {
        permissionChecker.check(permissions.toTypedArray()) {
            onAllGranted(permissionGrantedAction)

            onAnyDenied { permissionDeniedAction?.invoke() }
        }
    }
}