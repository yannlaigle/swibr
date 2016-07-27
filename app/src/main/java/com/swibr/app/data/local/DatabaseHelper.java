package com.swibr.app.data.local;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;
import com.swibr.app.data.model.Article.Article;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

@Singleton
public class DatabaseHelper {

    private final BriteDatabase mDb;
    private static final String TAG = "DatabaseHelper";

    @Inject
    public DatabaseHelper(DbOpenHelper dbOpenHelper) {
        mDb = SqlBrite.create().wrapDatabaseHelper(dbOpenHelper);
    }

    public BriteDatabase getBriteDb() {
        return mDb;
    }

    /**
     * Remove all the data from all the tables in the database.
     */
    public Observable<Void> clearTables() {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                if (subscriber.isUnsubscribed()) return;
                BriteDatabase.Transaction transaction = mDb.newTransaction();
                try {
                    Cursor cursor = mDb.query("SELECT name FROM sqlite_master WHERE type='table'");
                    while (cursor.moveToNext()) {
                        mDb.delete(cursor.getString(cursor.getColumnIndex("name")), null);
                    }
                    cursor.close();
                    transaction.markSuccessful();
                    subscriber.onCompleted();
                } finally {
                    transaction.end();
                }
            }
        });
    }

    public Observable<Article> saveSwibr(final Article article) {
        return Observable.create(new Observable.OnSubscribe<Article>() {
            @Override
            public void call(Subscriber<? super Article> subscriber) {
                if (subscriber.isUnsubscribed()) return;
                BriteDatabase.Transaction transaction = mDb.newTransaction();
                try {
                    long result = mDb.insert(SwibrArticleTable.TABLE_NAME,
                            SwibrArticleTable.toContentValues(article),
                            SQLiteDatabase.CONFLICT_REPLACE);
                    if (result >= 0) subscriber.onNext(article);
                    transaction.markSuccessful();
                    subscriber.onCompleted();
                } catch (Exception e) {
                    Log.e(TAG, "saveSwibr Exception: ", e);
                } finally {
                    Log.d(TAG, "SaveSwibr : success");
                    transaction.end();
                }
            }
        });
    }

    public Observable<Article> setSwibrs(final Collection<Article> articles) {
        return Observable.create(new Observable.OnSubscribe<Article>() {
            @Override
            public void call(Subscriber<? super Article> subscriber) {
                if (subscriber.isUnsubscribed()) return;
                BriteDatabase.Transaction transaction = mDb.newTransaction();
                try {
                    mDb.delete(SwibrArticleTable.TABLE_NAME, null);
                    for (Article article : articles) {
                        long result = mDb.insert(SwibrArticleTable.TABLE_NAME,
                                SwibrArticleTable.toContentValues(article),
                                SQLiteDatabase.CONFLICT_REPLACE);
                        if (result >= 0) subscriber.onNext(article);
                    }
                    transaction.markSuccessful();
                    subscriber.onCompleted();
                } finally {
                    transaction.end();
                }
            }
        });
    }

    public Observable<List<Article>> getSwibrs() {
        return mDb.createQuery(SwibrArticleTable.TABLE_NAME,
                "SELECT * FROM " + SwibrArticleTable.TABLE_NAME + " ORDER BY " + SwibrArticleTable.COLUMN_ID + " ASC")
                .mapToList(new Func1<Cursor, Article>() {
                    @Override
                    public Article call(Cursor cursor) {
                        return SwibrArticleTable.parseCursor(cursor);
                    }
                });
    }

}
