package com.swibr.app.injection.module;

import android.app.Application;
import android.content.Context;

import com.squareup.otto.Bus;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import com.swibr.app.data.remote.OcrService;
import com.swibr.app.data.remote.SwibrsService;
import com.swibr.app.injection.ApplicationContext;

/**
 * Provide application-level dependencies.
 */
@Module
public class ApplicationModule {
    protected final Application mApplication;

    public ApplicationModule(Application application) {
        mApplication = application;
    }

    @Provides
    Application provideApplication() {
        return mApplication;
    }

    @Provides
    @ApplicationContext
    Context provideContext() {
        return mApplication;
    }

    @Provides
    @Singleton
    Bus provideEventBus() {
        return new Bus();
    }

    @Provides
    @Singleton
    SwibrsService provideSwibrsService() {
        return SwibrsService.Creator.newSwibrsService();
    }

    @Provides
    @Singleton
    OcrService provideOcrService() {
        return OcrService.Creator.newOcrService();
    }

}
