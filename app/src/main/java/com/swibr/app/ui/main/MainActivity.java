package com.swibr.app.ui.main;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.*;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import com.swibr.app.R;
import com.swibr.app.data.SyncService;
import com.swibr.app.data.model.Name;
import com.swibr.app.data.model.Profile;
import com.swibr.app.data.model.Swibr;
import com.swibr.app.ui.base.BaseActivity;
import com.swibr.app.ui.capture.CaptureService;
import com.swibr.app.util.AndroidComponentUtil;
import com.swibr.app.util.DialogFactory;

public class MainActivity extends BaseActivity implements MainMvpView {


    private static int REQUEST_CODE = 1000;
    private static final String TAG = CaptureService.class.getName();
    private static final String EXTRA_TRIGGER_SYNC_FLAG = "com.swibr.app.ui.main.MainActivity.EXTRA_TRIGGER_SYNC_FLAG";

    @Inject MainPresenter mMainPresenter;
    @Inject SwibrsAdapter mSwibrsAdapter;

    @Bind(R.id.recycler_view) RecyclerView mRecyclerView;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ImageButton mDrawerBtn;
    private String[] mDrawerTitles;

    /**
     * Return an Intent to start this Activity.
     * triggerDataSyncOnCreate allows disabling the background sync service onCreate. Should
     * only be set to false during testing.
     */
    public static Intent getStartIntent(Context context, boolean triggerDataSyncOnCreate) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(EXTRA_TRIGGER_SYNC_FLAG, triggerDataSyncOnCreate);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivityComponent().inject(this);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mRecyclerView.setAdapter(mSwibrsAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mMainPresenter.attachView(this);
        mMainPresenter.loadSwibrs();

        if (getIntent().getBooleanExtra(EXTRA_TRIGGER_SYNC_FLAG, true)) {
            startService(SyncService.getStartIntent(this));
        }


        initTutorial();

        setupDrawer();

        initUsageStats();
        initCaptureService();

        //addSwibr();
    }

    private void setupDrawer() {

        // TODO better design
        // - http://codetheory.in/android-navigation-drawer/
        // - http://blog.teamtreehouse.com/add-navigation-drawer-android

        mDrawerBtn = (ImageButton)findViewById(R.id.drawer_btn);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        mDrawerTitles = getResources().getStringArray(R.array.menu_items);
        
        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mDrawerTitles));
        // Set the list's click listener
        // mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        mDrawerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
            mDrawerLayout.openDrawer(Gravity.LEFT);
            }
        });
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {

            Toast.makeText(MainActivity.this, String.format("Menu Item %d", position), Toast.LENGTH_LONG).show();
        }
    }

    private void addSwibr() {

        // TODO
        // - http://open.blogs.nytimes.com/2014/08/18/getting-groovy-with-reactive-android/
        // - https://www.youtube.com/watch?v=k3D0cWyNno4
        // - https://github.com/square/sqlbrite/blob/master/sqlbrite-sample

        String uniqueSuffix = "r" +  UUID.randomUUID().toString();

        Name name = new Name();
        name.first = "Name-" + uniqueSuffix;
        name.last = "Surname-" + uniqueSuffix;

        Profile profile = new Profile();
        profile.email = "email" + uniqueSuffix + "@example.com";
        profile.name = name;
        profile.dateOfBirth = new Date();
        profile.hexColor = "#0066FF";
        profile.avatar = "http://api.ribot.io/images/" + uniqueSuffix;
        profile.bio = UUID.randomUUID().toString();

        Swibr swibr = new Swibr(profile);

        mMainPresenter.addSwibr(swibr);
    }

    protected void initTutorial() {

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final boolean runTutorialOnStart = prefs.getBoolean("runTutorialOnStart", true);

        if (runTutorialOnStart) {

            // Disable runTutorialOnStart is enable to avoid loop
            SharedPreferences.Editor prefEdit = prefs.edit();
            prefEdit.putBoolean("runTutorialOnStart", false);
            prefEdit.commit();

            Intent i = new Intent();
            i.setClass(MainActivity.this, TutotialActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }
    }

    protected void initUsageStats() {

        final Context context = this;
        boolean isUsageStatsEnabled = AndroidComponentUtil.isUsageStatsEnabled(context);

        if(!isUsageStatsEnabled) {

            new AlertDialog.Builder(this)
                .setTitle(R.string.AuthDialog)
                .setMessage(R.string.AuthDialogText)
                .setCancelable(true)
                .setNegativeButton(R.string.AuthDialogCancelBtn, null)
                .setPositiveButton(R.string.AuthDialogSettings, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                    }
                })
                .show();
        }
    }

    protected void initCaptureService() {

        // Service may on boot if enable see BootCompletedIntentReceiver

        final Context context = this;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final boolean captureServiceEnabled = prefs.getBoolean("runCaptureService", false);

        // Start Service if not already running
        if (
            captureServiceEnabled &&
                AndroidComponentUtil.isServiceRunning(context, CaptureService.class) == false
        ) {

            checkDrawOverlayPermission();

            startService(new Intent(MainActivity.this, CaptureService.class));
        }

        final Switch floatingSwitch = (Switch) findViewById(R.id.floating_switch);
        floatingSwitch.setChecked(captureServiceEnabled);
        floatingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                // Update CaptureService value
                SharedPreferences.Editor prefEdit = prefs.edit();
                prefEdit.putBoolean("runCaptureService", isChecked);
                prefEdit.commit();

                if (isChecked) {

                    checkDrawOverlayPermission();

                    new AlertDialog.Builder(context)
                            .setTitle(R.string.FloatingSwitchDialog)
                            .setMessage(R.string.FloatingSwitchDialogText)
                            .setCancelable(false)
                            .setNeutralButton(R.string.FloatingSwitchDialogBtn, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                startService(new Intent(MainActivity.this, CaptureService.class));
                                }
                            })
                            .show();
                } else {
                    stopService(new Intent(MainActivity.this, CaptureService.class));
                }
            }
        });
    }

    public void checkDrawOverlayPermission () {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,  Intent data) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (requestCode == REQUEST_CODE) {
                if (Settings.canDrawOverlays(this)) {
                    startService(new Intent(MainActivity.this, CaptureService.class));
                }
            }
        }
    }

    /***** MVP View methods implementation *****/

    @Override
    public void showSwibrs(List<Swibr> swibrs) {
        mSwibrsAdapter.setSwibrs(swibrs);
        mSwibrsAdapter.notifyDataSetChanged();
    }

    @Override
    public void showError() {
        DialogFactory.createGenericErrorDialog(this, getString(R.string.error_loading_ribots))
                .show();
    }

    @Override
    public void showSwibrsEmpty() {
        mSwibrsAdapter.setSwibrs(Collections.<Swibr>emptyList());
        mSwibrsAdapter.notifyDataSetChanged();
        Toast.makeText(this, R.string.empty_ribots, Toast.LENGTH_LONG).show();
    }

}
