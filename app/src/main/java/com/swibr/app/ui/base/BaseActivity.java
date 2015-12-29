package com.swibr.app.ui.base;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.swibr.app.SwibrApplication;
import com.swibr.app.injection.component.ActivityComponent;
import com.swibr.app.injection.component.DaggerActivityComponent;
import com.swibr.app.injection.module.ActivityModule;

public class BaseActivity extends AppCompatActivity {

    private ActivityComponent mActivityComponent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public ActivityComponent getActivityComponent() {
        if (mActivityComponent == null) {
            mActivityComponent = DaggerActivityComponent.builder()
                    .activityModule(new ActivityModule(this))
                    .applicationComponent(SwibrApplication.get(this).getComponent())
                    .build();
        }
        return mActivityComponent;
    }

}
