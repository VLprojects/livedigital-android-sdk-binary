package space.livedigital.example

import android.app.Application
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

        val telecomManager = applicationContext.getSystemService(TELECOM_SERVICE) as TelecomManager
        val componentName = ComponentName(applicationContext, CallConnectionService::class.java)
        val phoneAccountHandle = PhoneAccountHandle(componentName, "LD SDK example")
        val phoneAccount = PhoneAccount.builder(phoneAccountHandle, "LD SDK example")
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER).build()
        telecomManager.registerPhoneAccount(phoneAccount)
    }
}
