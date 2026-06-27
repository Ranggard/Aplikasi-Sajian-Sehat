package com.example.sajiansehat.api;

import com.example.sajiansehat.models.RekomendasiRequest;
import com.example.sajiansehat.models.RekomendasiResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("api/rekomendasi")
    Call<RekomendasiResponse> getRekomendasi(@Body RekomendasiRequest request);
}
