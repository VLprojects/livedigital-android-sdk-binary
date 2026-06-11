package space.livedigital.example.di

import com.chuckerteam.chucker.api.ChuckerInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val interceptorsModule = module {
    single {
        ChuckerInterceptor.Builder(androidContext())
            .build()
    }
}