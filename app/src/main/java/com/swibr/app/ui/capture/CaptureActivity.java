package com.swibr.app.ui.capture;

import android.content.Context;
import android.content.Intent;
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
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.ResponseBody;
import com.swibr.app.R;
import com.swibr.app.data.DataManager;
import com.swibr.app.data.model.Name;
import com.swibr.app.data.model.Profile;
import com.swibr.app.data.model.Swibr;
import com.swibr.app.data.remote.OcrService;
import com.swibr.app.ui.base.BaseActivity;
import com.swibr.app.util.AndroidComponentUtil;
import com.swibr.app.util.ProgressRequestBody;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.UUID;

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

    private static final String TAG = CaptureActivity.class.getName();
    private static final int REQUEST_CODE = 1001;

    private MediaProjectionManager mProjectionManager;
    private MediaProjection mMediaProjection;
    private Handler mHandler;

    private File mPicturesDirectory;
    private ImageReader mImageReader;
    private int mWidth;
    private int mHeight;

    @Inject DataManager mDataManager;
    @Inject OcrService mOcrService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        getActivityComponent().inject(this);

        // Set Transparent layout
        setContentView(R.layout.activity_capture);

        // Get the default public pictures directory
        mPicturesDirectory = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);

        // Create folder if missing
        File storeDirectory = new File(mPicturesDirectory.getAbsolutePath().toString());
        if (!storeDirectory.exists()) {
            boolean success = storeDirectory.mkdirs();
            if (!success) {
                Toast.makeText(this, R.string.CaptureFailedAccessDirectory, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to create pictures directory.");
                finish();
                return;
            }
        }

        // Emulate capture on emulator
        if (AndroidComponentUtil.isRunningOnEmulator()) {
            startCaptureTest();

        // Run real capture on Device
        } else {
            startCapture();
        }
    }

    /**
     * Hanle Media Projection result.
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
                mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), mHandler);

                moveTaskToBack(true);
                stopCapture();
                finish();
            }
        }
    }

    /**
     * Handle ImageReader result
     */
    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {

            Context context = CaptureActivity.this;

            try {

                Image image = mImageReader.acquireLatestImage();
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

                // Save the image as capture
                publishCapture(newImage);

                Log.i(TAG, "Swibr image completed: " + newImage.getAbsolutePath());
                Toast.makeText(context, R.string.CaptureSucceeded, Toast.LENGTH_LONG).show();

                // Stop Capture
                mImageReader.close();

            } catch (Exception e) {
                Log.e(TAG, "Swibr image capture fail cause", e);
                Toast.makeText(context, R.string.CaptureFailed, Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Dummy Capture for Emulator and UnitTest.
     */
    private void startCaptureTest() {

        Log.i(TAG, "startCaptureTest");

        Context context = CaptureActivity.this;

        try {

            // Save the bitmap as image
            File newImage = getTestImage();

            // Save the image as capture
            publishCapture(newImage);

            Log.i(TAG, "[TEST] Swibr image completed: " + newImage.getAbsolutePath());
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

        Log.i(TAG, "startCapture");

        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);

        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                Log.i(TAG, "stopCapture: started");
                mHandler = new Handler();
                Looper.loop();
            }
        }.start();
    }

    private void stopCapture() {

        Log.i(TAG, "stopCapture");

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mMediaProjection != null) {

                    Log.i(TAG, "stopCapture: stoped");
                    mMediaProjection.stop();
                }
            }
        });
    }

    /**
     * Publish Capture as a Swibr push for Analyze
     */
    private void publishCapture(File file) {

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

        Swibr newSwibr = new Swibr(profile);

        mDataManager.addSwibr(newSwibr)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe();

        // Analyze File
        analyzeImageFile(newSwibr, file);
    }

    /**
     * Run Image Capture Analyze
     * @param swibr
     * @param file
     */
    private void analyzeImageFile(Swibr swibr, File file) {

        ProgressRequestBody requestBody = ProgressRequestBody.createImage(
                MediaType.parse("multipart/form-data"),
                file,
                new ProgressRequestBody.UploadCallbacks() {

                    @Override
                    public void onProgressUpdate(String path, int percent) {
                        Log.i(TAG, path + "\t>>>" + percent);
                    }

                    @Override
                    public void onError(int position) {

                    }

                    @Override
                    public void onFinish(int position, String urlId) {

                    }
                }
        );

        String mode = getString(R.string.havenondemand_ocr_mode);
        String apikey = getString(R.string.havenondemand_apikey);
        Call<ResponseBody> call = mOcrService.upload(requestBody, mode, apikey);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Response<ResponseBody> response, Retrofit retrofit) {

                try {
                    String jsonData = response.body().string();
                    Log.v(TAG, jsonData.toString());

                } catch (IOException e) {
                    Log.v(TAG, "Exception caught : ", e);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e("uploadImageFile", "onFailure",  t);
            }
        });
    }

    /**
     * Generate a dummy capture using a template image and write some random text on top of it.
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
        float x_coord = (src.getWidth() - width)/2;
        cs.drawText(yourText, x_coord, height+15f, tPaint); // 15f is to put space between top edge and the text, if you want to change it, you can

        return saveImage(bitmap);
    }

    /**
     * Generate capture file name and prepare file.
     * @return
     * @throws IOException
     */
    private File getImageFile() throws IOException {

        String filename = "Swibr_" + System.currentTimeMillis() + ".jpg";
        File file = new File(mPicturesDirectory, filename);

        if (!file.exists()) {
            boolean success = file.createNewFile();
            if(!success) {
                Log.e(TAG, "Failed to create file: " + filename);
            }
        }

        return file;
    }

    /**
     * Save capture on user device.
     * @param bitmap
     * @return
     * @throws IOException
     */
    private File saveImage(Bitmap bitmap) throws IOException {

        File dest = null;
        FileOutputStream output = null;

        try {

            dest = getImageFile();
            output = new FileOutputStream(dest);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);

        } catch (IOException e) {
            Log.e(TAG, "Swibr image save fail cause", e);
            throw e;

        } finally {

            if (null != output) {
                try {
                    output.close();
                } catch (IOException e) {
                    throw e;
                }
            }
        }

        return dest;
    }
}
