package space.livedigital.example

import android.app.Application
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
    }
}
