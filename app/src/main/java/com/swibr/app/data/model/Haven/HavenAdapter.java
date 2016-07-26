package com.swibr.app.data.model.Haven;

import android.util.Log;

import com.google.gson.Gson;

/**
 * Created by Shide on 25/07/2016.
 */
public class HavenAdapter {

    public final static String TAG = "HavenAdapter";

    public static TextResult fromJson(String content) {
        //Check response contenttype is Json
        Gson gson = new Gson();
        TextResult textResult = gson.fromJson(content, TextResult.class);
        Log.d(TAG, "JSON objects decoded : " + textResult.text_block.length);
        return textResult;
    }


    public static String toJsonString(TextResult textResult) {
        Gson gson = new Gson();
        return gson.toJson(textResult.text_block, TextBlock[].class);

    }
}
