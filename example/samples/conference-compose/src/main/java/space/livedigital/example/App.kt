package space.livedigital.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
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
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager =
            applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CALL_CHANNEL_ID = "pending_call"
        const val CALL_CHANNEL_NAME = "PendingCall"
    }
}
