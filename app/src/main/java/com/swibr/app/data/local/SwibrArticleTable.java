package com.swibr.app.data.local;

import android.content.ContentValues;
import android.database.Cursor;

import com.swibr.app.data.model.Article.Article;

/**
 * Created by Shide on 23/07/2016.
 */
public abstract class SwibrArticleTable {
    /*{
        uuid:
        capture:
        state: capture, analyze, done
        creation:
        update:
        analyze: {
            text:
            keywords:
            colors:
            urls:
        },
        context:{
            appName:
            url:
            date:
        },
        profile: {
            uuid:
            device:
            location:
        }
    }
    */

    public static final String TABLE_NAME = "swibr_article";

    public static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_IMGURL = "imgurl";
    private static final String COLUMN_URL_ORIGIN = "urlorigin";
    private static final String COLUMN_URL_WEB = "urlweb";

    private static final String COLUMN_CREATED = "created";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_PACKAGE = "packageName";
    private static final String COLUMN_BUNDLE = "bundle";



    public static final String CREATE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_TITLE + " TEXT PRIMARY KEY, " +
                    COLUMN_DESCRIPTION + " TEXT, " +
                    COLUMN_ID + " INTEGER, " +
                    COLUMN_IMGURL + " TEXT, " +
                    COLUMN_URL_ORIGIN + " TEXT, " +
                    COLUMN_CREATED + " DATE, " +
                    COLUMN_URL_WEB + " TEXT,"+

                    COLUMN_TYPE + " TEXT,"+
                    COLUMN_PACKAGE + " TEXT,"+
                    COLUMN_BUNDLE + " TEXT"+

                    "); ";

    public static ContentValues toContentValues(Article article) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, article.id);
        values.put(COLUMN_CREATED, article.created);
        values.put(COLUMN_TITLE, article.title);
        values.put(COLUMN_DESCRIPTION, article.description);
        values.put(COLUMN_IMGURL, article.imgUrl);
        values.put(COLUMN_URL_ORIGIN, article.urlOrigin);
        values.put(COLUMN_URL_WEB, article.urlWeb);

        values.put(COLUMN_TYPE, article.type);
        values.put(COLUMN_PACKAGE, article.packageName);
        values.put(COLUMN_BUNDLE, article.bundle);
        return values;
    }

    public static Article parseCursor(Cursor cursor) {
        Article article = new Article();
        article.id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
        article.title = cursor.getString(cursor.getColumnIndex(COLUMN_TITLE));
        article.created = cursor.getString(cursor.getColumnIndex(COLUMN_CREATED));
        article.description = cursor.getString(cursor.getColumnIndex(COLUMN_DESCRIPTION));
        article.imgUrl = cursor.getString(cursor.getColumnIndex(COLUMN_IMGURL));
        article.urlOrigin = cursor.getString(cursor.getColumnIndex(COLUMN_URL_ORIGIN));
        article.urlWeb = cursor.getString(cursor.getColumnIndex(COLUMN_URL_WEB));

        article.type = cursor.getString(cursor.getColumnIndex(COLUMN_TYPE));
        article.packageName = cursor.getString(cursor.getColumnIndex(COLUMN_PACKAGE));
        article.bundle = cursor.getString(cursor.getColumnIndex(COLUMN_BUNDLE));
        return article;
    }
}
