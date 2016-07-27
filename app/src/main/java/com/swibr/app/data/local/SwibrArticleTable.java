package com.swibr.app.data.local;

import android.content.ContentValues;
import android.database.Cursor;

import com.swibr.app.data.model.Article.Article;

/**
 * Created by Shide on 23/07/2016.
 */
public abstract class SwibrArticleTable {
    /*
    {
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

    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_IMGURL = "imgurl";
    private static final String COLUMN_URL_ORIGIN = "urlorigin";
    private static final String COLUMN_URL_WEB = "urlweb";

    public static final String CREATE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_TITLE + " TEXT PRIMARY KEY, " +
                    COLUMN_DESCRIPTION + " TEXT, " +
                    COLUMN_ID + " TEXT, " +
                    COLUMN_IMGURL + " TEXT, " +
                    COLUMN_URL_ORIGIN + " TEXT, " +
                    COLUMN_URL_WEB + " TEXT); ";

    public static ContentValues toContentValues(Article article) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, article.title);
        values.put(COLUMN_DESCRIPTION, article.description);
        values.put(COLUMN_ID, article.id);
        values.put(COLUMN_IMGURL, article.imgUrl);
        values.put(COLUMN_URL_ORIGIN, article.urlOrigin);
        values.put(COLUMN_URL_WEB, article.urlWeb);
        return values;
    }

    public static Article parseCursor(Cursor cursor) {
        Article article = new Article();
        article.id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID));
        article.title = cursor.getString(cursor.getColumnIndex(COLUMN_TITLE));
        article.description = cursor.getString(cursor.getColumnIndex(COLUMN_DESCRIPTION));
        article.imgUrl = cursor.getString(cursor.getColumnIndex(COLUMN_IMGURL));
        article.urlOrigin = cursor.getString(cursor.getColumnIndex(COLUMN_URL_ORIGIN));
        article.urlWeb = cursor.getString(cursor.getColumnIndex(COLUMN_URL_WEB));
        return article;
    }
}
