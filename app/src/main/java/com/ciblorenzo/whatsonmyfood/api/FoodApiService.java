package com.ciblorenzo.whatsonmyfood.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface FoodApiService {
    @POST("v2/{endpointId}/runsync")
    Call<RunPodResponse> explainFood(
        @Path("endpointId") String endpointId,
        @Header("Authorization") String authHeader,
        @Body RunPodRequest request
    );
}
