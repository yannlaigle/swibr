package com.swibr.app.injection.component;

import android.app.Application;
import android.content.Context;

import com.squareup.otto.Bus;

import javax.inject.Singleton;

import dagger.Component;
import com.swibr.app.data.DataManager;
import com.swibr.app.data.SyncService;
import com.swibr.app.data.local.DatabaseHelper;
import com.swibr.app.data.local.PreferencesHelper;
import com.swibr.app.data.remote.SwibrsService;
import com.swibr.app.injection.ApplicationContext;
import com.swibr.app.injection.module.ApplicationModule;

@Singleton
@Component(modules = ApplicationModule.class)
public interface ApplicationComponent {

    void inject(SyncService syncService);

    @ApplicationContext Context context();
    Application application();
    SwibrsService ribotsService();
    PreferencesHelper preferencesHelper();
    DatabaseHelper databaseHelper();
    DataManager dataManager();
    Bus eventBus();

}
