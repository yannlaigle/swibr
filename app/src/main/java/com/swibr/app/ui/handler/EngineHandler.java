package com.swibr.app.ui.handler;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.JsonSyntaxException;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.ResponseBody;
import com.swibr.app.R;
import com.swibr.app.data.DataManager;
import com.swibr.app.data.model.Article.Article;
import com.swibr.app.data.model.Article.ArticleAdapter;
import com.swibr.app.data.model.Haven.HavenAdapter;
import com.swibr.app.data.model.Haven.TextResult;
import com.swibr.app.data.remote.HavenOCRService.HavenOcr;
import com.swibr.app.data.remote.SwibrsService;
import com.swibr.app.ui.base.BaseActivity;
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
 * Created by Shide on 29/07/2016.
 */
public class EngineHandler {

    private final String TAG = getClass().toString();
    private final PackageManager mPackageManager;
    private final Context mContext;
    private final File mPicturesDirectory;
    private int mWidth;
    private int mHeight;

    private Call<ResponseBody> mEngineCall;
    private Call<ResponseBody> mHavenCall;

    @Inject
    HavenOcr mHavenOcrService;
    @Inject
    DataManager mDataManager;
    @Inject
    SwibrsService mSwibrService;

    public EngineHandler(BaseActivity activity) {
        mPackageManager = activity.getPackageManager();
        // Get the default public pictures directory
        mPicturesDirectory = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);

        mContext = activity.getApplicationContext();

        // Create folder if missing
        File storeDirectory = new File(mPicturesDirectory.getAbsolutePath());
        if (!storeDirectory.exists()) {
            boolean success = storeDirectory.mkdirs();
            if (!success) {
                Toast.makeText(activity, R.string.CaptureFailedAccessDirectory, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to create pictures directory.");
                activity.finish();
            }
        }

        Point size = new Point();
        activity.getWindowManager().getDefaultDisplay().getSize(size);
        mWidth = size.x;
        mHeight = size.y;
    }

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
        ApplicationInfo appInfo = getLastUsedPackage();

        if (appInfo != null) {
            //TODO : save swibe localy?
            //TODO : get intent info such as Bundle savedState
            //TODO : post info to engine
            //TODO : remove try/catch

            Log.d(TAG, "onImageAvailable: Saving app package info");

            Intent lastApp = mPackageManager.getLaunchIntentForPackage(appInfo.packageName);
            Article article = new Article();

            article.type = "application";
            article.packageName = appInfo.packageName;
            article.bundle = SerializeBundle(lastApp.getExtras());

            mDataManager.addSwibr(article);

        } else {
            Log.d(TAG, "parseImageReader: appInfo null, starting img analysis");
            analyzeImageFile(newImage);
        }


        // Stop Capture
        reader.close();

        Log.d(TAG, "Swibr image completed: " + newImage.getAbsolutePath());
        Toast.makeText(mContext, R.string.CaptureSucceeded, Toast.LENGTH_LONG).show();
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
        UsageStatsManager mUsageStatsManager = (UsageStatsManager) mContext.getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        // We get usage stats for the last 10 seconds
        List<UsageStats> stats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 60, time);

        if (stats == null) {
            Log.e(TAG, "getLastUsedPackage: Could not get statistics info");
            Toast.makeText(mContext, "Impossible de récupérer vos statistiques d'utilisation", Toast.LENGTH_LONG).show();
            return null;
        }

        // Sort the stats by the last time used
        SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
        for (UsageStats usageStats : stats) {
            if (!usageStats.getPackageName().equals("com.swibr.app"))
                mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
        }

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

        String mode = mContext.getString(R.string.havenondemand_ocr_mode);
        String apikey = mContext.getString(R.string.havenondemand_apikey);
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
