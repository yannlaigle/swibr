package com.swibr.app.data;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.functions.Func2;

import com.swibr.app.data.local.DatabaseHelper;
import com.swibr.app.data.local.PreferencesHelper;
import com.swibr.app.data.model.Swibr;
import com.swibr.app.data.remote.SwibrsService;
import com.swibr.app.util.EventPosterHelper;

@Singleton
public class DataManager {

    private final SwibrsService mSwibrsService;
    private final DatabaseHelper mDatabaseHelper;
    private final PreferencesHelper mPreferencesHelper;
    private final EventPosterHelper mEventPoster;

    @Inject
    public DataManager(SwibrsService swibrsService, PreferencesHelper preferencesHelper,
                       DatabaseHelper databaseHelper, EventPosterHelper eventPosterHelper) {
        mSwibrsService = swibrsService;
        mPreferencesHelper = preferencesHelper;
        mDatabaseHelper = databaseHelper;
        mEventPoster = eventPosterHelper;
    }

    public PreferencesHelper getPreferencesHelper() {
        return mPreferencesHelper;
    }

    public Observable<Swibr> syncSwibrs() {

        // TODO push new items

        return mSwibrsService.getSwibrs()
                .concatMap(new Func1<List<Swibr>, Observable<Swibr>>() {
                    @Override
                    public Observable<Swibr> call(List<Swibr> swibrs) {
                    return mDatabaseHelper.setSwibrs(swibrs);
                    }
                });
    }

    public Observable<List<Swibr>> getSwibrs() {
        return mDatabaseHelper.getSwibrs().distinct();
    }

    public Observable<Swibr> addSwibr(Swibr newSwibr) {
        return mDatabaseHelper.saveSwibr(newSwibr);
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
