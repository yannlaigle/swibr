package com.swibr.app.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.swibr.app.injection.ApplicationContext;

@Singleton
public class PreferencesHelper  {

    public static final String PREF_FILE_NAME = "swibr_pref_file";

    private final SharedPreferences preferences;

    @Inject
    public PreferencesHelper(@ApplicationContext Context context) {
        preferences = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
    }

    public void clear() {
        preferences.edit().clear().apply();
    }

    /**
     * Return SharedPref Boolean Value
     * @param key
     * @param defaultValue
     * @return
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return preferences.getBoolean(key, defaultValue);
    }

    public void putBoolean(String key, boolean value) {
        preferences.edit().putBoolean(key, value).apply();
    }
}
