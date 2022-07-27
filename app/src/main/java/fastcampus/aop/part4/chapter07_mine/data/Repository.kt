package fastcampus.aop.part4.chapter07_mine.data

import fastcampus.aop.part4.chapter07_mine.BuildConfig
import fastcampus.aop.part4.chapter07_mine.data.models.PhotoResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

object Repository {

    private val unsplashApiService: UnsplashApiService by lazy {
        Retrofit.Builder()
            .baseUrl(Url.UNSPLASH_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(buildOkHttpClient())
            .build()
            .create()

    }

    suspend fun getRandomPhotos(query: String?):List<PhotoResponse>? =
        unsplashApiService.getRandomPhotos(query).body()

//    suspend fun getRandomPhotos(query: String?):List<PhotoResponse>? {
//        return unsplashApiService.getRandomPhotos(query).body()
//    }

    private fun buildOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if(BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                }
            )
            .build()

}