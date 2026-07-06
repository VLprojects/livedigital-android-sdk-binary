package space.livedigital.example.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import space.livedigital.example.logger.ConsoleLogger
import space.livedigital.sdk.engine.LiveDigitalEngine
import space.livedigital.sdk.engine.LiveDigitalSDKEnvironment
import space.livedigital.sdk.engine.StockLiveDigitalEngine

val liveDigitalEngineModule = module {

    factory<LiveDigitalEngine> { (loadBalancerUrl: String?, signalingApiUrl: String?) ->
        val environment = when {
            loadBalancerUrl != null && signalingApiUrl != null -> {
                LiveDigitalSDKEnvironment(
                    loadBalancerUrl = loadBalancerUrl,
                    signalingApiUrl = signalingApiUrl
                )
            }

            loadBalancerUrl != null -> {
                LiveDigitalSDKEnvironment(loadBalancerUrl = loadBalancerUrl)
            }

            signalingApiUrl != null -> {
                LiveDigitalSDKEnvironment(signalingApiUrl = signalingApiUrl)
            }

            else -> {
                LiveDigitalSDKEnvironment()
            }
        }
        StockLiveDigitalEngine(
            context = androidContext(),
            environment = environment,
            userAgent = "",
            externalLoggers = listOf(ConsoleLogger)
        )
    }
}
