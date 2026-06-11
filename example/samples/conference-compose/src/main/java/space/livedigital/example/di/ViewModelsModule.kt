package space.livedigital.example.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import space.livedigital.example.MainViewModel

val viewModelsModule = module {
    viewModel {
        MainViewModel()
    }
}
