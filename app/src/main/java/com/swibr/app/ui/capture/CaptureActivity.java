package com.swibr.app.ui.capture;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;

import com.google.gson.JsonSyntaxException;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.ResponseBody;
import com.swibr.app.R;
import com.swibr.app.data.DataManager;
import com.swibr.app.data.local.PreferencesHelper;
import com.swibr.app.data.model.Article.Article;
import com.swibr.app.data.model.Article.ArticleAdapter;
import com.swibr.app.data.model.Haven.HavenAdapter;
import com.swibr.app.data.model.Haven.TextResult;
import com.swibr.app.data.remote.HavenOCRService.HavenOcr;
import com.swibr.app.data.remote.SwibrsService;
import com.swibr.app.ui.base.BaseActivity;
import com.swibr.app.ui.handler.EngineHandler;
import com.swibr.app.util.AndroidComponentUtil;
import com.swibr.app.util.ProgressRequestBody;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.inject.Inject;

import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

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
    private PackageManager mPackageManager;

    private ImageReader mImageReader;
    private int mWidth;
    private int mHeight;

    private ImageReader.OnImageAvailableListener imageAvailableListener;
    private PreferencesHelper mPrefHelper;
    private File mPicturesDirectory;
//    private EngineHandler mEngineHandler;


    private Call<ResponseBody> mEngineCall;
    private Call<ResponseBody> mHavenCall;

    @Inject
    HavenOcr mHavenOcrService;
    @Inject
    DataManager mDataManager;
    @Inject
    SwibrsService mSwibrService;

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


        mPackageManager = getPackageManager();
        // Get the default public pictures directory
        mPicturesDirectory = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);

        // Create folder if missing
        File storeDirectory = new File(mPicturesDirectory.getAbsolutePath());
        if (!storeDirectory.exists()) {
            boolean success = storeDirectory.mkdirs();
            if (!success) {
                Toast.makeText(this, R.string.CaptureFailedAccessDirectory, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to create pictures directory.");
                finish();
            }
        }

        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        mWidth = size.x;
        mHeight = size.y;


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
                        parseImageReader(reader);
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
            analyzeImageFile(newImage);

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


    /**
     * Parse the onImageAvailble result
     */
    public void parseImageReader(ImageReader reader) {

        Log.d(TAG, "parseImageReader: Starting");
        Image image = reader.acquireLatestImage();
        Image.Plane[] planes = image.getPlanes();

        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * mWidth;

        // Create bitmap
        Bitmap bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);

        // Save the bitmap as image
        File newImage = saveImage(bitmap);
        ApplicationInfo appInfo = null;

        try {
            appInfo = getLastUsedPackage();
        } catch (Exception e) {
            Log.e(TAG, "parseImageReader: Exception", e);
        }

        Boolean saved = false;
        if (appInfo != null) {
            //TODO : get intent info such as Bundle savedState
            //TODO : post info to engine?

            Log.d(TAG, "onImageAvailable: Saving app package info");

            Intent lastApp = mPackageManager.getLaunchIntentForPackage(appInfo.packageName);
            if (lastApp != null) {

                Article article = new Article();

                article.type = "application";
                article.packageName = appInfo.packageName;
                article.bundle = SerializeBundle(lastApp.getExtras());

                saved = !article.bundle.equals("");
                if (saved)
                    mDataManager.addSwibr(article);

            } else
                Log.d(TAG, "parseImageReader: Could not get last app Intent : " + appInfo.packageName);
        }

        if (!saved) {
            Log.d(TAG, "parseImageReader: appInfo null, starting img analysis");
            analyzeImageFile(newImage);
        }

        // Stop Capture
        reader.close();

        Log.d(TAG, "Swibr image completed: " + newImage.getAbsolutePath());
        Toast.makeText(this, R.string.CaptureSucceeded, Toast.LENGTH_LONG).show();
    }

    private String SerializeBundle(Bundle bundle) {
        String serialized = "";
        Parcel parcel = Parcel.obtain();

        try {
            bundle.writeToParcel(parcel, 0);

            ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
            IOUtils.write(parcel.marshall(), byteArray);

            return Base64.encodeToString(byteArray.toByteArray(), 0);

        } catch (Exception e) {
            Log.e(TAG, "SerializeBundle: Exception", e);
        } finally {
            parcel.recycle();
        }

        return serialized;

    }

    private ApplicationInfo getLastUsedPackage() {

        //noinspection ResourceType
        UsageStatsManager mUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        // We get usage stats for the last 10 seconds
        List<UsageStats> stats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 60, time);

        if (stats == null) {
            Log.e(TAG, "getLastUsedPackage: Could not get statistics info");
            Toast.makeText(this, "Impossible de récupérer vos statistiques d'utilisation", Toast.LENGTH_LONG).show();
            return null;
        }

        // Sort the stats by the last time used
        SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
        for (UsageStats usageStats : stats) {
            if (!usageStats.getPackageName().equals("com.swibr.app"))
                mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
        }
        if (mySortedMap.isEmpty())
            return null;

        UsageStats lastPackageUsed = mySortedMap.get(mySortedMap.lastKey());

        //get a list of installed apps.
        List<ApplicationInfo> packages = mPackageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo packageInfo : packages) {
            if (lastPackageUsed.getPackageName().equals(packageInfo.packageName)) {
                Log.d(TAG, "Installed package :" + packageInfo.packageName);
                Log.d(TAG, "Source dir : " + packageInfo.sourceDir);
                //Log.d(TAG, "Launch Activity :" + packageInfo.getLaunchIntentForPackage(packageInfo.packageName));
                return packageInfo;
            }
        }

        return null;
    }

    /**
     * Run Image Capture Analyze
     *
     * @param file
     */
    public void analyzeImageFile(File file) {

        if (file == null) {
            Log.e(TAG, "analyzeImageFile: Cannot parse null file");
            return;
        }

        ProgressRequestBody requestBody = ProgressRequestBody.createImage(
                MediaType.parse("multipart/form-data"),
                file,
                new ProgressRequestBody.UploadCallbacks() {

                    @Override
                    public void onProgressUpdate(String path, int percent) {

                    }

                    @Override
                    public void onError(int position) {
                        Log.e(TAG, "haven onProgressUpdate Error at position : " + String.valueOf(position));
                    }

                    @Override
                    public void onFinish(int position, String urlId) {
                        Log.d(TAG, "Finished saving picture : " + urlId);
                    }
                }
        );

        String mode = getString(R.string.havenondemand_ocr_mode);
        String apikey = getString(R.string.havenondemand_apikey);
        String[] languages = new String[1];
        languages[0] = "fr";

        mHavenCall = mHavenOcrService.upload(requestBody, mode, languages, apikey);


        Log.d(TAG, "makeHavenCall: Preparing");
        mHavenCall.enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(Response<ResponseBody> response, Retrofit retrofit) {
                TextResult textResult = null;
                if (response.body() == null) {
                    Log.e(TAG, "HavenCall onResponse: Null response from server");
                    return;
                }

                try {
                    String content = response.body().string();

                    Log.d(TAG, "Body response : " + content);
                    textResult = HavenAdapter.fromJson(content);

                } catch (IOException e) {
                    Log.e(TAG, "AnalyzeImageFile IOException caught : ", e);
                }
                Log.d(TAG, "Going for engine call");

                if (textResult != null) {
                    makeEngineCall(textResult);
                } else
                    Log.d(TAG, "Canceled engine call");
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e("analyzeImageFile", "onFailure", t);
            }
        });
    }


    private void makeEngineCall(TextResult textResult) {

        String json = HavenAdapter.toJsonString(textResult);

        mEngineCall = mSwibrService.search(json);

        mEngineCall.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Response<ResponseBody> response, Retrofit retrofit) {
                String content = null;
                Article article = null;

                try {
                    content = response.body().string();
                    Log.d(TAG, "EngineCall onResponse : " + content);
                } catch (IOException e) {
                    Log.e(TAG, "EngineCall onResponse: IOException caught : ", e);
                } catch (Exception e) {
                    Log.e(TAG, "EngineCall onResponse: Something bad happened", e);
                }

                if (content == null) return;

                try {
                    article = ArticleAdapter.fromJson(content);
                } catch (JsonSyntaxException e) {
                    Log.e(TAG, "EngineCall: JsonSyntaxException", e);
                }

                if (article == null) return;

                Log.d(TAG, "EngineCall: Saving article");

                mDataManager.addSwibr(article)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe();
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e("Engine call", "onFailure", t);
            }
        });
    }

}
