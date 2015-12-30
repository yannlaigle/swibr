package com.swibr.app.data.local;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import com.swibr.app.data.model.Swibr;

@Singleton
public class DatabaseHelper {

    private final BriteDatabase mDb;

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

    public Observable<Swibr> saveSwibr(final Swibr newSwibr) {
        return Observable.create(new Observable.OnSubscribe<Swibr>() {
            @Override
            public void call(Subscriber<? super Swibr> subscriber) {
                if (subscriber.isUnsubscribed()) return;
                BriteDatabase.Transaction transaction = mDb.newTransaction();
                try {
                    long result = mDb.insert(Db.SwibrProfileTable.TABLE_NAME,
                            Db.SwibrProfileTable.toContentValues(newSwibr.profile),
                            SQLiteDatabase.CONFLICT_REPLACE);
                    if (result >= 0) subscriber.onNext(newSwibr);
                    transaction.markSuccessful();
                    subscriber.onCompleted();
                } finally {
                    transaction.end();
                }
            }
        });
    }

    public Observable<Swibr> setSwibrs(final Collection<Swibr> newSwibrs) {
        return Observable.create(new Observable.OnSubscribe<Swibr>() {
            @Override
            public void call(Subscriber<? super Swibr> subscriber) {
                if (subscriber.isUnsubscribed()) return;
                BriteDatabase.Transaction transaction = mDb.newTransaction();
                try {
                    mDb.delete(Db.SwibrProfileTable.TABLE_NAME, null);
                    for (Swibr swibr : newSwibrs) {
                        long result = mDb.insert(Db.SwibrProfileTable.TABLE_NAME,
                                Db.SwibrProfileTable.toContentValues(swibr.profile),
                                SQLiteDatabase.CONFLICT_REPLACE);
                        if (result >= 0) subscriber.onNext(swibr);
                    }
                    transaction.markSuccessful();
                    subscriber.onCompleted();
                } finally {
                    transaction.end();
                }
            }
        });
    }

    public Observable<List<Swibr>> getSwibrs() {
        return mDb.createQuery(Db.SwibrProfileTable.TABLE_NAME,
                "SELECT * FROM " + Db.SwibrProfileTable.TABLE_NAME)
                .mapToList(new Func1<Cursor, Swibr>() {
                    @Override
                    public Swibr call(Cursor cursor) {
                    return new Swibr(Db.SwibrProfileTable.parseCursor(cursor));
                    }
                });
    }

}
