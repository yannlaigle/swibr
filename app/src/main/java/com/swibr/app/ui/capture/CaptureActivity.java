package com.swibr.app.ui.capture;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;

import com.swibr.app.R;
import com.swibr.app.ui.main.MainActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by hthetiot on 12/29/15.
 */
public class CaptureActivity extends Activity {

    private static final String TAG = CaptureActivity.class.getName();

    private static int REQUEST_CODE = 1001;

    private MediaProjectionManager mProjectionManager;
    private MediaProjection mMediaProjection;
    private Handler mHandler;

    private File mPicturesDirectory;
    private ImageReader mImageReader;
    private int mWidth;
    private int mHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        // Save all photos in the default public pictures directory
        mPicturesDirectory = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);

        // Create folder if missing
        File storeDirectory = new File(mPicturesDirectory.getAbsolutePath().toString());
        if (!storeDirectory.exists()) {
            boolean success = storeDirectory.mkdirs();
            if(!success) {
                Toast.makeText(this, "Failed to access pictures directory.", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Failed to create pictures directory.");
                finish();
                return;
            }
        }

        startCapture();
    }

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

                // Save the next available image
                File newImage = saveImage(bitmap);

                // Tell the framework, so the image will be in the gallery
                MediaScannerConnection.scanFile(context,
                    new String[]{newImage.getAbsolutePath()},
                    new String[]{"image/png"},
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i(TAG, "Scanned " + path + ":");
                            Log.i(TAG, "-> uri=" + uri);
                        }
                    });

                Log.i(TAG, "Swibr image completed: " + newImage.getAbsolutePath());
                Toast.makeText(context, "Swibr capture succeeded!", Toast.LENGTH_LONG).show();

                // Stop Capture
                mImageReader.close();

            } catch (Exception e) {
                Log.e(TAG, "Swibr image capture fail cause", e);
                Toast.makeText(context, "Swibr capture failed!", Toast.LENGTH_LONG).show();
            }
        }
    }


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
                    e.printStackTrace();
                }
            }
        }

        return dest;
    }

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
}
