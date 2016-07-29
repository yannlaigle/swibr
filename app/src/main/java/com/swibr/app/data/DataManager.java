package com.swibr.app.data;

import com.swibr.app.data.local.DatabaseHelper;
import com.swibr.app.data.local.PreferencesHelper;
import com.swibr.app.data.model.Article.Article;
import com.swibr.app.data.remote.HavenOCRService.HavenOcr;
import com.swibr.app.data.remote.SwibrsService;
import com.swibr.app.util.EventPosterHelper;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Func1;

@Singleton
public class DataManager {

    private final SwibrsService mSwibrsService;
    private final HavenOcr mHavenOcrService;
    private final DatabaseHelper mDatabaseHelper;
    private final PreferencesHelper mPreferencesHelper;
    private final EventPosterHelper mEventPoster;

    @Inject
    public DataManager(SwibrsService swibrsService, HavenOcr havenOcrService, PreferencesHelper preferencesHelper,
                       DatabaseHelper databaseHelper, EventPosterHelper eventPosterHelper) {
        mSwibrsService = swibrsService;
        mHavenOcrService = havenOcrService;
        mPreferencesHelper = preferencesHelper;
        mDatabaseHelper = databaseHelper;
        mEventPoster = eventPosterHelper;
    }

    public PreferencesHelper getPreferencesHelper() {
        return mPreferencesHelper;
    }

    public Observable<Article> syncSwibrs() {

        // TODO push new items

        return mSwibrsService.getSwibrs()
                .concatMap(new Func1<List<Article>, Observable<Article>>() {
                    @Override
                    public Observable<Article> call(List<Article> articles) {
                    return mDatabaseHelper.setSwibrs(articles);
                    }
                });
    }

    public Observable<List<Article>> getSwibrs() {
        return mDatabaseHelper.getSwibrs().distinct();
    }

    public Observable<Article> addSwibr(Article article) {
        return mDatabaseHelper.saveSwibr(article);
    }

    /// Helper method to post events from doOnCompleted.
    private Action0 postEventAction(final Object event) {
        return new Action0() {
            @Override
            public void call() {
                mEventPoster.postEventSafely(event);
            }
        };
    }

}
