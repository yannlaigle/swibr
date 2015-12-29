package com.swibr.app.test.common.injection.component;

import javax.inject.Singleton;

import dagger.Component;
import com.swibr.app.injection.component.ApplicationComponent;
import com.swibr.app.test.common.injection.module.ApplicationTestModule;

@Singleton
@Component(modules = ApplicationTestModule.class)
public interface TestComponent extends ApplicationComponent {

}
