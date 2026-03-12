package space.livedigital.example

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import space.livedigital.example.ui.screens.MainScreen
import space.livedigital.example.ui.theme.AppTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent()
    }

    private fun setContent() {
        setContent {
            AppTheme {
                PermissionChecker()
                MainScreen(
                    onCopyButtonClicked = ::copyPushTokenToClipboards,
                    onOpenCallAccountSettingsButtonClicked = ::openPhoneAccountsSettings
                )
            }
        }
    }

    @Composable
    fun PermissionChecker() {
        val context = LocalContext.current

        val permissions = remember {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.WRITE_CONTACTS,
                    Manifest.permission.READ_CONTACTS
                )
            } else {
                arrayOf(
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.WRITE_CONTACTS,
                    Manifest.permission.READ_CONTACTS
                )
            }
        }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { _ ->
            // Ignore result
        }

        LaunchedEffect(Unit) {
            val needsRequest = permissions.any {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }

            if (needsRequest) {
                launcher.launch(permissions)
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
}