package com.swibr.app.data;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class SearchProviderTask extends AsyncTask<Uri, Void, List<String>> {

    private Context mContext;
    private String TAG = "SearchProviderTask";

    public SearchProviderTask(Context context) {
        super();
        mContext = context;
    }

    @Override
    protected List<String> doInBackground(Uri... params) {
        List<String> contentProviders = new ArrayList<String>();

        try {
            PackageManager pm = mContext.getPackageManager();
            for (PackageInfo pack : pm.getInstalledPackages(PackageManager.GET_PROVIDERS)) {
                ProviderInfo[] providers = pack.providers;
                if (providers != null) {
                    for (ProviderInfo provider : providers) {
                        contentProviders.add("content://" + provider.authority);
                    }
                }
            }
        } catch (Exception e) {
            // PackageManager has died?
            Log.e(TAG, e.getMessage());
        }

        // Sort alphabetically and ignore case sensitivity
        Collections.sort(contentProviders, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                return lowerCase(lhs).compareTo(lowerCase(rhs));
            }

            private String lowerCase(String s) {
                return s.toLowerCase(Locale.getDefault());
            }
        });
        return contentProviders;
    }
}
