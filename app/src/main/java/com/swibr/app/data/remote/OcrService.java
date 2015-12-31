package com.swibr.app.data.remote;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.ResponseBody;
import com.squareup.okhttp.logging.HttpLoggingInterceptor;
import com.swibr.app.util.ProgressRequestBody;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import retrofit.Call;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import retrofit.RxJavaCallAdapterFactory;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.http.Query;

public interface OcrService {

    // TODO
    // - https://community.idolondemand.com/t5/Wiki/Java-Calling-the-APIs/ta-p/288
    // - https://futurestud.io/blog/retrofit-2-how-to-upload-files-to-server
    // - https://futurestud.io/blog/retrofit-2-upgrade-guide-from-1-9

    String ENDPOINT = "https://api.idolondemand.com/";

    @Multipart
    @POST("1/api/sync/ocrdocument/v1")
    Call<ResponseBody> upload(
            @Part("file\"; filename=\"image.png\" ") ProgressRequestBody file,
            @Query("mode") String mode,
            @Query("apikey") String apikey);

    /******** Helper class that sets up a new services *******/
    class Creator {

        public static OcrService newOcrService() {

            HttpLoggingInterceptor httpLogging = new HttpLoggingInterceptor();
            httpLogging.setLevel(HttpLoggingInterceptor.Level.HEADERS);

            LoggingInterceptor logging = new LoggingInterceptor();

            OkHttpClient httpClient = new OkHttpClient();
            httpClient.setConnectTimeout(60, TimeUnit.SECONDS); // connect timeout
            httpClient.setReadTimeout(60, TimeUnit.SECONDS);    // socket timeout
            httpClient.interceptors().add(httpLogging);
            httpClient.interceptors().add(logging);

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(OcrService.ENDPOINT)
                    //.addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .client(httpClient)
                    .build();

            return retrofit.create(OcrService.class);
        }
    }
}