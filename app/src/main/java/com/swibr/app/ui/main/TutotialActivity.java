package com.swibr.app.ui.main;

import android.content.Intent;
import android.os.Bundle;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import com.swibr.app.R;
import com.swibr.app.data.SyncService;
import com.swibr.app.ui.base.BaseActivity;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by hthetiot on 12/29/15.
 */
public class TutotialActivity extends BaseActivity {

    @Bind(R.id.exit_tuto) Button mExitTuto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivityComponent().inject(this);
        setContentView(R.layout.activity_tutorial);
        ButterKnife.bind(this);

        // TODO use viewflipper
        // - http://examples.javacodegeeks.com/android/core/widget/viewflipper/android-viewflipper-example/

        mExitTuto.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                exitTutorial();
            }
        });
    }

    private void exitTutorial() {
        Intent i = new Intent();
        i.setClass(TutotialActivity.this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }
}
