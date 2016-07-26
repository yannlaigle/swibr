package com.swibr.app.ui.capture;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Binder;
import android.os.IBinder;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.swibr.app.R;
import com.swibr.app.data.model.Article;
import com.swibr.app.ui.main.MainActivity;

import java.io.File;

/**
 * Created by hthetiot on 12/29/15.
 */
public class CaptureService extends Service {

    private static final String TAG = CaptureService.class.getName();

    private ImageView floatingBtn;
    private WindowManager mWindowManager;
    private IBinder mBinder = new MyBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    public class MyBinder extends Binder {
        CaptureService getService() {
            return CaptureService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // TODO
        // - set run false

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        startNotification();
        renderFloatingBtn();
    }

    /**
     * Render Floating Button for Capture
     */
    protected void renderFloatingBtn() {

        // TODO
        // - add screen rotation support ?

        floatingBtn = new ImageView(this);
        floatingBtn.setImageResource(R.drawable.btn_standby);

        WindowManager windowSize = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display displaySize = windowSize.getDefaultDisplay();

        Point sizePoint = new Point();
        displaySize.getSize(sizePoint);
        final int widthPoint = sizePoint.x;
        final int heightPoint = sizePoint.y;

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = heightPoint;

        floatingBtn.setOnTouchListener(new View.OnTouchListener() {

            private int MIN_DISTANCE = widthPoint * 30 / 100;

            private WindowManager.LayoutParams paramsF = params;
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private float deltaX;
            private float x1;
            private float x2;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        x1 = event.getRawX();
                        initialX = paramsF.x;
                        initialY = paramsF.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        break;

                    case MotionEvent.ACTION_MOVE:
                        floatingBtn.setImageResource(R.drawable.btn_active);
                        paramsF.x = initialX + (int) (event.getRawX() - initialTouchX) - 200;
                        paramsF.y = initialY + (int) (event.getRawY() - initialTouchY) - 200;
                        mWindowManager.updateViewLayout(floatingBtn, paramsF);
                        break;

                    case MotionEvent.ACTION_UP:
                        floatingBtn.setImageResource(R.drawable.btn_standby);
                        paramsF.x = 0;
                        paramsF.y = heightPoint;
                        mWindowManager.updateViewLayout(floatingBtn, paramsF);

                        x2 = event.getRawX();
                        deltaX = x2 - x1;
                        if (Math.abs(deltaX) > MIN_DISTANCE) {
                            requestCapture();
                        }
                        break;
                }

                return false;
            }
        });

        mWindowManager.addView(floatingBtn, params);
    }

    /**
     * Start Capture Activity
     */
    private void requestCapture() {
        Intent i = new Intent();
        i.setClass(CaptureService.this, CaptureActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    /**
     * Start Service Notification
     */
    private void startNotification() {

        // TODO
        // - remove notification when service stop

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        final Notification.Builder builder = new Notification.Builder(this);
        builder.setCategory(Notification.CATEGORY_SERVICE)
                .setVisibility(-1)
                .setAutoCancel(false)
                .setContentTitle(getString(R.string.ServiceNofif))
                .setContentText(getString(R.string.ServiceNofifText))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(contentIntent);

        Notification swibrNotification = builder.build();
        swibrNotification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_FOREGROUND_SERVICE | Notification.FLAG_NO_CLEAR;
        startForeground(2, swibrNotification);
    }

    /**
     * Create a capture notification
     * @param image
     */
    private void captureNotification(Article article, File image) {

        // TODO
        // - share
        // - open
        // - cancel

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        final Notification.Builder builder = new Notification.Builder(this);
        builder.setCategory(Notification.CATEGORY_SERVICE)
                .setVisibility(-1)
                .setAutoCancel(false)
                .setContentTitle(getString(R.string.CaptureNofif))
                .setContentText(getString(R.string.CaptureNofifText))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(contentIntent);

        Notification swibrNotification = builder.build();
        swibrNotification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_FOREGROUND_SERVICE | Notification.FLAG_NO_CLEAR;
        startForeground(3, swibrNotification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // TODO
        // - set run false
        // - stop service notification

        if (floatingBtn != null) {
            mWindowManager.removeView(floatingBtn);
        }
    }
}
