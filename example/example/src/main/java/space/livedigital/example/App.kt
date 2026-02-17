package space.livedigital.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import space.livedigital.example.calls.telecom.services.CallConnectionService
import space.livedigital.example.di.liveDigitalEngineModule
import space.livedigital.example.di.viewModelsModule

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@App)
            val moduleList = listOf(viewModelsModule, liveDigitalEngineModule)
            this.modules(moduleList)
        }

        val channel = NotificationChannel(
            CALL_CHANNEL_ID,
            CALL_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager =
            applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val telecomManager = applicationContext.getSystemService(TELECOM_SERVICE) as TelecomManager
        val componentName = ComponentName(applicationContext, CallConnectionService::class.java)
        val phoneAccountHandle = PhoneAccountHandle(componentName, "LD SDK example")
        val phoneAccount = PhoneAccount.builder(phoneAccountHandle, "LD SDK example")
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER).build()
        telecomManager.registerPhoneAccount(phoneAccount)
    }

    companion object {
        const val CALL_CHANNEL_ID = "pending_call"
        const val CALL_CHANNEL_NAME = "PendingCall"
    }
}
