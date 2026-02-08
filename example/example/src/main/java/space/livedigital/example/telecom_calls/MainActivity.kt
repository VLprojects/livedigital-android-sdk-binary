package space.livedigital.example.telecom_calls

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.messaging.FirebaseMessaging
import com.sequenia.permissionchecker.check
import com.sequenia.permissionchecker.registerPermissionChecker
import space.livedigital.example.databinding.MainActivityBinding

class MainActivity : AppCompatActivity() {

    private val permissionChecker = registerPermissionChecker()
    private var binding: MainActivityBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                checkPermissions(
                    listOf(
                        Manifest.permission.POST_NOTIFICATIONS,
                        Manifest.permission.READ_PHONE_STATE
                    ),
                    {}
                )
            } else {
                checkPermissions(
                    listOf(Manifest.permission.READ_PHONE_STATE),
                    {}
                )
            }
        }

        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        binding?.copyButton?.setOnClickListener {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    copyToClipboard(this, token)
                } else {
                    Log.d("FCM", "Fetching FCM registration token failed", task.exception)
                }
            }
        }

        binding?.addPhoneAccountButton?.setOnClickListener {
            startActivity(Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS))
        }
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("label", text)
        clipboard.setPrimaryClip(clip)
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

    class CenterButtonLayout(context: Context) : FrameLayout(context) {

        private val copyButton: Button = Button(context).apply {
            text = "Copy token"
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        }

        init {
            addView(copyButton)
        }

        fun setButtonClickListener(listener: OnClickListener) {
            copyButton.setOnClickListener(listener)
        }
    }
}