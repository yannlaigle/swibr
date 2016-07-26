package com.swibr.app.ui.main;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Switch;

import com.swibr.app.R;
import com.swibr.app.data.SyncService;
import com.swibr.app.data.model.Article;
import com.swibr.app.ui.base.BaseActivity;
import com.swibr.app.ui.capture.CaptureService;
import com.swibr.app.ui.drawer.DrawerItemCustomAdapter;
import com.swibr.app.ui.drawer.ObjectDrawerItem;
import com.swibr.app.ui.tutorial.TutorialActivity;
import com.swibr.app.util.AndroidComponentUtil;
import com.swibr.app.util.DialogFactory;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends BaseActivity implements MainMvpView {

    private static final String TAG = CaptureService.class.getName();
    private static final int START_CAPTURE_SERVICE_REQUEST_CODE = 1000;
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

        // Render view
        setupDrawer();
        setupCaptureService();
        setupSwibrs();

        // Init other steps
        initTutorial();
        initUsageStats();
    }

    /**
     * Setup navigation drawer
     */
    private void setupDrawer() {

        // TODO better design
        // - http://codetheory.in/android-navigation-drawer/
        // - http://blog.teamtreehouse.com/add-navigation-drawer-android
        final Context context = this;

        mDrawerBtn = (ImageButton)findViewById(R.id.drawer_btn);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // Create menu items
        ObjectDrawerItem[] drawerItem = new ObjectDrawerItem[4];
        drawerItem[0] = new ObjectDrawerItem(R.drawable.ic_folder_special_black_24dp, getString(R.string.DrawerItemCategory));
        drawerItem[1] = new ObjectDrawerItem(R.drawable.ic_settings_black_24dp, getString(R.string.DrawerItemSettings));
        drawerItem[2] = new ObjectDrawerItem(R.drawable.ic_help_black_24dp, getString(R.string.DrawerItemTutorial));
        drawerItem[3] = new ObjectDrawerItem(R.drawable.ic_feedback_black_24dp, getString(R.string.DrawerItemFeedback));

        // Set the adapter for the list view
        DrawerItemCustomAdapter adapter = new DrawerItemCustomAdapter(context, R.layout.drawer_list_item, drawerItem);
        mDrawerList.setAdapter(adapter);

        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {

                switch (position) {
                    case 0:
                        startCategoryPage();
                        break;

                    case 1:
                        startUsageStats();
                        break;

                    case 2:
                        startTutorial();
                        break;

                    case 3:
                        startFeedback();
                        break;
                }

                mDrawerList.setItemChecked(position, true);
                mDrawerList.setSelection(position);
                mDrawerLayout.closeDrawer(mDrawerList);
            }
        });

        // Set the button's click listener
        mDrawerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                mDrawerLayout.openDrawer(Gravity.LEFT);
            }
        });
    }

    /**
     * Start and setup Capture service and start service btn.
     */
    private void setupCaptureService() {

        final Context context = this;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final boolean captureServiceEnabled = prefs.getBoolean("runCaptureService", false);
        final boolean isCaptureServiceRunning = AndroidComponentUtil.isServiceRunning(context, CaptureService.class);

        final Switch floatingSwitch = (Switch) findViewById(R.id.floating_switch);
        floatingSwitch.setChecked(captureServiceEnabled);
        floatingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                // Update runCaptureService value
                SharedPreferences.Editor prefEdit = prefs.edit();
                prefEdit.putBoolean("runCaptureService", isChecked);
                prefEdit.commit();

                if (isChecked) {

                    // Start Service
                    startCaptureService();

                    // Display info to user that service is available now
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.FloatingSwitchDialog)
                            .setMessage(R.string.FloatingSwitchDialogText)
                            .setCancelable(false)
                            .setNeutralButton(R.string.FloatingSwitchDialogBtn, null)
                            .show();
                } else {
                    stopCaptureService();
                }
            }
        });

        // Start Service if not already running and active
        if (captureServiceEnabled) {
            startCaptureService();
        } else {
            stopCaptureService();
        }
    }

    /**
     * Start Capture service
     */
    public void startCaptureService() {

        final Context context = this;
        final boolean isCaptureServiceRunning = AndroidComponentUtil.isServiceRunning(context, CaptureService.class);

        // Service may on boot if enable see CaptureIntentReceiver
        if (!isCaptureServiceRunning) {

            // Check and request if needed DrawOverlay Permission required by Capture service for
            // displaying btn over UI for Android 6+.
            if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                            && !Settings.canDrawOverlays(context)
                    ) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));

                startActivityForResult(intent, START_CAPTURE_SERVICE_REQUEST_CODE);

            } else {
                startService(new Intent(context, CaptureService.class));
            }
        }
    }

    /**
     * Stop Capture service
     */
    public void stopCaptureService() {

        final Context context = this;
        final boolean isCaptureServiceRunning = AndroidComponentUtil.isServiceRunning(context, CaptureService.class);

        if (isCaptureServiceRunning) {
            stopService(new Intent(context, CaptureService.class));
        }
    }

    /**
     * Setup Swibr Data, UI and background service.
     */
    private void setupSwibrs() {

        // Start background sync
        if (getIntent().getBooleanExtra(EXTRA_TRIGGER_SYNC_FLAG, true)) {
            startService(SyncService.getStartIntent(this));
        }

        mRecyclerView.setAdapter(mSwibrsAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mMainPresenter.attachView(this);
        mMainPresenter.loadSwibrs();

        /**
         * Implement swipe on RecycleView to remove or store items
         */
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                int position = viewHolder.getAdapterPosition();
                if (swipeDir == ItemTouchHelper.RIGHT) {
                    // TODO Store Swibe
                }
                if (swipeDir == ItemTouchHelper.LEFT) {
                    // TODO Remove Swibe
                }
                mRecyclerView.getAdapter().notifyItemRemoved(position);
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;

                    Paint p = new Paint();
                    if (dX > 0) {
                        /* Store animation */
                        p.setARGB(255, 46, 204, 113);
                        c.drawRect((float) itemView.getLeft(), (float) itemView.getTop(), dX,
                                (float) itemView.getBottom(), p);
                    } else {
                        /* Remove animation */
                        p.setARGB(255, 231, 76, 60);
                        c.drawRect((float) itemView.getRight() + dX, (float) itemView.getTop(),
                                (float) itemView.getRight(), (float) itemView.getBottom(), p);
                    }

                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(mRecyclerView);

    }

    /**
     * Start tutorial if first start or pref enable otherwise load swibrs.
     */
    private void initTutorial() {

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final boolean runTutorialOnStart = prefs.getBoolean("runTutorialOnStart", true);

        if (runTutorialOnStart) {

            // Disable runTutorialOnStart is enable to avoid loop
            SharedPreferences.Editor prefEdit = prefs.edit();
            prefEdit.putBoolean("runTutorialOnStart", false);
            prefEdit.commit();

            startTutorial();
        }
    }

    /**
     * Start Tutorial Activity
     */
    public void startTutorial() {

        Intent tutorialIndent = new Intent();
        tutorialIndent.setClass(MainActivity.this, TutorialActivity.class);
        tutorialIndent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(tutorialIndent);
    }

    /**
     * Start Category Page
     */
    public void startCategoryPage() {
        // TODO expand categories above swibes list
        // - display like flipboard category : https://play.google.com/store/apps/details?id=flipboard.app&hl=fr
        // - onclick on Category, sort and refresh swibes
    }

    /**
     * Start Feedback Activity
     */
    public void startFeedback() {

        Intent Email = new Intent(Intent.ACTION_SEND);
        Email.setType("text/email");
        Email.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.FeedbackEmail)});
        Email.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.FeedbackSubject));
        Email.putExtra(Intent.EXTRA_TEXT, getString(R.string.FeedbackText));
        startActivity(Intent.createChooser(Email, getString(R.string.FeedbackTitle)));

        // Create the Intent
        final Context context = this;
        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
    }

    /**
     * Request user usage stats
     */
    private void initUsageStats() {

        final Context context = this;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final boolean isUsageStatsRequested = prefs.getBoolean("requestedUsageStats", false);
        final boolean isUsageStatsEnabled = AndroidComponentUtil.isUsageStatsEnabled(context);

        if(!isUsageStatsEnabled && !isUsageStatsRequested) {

            new AlertDialog.Builder(context)
                .setTitle(R.string.AuthDialog)
                .setMessage(R.string.AuthDialogText)
                .setCancelable(true)
                .setNegativeButton(R.string.AuthDialogCancelBtn, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        // Update requestedUsageStats value
                        SharedPreferences.Editor prefEdit = prefs.edit();
                        prefEdit.putBoolean("requestedUsageStats", true);
                        prefEdit.commit();
                    }
                })
                .setPositiveButton(R.string.AuthDialogSettings, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        // Update requestedUsageStats value
                        SharedPreferences.Editor prefEdit = prefs.edit();
                        prefEdit.putBoolean("requestedUsageStats", true);
                        prefEdit.commit();

                        startUsageStats();
                    }
                })
                .show();
        }
    }

    public void startUsageStats() {

        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivity(intent);
    }

    /**
     * Handle onActivityResult
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode,  Intent data) {

        final Context context = this;

        // Handle DrawOverlay Permission result for Android 6+
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                requestCode == START_CAPTURE_SERVICE_REQUEST_CODE
        ) {
            if (Settings.canDrawOverlays(context)) {
                startService(new Intent(context, CaptureService.class));
            }
        }
    }

    /***** MVP View methods implementation *****/

    @Override
    public void showSwibrs(List<Article> articles) {
        mSwibrsAdapter.setSwibrs(articles);
        mSwibrsAdapter.notifyDataSetChanged();
    }

    @Override
    public void showError() {
        DialogFactory.createGenericErrorDialog(this, getString(R.string.error_loading))
                .show();
    }

    @Override
    public void showSwibrsEmpty() {
        mSwibrsAdapter.setSwibrs(Collections.<Article>emptyList());
        mSwibrsAdapter.notifyDataSetChanged();
    }
}
