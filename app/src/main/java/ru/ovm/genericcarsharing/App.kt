package ru.ovm.genericcarsharing

import android.app.Application
import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.ovm.genericcarsharing.net.cars.ApiCars
import ru.ovm.genericcarsharing.net.directions.ApiDirections
import ru.ovm.genericcarsharing.ui.map.MapViewModel
import ru.ovm.genericcarsharing.utils.Utils
import java.io.File


@Suppress("unused")
class App : Application() {

    private val appModule = module {

        single {
            val context: Context = get()
            val httpCacheDirectory = File(context.cacheDir, "responses")
            val cacheSize: Long = 10 * 1024 * 1024 // 10 MiB

            Cache(httpCacheDirectory, cacheSize)
        }

        single {
            val httpClient = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .addInterceptor {
                    val originalResponse = it.proceed(it.request())
                    if (Utils.isNetworkConnected(get())) {
                        val maxAge = 60 * 10 // 10 минут
                        originalResponse.newBuilder()
                            .header("Cache-Control", "public, max-age=$maxAge")
                            .build()
                    } else {
                        val maxStale = 60 * 60 * 24 * 28 // 4 недели
                        originalResponse.newBuilder()
                            .header("Cache-Control", "public, only-if-cached, max-stale=$maxStale")
                            .build()
                    }
                }
                .cache(get())
                .build()

            httpClient
        }

        single<Gson> {
            GsonBuilder().create()
        }

        single<ApiCars>(named(Api.CARS)) {
            Retrofit.Builder()
                .baseUrl(ApiCars.ENDPOINT)
                .addConverterFactory(GsonConverterFactory.create(get()))
                .client(get())
                .build().create(ApiCars::class.java)
        }

        single<ApiDirections>(named(Api.DIRECTIONS)) {
            Retrofit.Builder()
                .baseUrl(ApiDirections.ENDPOINT)
                .addConverterFactory(GsonConverterFactory.create(get()))
                .client(get())
                .build().create(ApiDirections::class.java)
        }

        viewModel {
            MapViewModel(
                get(named(Api.CARS)),
                get(named(Api.DIRECTIONS)),
                androidContext()
            )
        }
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@App)
            modules(appModule)
        }
    }

    enum class Api {
        CARS, DIRECTIONS
    }
}