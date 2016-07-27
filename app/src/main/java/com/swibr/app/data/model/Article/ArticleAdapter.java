package com.swibr.app.data.model.Article;


import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class ArticleAdapter {

    public final static String TAG = "HavenAdapter";

    public static Article fromJson(String content) throws JsonSyntaxException {
        //Check response contenttype is Json
        Gson gson = new Gson();
        Article article = gson.fromJson(content, Article.class);
        Log.d(TAG, "JSON objects decoded : " + article.title);
        return article;
    }

    public static String toJsonString(Article article) {
        Gson gson = new Gson();
        return gson.toJson(article, Article.class);

    }
}
