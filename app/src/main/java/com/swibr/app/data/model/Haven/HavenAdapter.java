package com.swibr.app.data.model.Haven;

import android.util.Log;

import com.google.gson.Gson;

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
        String regexpChar = "[^\\p{L}\\p{Nd} \\n]+";
        for (TextBlock block : textResult.text_block) {
            block.text = block.text.replaceAll(regexpChar, "");//removing weird characters
        }
        return gson.toJson(textResult.text_block, TextBlock[].class);

    }
}
