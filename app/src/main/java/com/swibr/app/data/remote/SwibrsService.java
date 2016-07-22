package com.swibr.app.data.remote;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit.Call;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import retrofit.RxJavaCallAdapterFactory;
import retrofit.http.GET;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.http.Query;
import rx.Observable;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.ResponseBody;
import com.squareup.okhttp.logging.HttpLoggingInterceptor;
import com.swibr.app.data.model.Article;
import com.swibr.app.data.model.Swibr;
import com.swibr.app.util.ProgressRequestBody;

public interface SwibrsService {

    String ENDPOINT = "http://10.0.2.2:3000/api/";
    int TIMEOUT = 60;

    // TODO
    // - http://open.blogs.nytimes.com/2014/08/18/getting-groovy-with-reactive-android/
    // - https://www.youtube.com/watch?v=k3D0cWyNno4
    // - https://github.com/square/sqlbrite/blob/master/sqlbrite-sample

    @GET("swibrs")
    Observable<List<Article>> getSwibrs();

    @POST("store")
    Call<ResponseBody> storeSwibe(
            @Query("data") String data);

    /********
     * Helper class that sets up a new services
     *******/
    class Creator {

        public static SwibrsService newSwibrsService() {
            Gson gson = new GsonBuilder()
//                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                    .create();

            HttpLoggingInterceptor httpLogging = new HttpLoggingInterceptor();
            httpLogging.setLevel(HttpLoggingInterceptor.Level.HEADERS);

            OkHttpClient httpClient = new OkHttpClient();
            httpClient.setConnectTimeout(TIMEOUT, TimeUnit.SECONDS); // connect timeout
            httpClient.setReadTimeout(TIMEOUT, TimeUnit.SECONDS);    // socket timeout
            httpClient.interceptors().add(httpLogging);

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(SwibrsService.ENDPOINT)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .client(httpClient)
                    .build();

            return retrofit.create(SwibrsService.class);
        }
    }
}
