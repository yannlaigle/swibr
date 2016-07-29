package com.swibr.app.ui.capture;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;

import com.swibr.app.R;
import com.swibr.app.data.local.PreferencesHelper;
import com.swibr.app.ui.base.BaseActivity;
import com.swibr.app.ui.handler.EngineHandler;
import com.swibr.app.util.AndroidComponentUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by hthetiot on 12/29/15.
 * This Activity is used to Capture the screen of the user.
 */
public class CaptureActivity extends BaseActivity {

    private static final String TAG = "CaptureActivity";
    private static final int REQUEST_CODE = 1001;
    private static final String WRITE_PERMISSION = "android.permission.WRITE_EXTERNAL_STORAGE";
    private static final String READ_PERMISSION = "android.permission.READ_EXTERNAL_STORAGE";

    private MediaProjectionManager mProjectionManager;
    private MediaProjection mMediaProjection;
    private Handler mHandler;

    private ImageReader mImageReader;
    private int mWidth;
    private int mHeight;

    private ImageReader.OnImageAvailableListener imageAvailableListener;
    private PreferencesHelper mPrefHelper;
    private File mPicturesDirectory;
    private EngineHandler mEngineHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");
        getActivityComponent().inject(this);

        //Instanciate the PrefHelper
        mPrefHelper = new PreferencesHelper(this);

        // Set Transparent layout
        setContentView(R.layout.activity_capture);

        // Get the default public pictures directory
        mPicturesDirectory = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);

        mEngineHandler = new EngineHandler(this);

        // Emulate capture on emulator
        if (AndroidComponentUtil.isRunningOnEmulator()) {
            startCaptureTest();

            // Run real capture on Device
        } else {
            startCapture();
        }
    }
    /**
     * * Hanle Media Projection result.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult " + String.valueOf(requestCode) + " " + String.valueOf(resultCode));
        if (requestCode == REQUEST_CODE) {
            mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
            if (mMediaProjection != null) {

                Display display = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                mWidth = size.x;
                mHeight = size.y;

                DisplayMetrics metrics = getResources().getDisplayMetrics();
                int density = metrics.densityDpi;
                int flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;

                mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2);
                mMediaProjection.createVirtualDisplay("screencap", mWidth, mHeight, density, flags, mImageReader.getSurface(), null, mHandler);

                imageAvailableListener = new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader reader) {
                        mEngineHandler.parseImageReader(reader);
                    }
                };

                mImageReader.setOnImageAvailableListener(imageAvailableListener, mHandler);

                moveTaskToBack(true);
                stopCapture();
                finish();
            } else
                Log.d(TAG, "MediaProjection null");
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "Request Code : " + requestCode);
        switch (requestCode) {
            case 200:
                for (String permission : permissions) {
                    markAsAsked(permission);
                }

                boolean canWrite = permissions[0].equals(WRITE_PERMISSION) &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED;
                Log.d(TAG, String.valueOf(canWrite));

                if (canWrite)
                    startCapture();
                break;
        }
    }

    /**
     * Dummy Capture for Emulator and UnitTest.
     */
    private void startCaptureTest() {

        Log.d(TAG, "startCaptureTest");

        Context context = CaptureActivity.this;

        try {

            // Save the bitmap as image
            File newImage = getTestImage();

            // Analyze File
            mEngineHandler.analyzeImageFile(newImage);

            Log.d(TAG, "[TEST] Swibr image completed: " + newImage.getAbsolutePath());
            Toast.makeText(context, "[TEST] Swibr capture succeeded!", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e(TAG, "[TEST] Swibr image capture fail cause", e);
            Toast.makeText(context, "[TEST] Swibr capture failed!", Toast.LENGTH_LONG).show();
        }

        moveTaskToBack(true);
        finish();
    }

    /**
     * Start MediaProjection Capture
     */
    private void startCapture() {

        //For versions > Android 6.0 Marshmallow
        if (!hasPermission(WRITE_PERMISSION)) {
            if (shoudAskPermission(WRITE_PERMISSION)) {
                Log.d(TAG, "Asking for write permissions");
                String[] perms = {WRITE_PERMISSION, READ_PERMISSION};
                int permsRequestCode = 200;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(perms, permsRequestCode);
                    return;
                }
            } else {
                Log.d(TAG, "Unauthorized to start capture");
                Toast.makeText(this.getApplicationContext(), "Could not start capture", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Log.d(TAG, "startCapture");
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);

        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                Log.d(TAG, "Starting capture");
                mHandler = new Handler();
                Looper.loop();
            }
        }.start();
    }

    private void stopCapture() {

        Log.d(TAG, "stopCapture");

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mMediaProjection != null) {

                    Log.d(TAG, "stopCapture: stoped");
                    mMediaProjection.stop();
                }
            }
        });
    }

    private boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
        }
        return true;
    }

    private boolean shoudAskPermission(String permission) {
        return (mPrefHelper.getBoolean(permission, true));
    }

    private void markAsAsked(String permission) {
        mPrefHelper.putBoolean(permission, false);
    }

    /**
     * Generate a dummy capture using a template image and write some random text on top of it.
     *
     * @return
     * @throws IOException
     */
    private File getTestImage() throws IOException {

        Bitmap src = BitmapFactory.decodeResource(getResources(), R.drawable.test_capture); // the original file yourimage.jpg i added in resources
        Bitmap bitmap = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);

        String yourText = "My custom Text adding to Image";

        Canvas cs = new Canvas(bitmap);
        Paint tPaint = new Paint();
        tPaint.setTextSize(35);
        tPaint.setColor(Color.BLUE);
        tPaint.setStyle(Paint.Style.FILL);
        cs.drawBitmap(src, 0f, 0f, null);
        float height = tPaint.measureText("yY");
        float width = tPaint.measureText(yourText);
        float x_coord = (src.getWidth() - width) / 2;
        cs.drawText(yourText, x_coord, height + 15f, tPaint); // 15f is to put space between top edge and the text, if you want to change it, you can

        return saveImage(bitmap);
    }

    /**
     * Save capture on user device.
     *
     * @param bitmap
     * @return
     * @throws IOException
     */
    private File saveImage(Bitmap bitmap) {

        File dest = null;
        FileOutputStream output = null;

        try {

            dest = getImageFile();
            output = new FileOutputStream(dest);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);

        } catch (IOException e) {
            Log.e(TAG, "Swibr image save fail cause", e);


        } finally {

            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    Log.e(TAG, "saveImage: IOException", e);
                }
            }
        }

        return dest;
    }

    /**
     * Generate capture file name and prepare file.
     *
     * @return
     * @throws IOException
     */
    private File getImageFile() throws IOException {

        String filename = "Swibr_" + System.nanoTime() + ".jpg";
        File file = new File(mPicturesDirectory, filename);

        if (!file.exists()) {
            boolean success = file.createNewFile();
            if (!success) {
                Log.e(TAG, "Failed to create file: " + filename);
            }
        }

        return file;
    }

//
//    private Article getLastBrowserArticle() {
//        Article article = null;
//        List<String> providers = new ArrayList<>();
//
//        //TODO implement Browser history search url for android version < 6
//
//        Log.d(TAG, "getLastBrowserArticle");
//
//        try {
////            providers = searchProviderTask.get();
//        } catch (InterruptedException | ExecutionException e) {
//            Log.e(TAG, Arrays.toString(e.getStackTrace()));
//        }
//
//        for (String p : providers) {
//            Uri uri;
//            try {
//                uri = Uri.parse(p);
//                Log.d(TAG, "URI Provider : " + p);
//
//            } catch (NullPointerException e) {
//                Log.e(TAG, "Error : " + e.getMessage());
//            }
//        }
//
//
//        //TODO check what to do next
//
//        return article;
//    }


}
