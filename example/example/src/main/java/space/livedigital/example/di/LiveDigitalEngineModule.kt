package space.livedigital.example.di

import android.content.ContentResolver
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import space.livedigital.example.calls.utils.CallRepository
import space.livedigital.example.logger.ConsoleLogger
import space.livedigital.sdk.engine.LiveDigitalEngine
import space.livedigital.sdk.engine.StockLiveDigitalEngine

val liveDigitalEngineModule = module {

    factory<LiveDigitalEngine> {
        StockLiveDigitalEngine(
            context = androidContext(),
            externalLoggers = listOf(ConsoleLogger)
        )
    }

    single<CallRepository> {
        CallRepository.create(androidContext())
    }

    single<ContentResolver> {
        androidContext().contentResolver
    }
}
