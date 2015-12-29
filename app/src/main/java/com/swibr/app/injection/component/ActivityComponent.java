package com.swibr.app.injection.component;

import dagger.Component;
import com.swibr.app.injection.PerActivity;
import com.swibr.app.injection.module.ActivityModule;
import com.swibr.app.ui.main.MainActivity;

/**
 * This component inject dependencies to all Activities across the application
 */
@PerActivity
@Component(dependencies = ApplicationComponent.class, modules = ActivityModule.class)
public interface ActivityComponent {

    void inject(MainActivity mainActivity);

}
