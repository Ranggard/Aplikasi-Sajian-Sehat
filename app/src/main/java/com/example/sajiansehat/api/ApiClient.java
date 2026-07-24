package com.example.sajiansehat.api;

import com.example.sajiansehat.BuildConfig;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    // API base URL from BuildConfig (configured in build.gradle.kts)
    private static final String BASE_URL = BuildConfig.API_BASE_URL;
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            // Initialize HTTP logging interceptor for debugging
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Configure OkHttp client with API Key interceptor and extended timeout
            OkHttpClient client = new OkHttpClient.Builder()
                    // API Key Interceptor - Add X-API-Key header to every request
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request originalRequest = chain.request();
                            
                            // Add API Key header to every request
                            Request newRequest = originalRequest.newBuilder()
                                    .addHeader("X-API-Key", BuildConfig.API_KEY)
                                    .addHeader("Content-Type", "application/json")
                                    .build();
                            
                            return chain.proceed(newRequest);
                        }
                    })
                    // Logging Interceptor
                    .addInterceptor(loggingInterceptor)
                    // Timeouts (10 minutes max for 1 month recommendation = 90 recipes with AI processing)
                    .connectTimeout(600, TimeUnit.SECONDS)   // 10 minutes
                    .readTimeout(600, TimeUnit.SECONDS)      // 10 minutes
                    .writeTimeout(600, TimeUnit.SECONDS)     // 10 minutes
                    .build();

            // Build Retrofit instance with Gson converter
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }
}
