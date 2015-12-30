package com.swibr.app.data.remote;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import retrofit.RxJavaCallAdapterFactory;
import retrofit.http.GET;
import rx.Observable;
import com.swibr.app.data.model.Swibr;

public interface SwibrsService {

    String ENDPOINT = "http://melchizetech.com/swibr/api/";


    // TODO
    // - http://open.blogs.nytimes.com/2014/08/18/getting-groovy-with-reactive-android/
    // - https://www.youtube.com/watch?v=k3D0cWyNno4
    // - https://github.com/square/sqlbrite/blob/master/sqlbrite-sample


    @GET("swibrs")
    Observable<List<Swibr>> getSwibrs();

    /******** Helper class that sets up a new services *******/
    class Creator {

        public static SwibrsService newSwibrsService() {
            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    .create();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(SwibrsService.ENDPOINT)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .build();

            return retrofit.create(SwibrsService.class);
        }
    }
}
