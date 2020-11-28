package ru.ovm.genericcarsharing

import android.app.Application
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.ovm.genericcarsharing.net.Api

@Suppress("unused")
class App : Application() {

    private val appModule = module {

        single {
            OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .build()
        }

        single<Gson> {
            GsonBuilder().create()
        }

        single<Api> {
            Retrofit.Builder()
                .baseUrl(Api.URL)
                .addConverterFactory(GsonConverterFactory.create(get()))
                .client(get())
                .build().create(Api::class.java)
        }

        viewModel { MainViewModel(get()) }
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@App)
            modules(appModule)
        }
    }

    companion object {
        // Если включить, то сильно лагает и приложение начинает есть до 0.7гб ram, но красиво смотрится
        const val ROTATE_CARS = false
    }
}