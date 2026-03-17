package space.livedigital.example

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.util.Log
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.messaging.FirebaseMessaging
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import space.livedigital.example.calls.telecom.services.CallConnectionService
import space.livedigital.example.ui.screens.MainScreen
import space.livedigital.example.ui.theme.AppTheme

class MainActivity : AppCompatActivity() {

    private val permissionsViewModel by viewModel<PermissionsViewModel> {
        parametersOf(buildPermissions(), isPhoneAccountEnabled())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(navigationBarStyle = createNavigationBarStyle())
        setContent()
    }

    override fun onResume() {
        super.onResume()
        val permissions = buildPermissions()
        val isPhoneAccountRegistered = isPhoneAccountEnabled()
        permissions.forEach {
            permissionsViewModel.onPermissionStateUpdated(it.name, it.isGranted)
        }
        permissionsViewModel.onPhoneAccountEnabledStateUpdated(isPhoneAccountRegistered)
    }

    private fun createNavigationBarStyle(): SystemBarStyle {
        return SystemBarStyle.light(
            scrim = Color.TRANSPARENT,
            darkScrim = Color.TRANSPARENT
        )
    }

    private fun setContent() {
        setContent {
            AppTheme {
                val permissionsState =
                    permissionsViewModel.permissionsState.collectAsStateWithLifecycle()

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { _ ->
                    val permissions = buildPermissions()
                    val isPhoneAccountRegistered = isPhoneAccountEnabled()
                    permissions.forEach {
                        permissionsViewModel.onPermissionStateUpdated(it.name, it.isGranted)
                    }
                    permissionsViewModel.onPhoneAccountEnabledStateUpdated(isPhoneAccountRegistered)
                }

                MainScreen(
                    permissionsState = permissionsState,
                    onCopyButtonClicked = ::copyPushTokenToClipboards,
                    onPermissionSwitchClicked = { permission ->
                        if (permission.isGranted) return@MainScreen
                        permissionLauncher.launch(permission.name)
                    },
                    onCallAccountSwitchClicked = ::openPhoneAccountsSettings
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

    private fun buildPermissions(): List<Permission> {
        return buildList {
            permissionImportanceByNames.forEach { (permissionName, importance) ->
                add(buildPermission(permissionName, importance))
            }
        }
    }

    private fun buildPermission(permissionName: String, importance: Importance): Permission {
        val isGranted =
            this.checkSelfPermission(permissionName) == PackageManager.PERMISSION_GRANTED
        return Permission(name = permissionName, importance = importance, isGranted = isGranted)
    }

    // Phone account is not really permission so we need to check it in a different way
    private fun isPhoneAccountEnabled(): Boolean {
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
        return telecomManager.callCapablePhoneAccounts.any {
            it.componentName == ComponentName(this, CallConnectionService::class.java)
        }
    }

    private fun openPhoneAccountsSettings() {
        startActivity(Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS))
    }

    companion object {
        private val permissionImportanceByNames =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                mapOf(
                    Manifest.permission.POST_NOTIFICATIONS to Importance.IMPORTANT_FOR_ALL_CALLS,
                    Manifest.permission.READ_PHONE_STATE to Importance.IMPORTANT_FOR_SYSTEM_CALLS,
                    Manifest.permission.WRITE_CONTACTS to Importance.DEFAULT,
                    Manifest.permission.CALL_PHONE to Importance.IMPORTANT_FOR_SYSTEM_CALLS,
                    Manifest.permission.READ_CONTACTS to Importance.DEFAULT,
                    Manifest.permission.RECORD_AUDIO to Importance.DEFAULT,
                    Manifest.permission.BLUETOOTH_CONNECT to Importance.DEFAULT
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mapOf(
                    Manifest.permission.READ_PHONE_STATE to Importance.IMPORTANT_FOR_SYSTEM_CALLS,
                    Manifest.permission.WRITE_CONTACTS to Importance.DEFAULT,
                    Manifest.permission.CALL_PHONE to Importance.IMPORTANT_FOR_SYSTEM_CALLS,
                    Manifest.permission.READ_CONTACTS to Importance.DEFAULT,
                    Manifest.permission.RECORD_AUDIO to Importance.DEFAULT,
                    Manifest.permission.BLUETOOTH_CONNECT to Importance.DEFAULT
                )
            } else {
                mapOf(
                    Manifest.permission.READ_PHONE_STATE to Importance.IMPORTANT_FOR_SYSTEM_CALLS,
                    Manifest.permission.WRITE_CONTACTS to Importance.DEFAULT,
                    Manifest.permission.CALL_PHONE to Importance.IMPORTANT_FOR_SYSTEM_CALLS,
                    Manifest.permission.READ_CONTACTS to Importance.DEFAULT,
                    Manifest.permission.RECORD_AUDIO to Importance.DEFAULT,
                )
            }
    }
}