package space.livedigital.example.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import space.livedigital.example.logger.ConsoleLogger
import space.livedigital.sdk.engine.LiveDigitalEngine
import space.livedigital.sdk.engine.StockLiveDigitalEngine

internal val liveDigitalEngineModule = module {

    factory<LiveDigitalEngine> {
        StockLiveDigitalEngine(
            context = androidContext(),
            externalLoggers = listOf(ConsoleLogger)
        )
    }
}
