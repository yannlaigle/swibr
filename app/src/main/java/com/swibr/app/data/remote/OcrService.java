package com.swibr.app.data.remote;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.logging.HttpLoggingInterceptor;
import com.swibr.app.util.ProgressRequestBody;

import retrofit.Call;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import retrofit.RxJavaCallAdapterFactory;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;

public interface OcrService {

    // TODO
    // - https://community.idolondemand.com/t5/Wiki/Java-Calling-the-APIs/ta-p/288

    String ENDPOINT = "https://api.idolondemand.com/1/api";

    @Multipart
    @POST("/async/ocrdocument/v1")
    Call<String> upload(
            @Part("file\"; filename=\"image.png\" ") ProgressRequestBody file,
            @Part("mode") String mode,
            @Part("apikey") String apiKey);

    /******** Helper class that sets up a new services *******/
    class Creator {

        public static OcrService newOcrService() {

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);
            OkHttpClient httpClient = new OkHttpClient();
            httpClient.interceptors().add(logging);

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(OcrService.ENDPOINT)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .client(httpClient)
                    .build();

            return retrofit.create(OcrService.class);
        }
    }
}