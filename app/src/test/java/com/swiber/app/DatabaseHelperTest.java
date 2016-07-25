package com.swiber.app;

import android.database.Cursor;


import com.swibr.app.BuildConfig;
import com.swibr.app.data.local.DatabaseHelper;
import com.swibr.app.data.local.DbOpenHelper;
import com.swibr.app.data.local.SwibrArticleTable;
import com.swibr.app.data.model.Article;
import com.swibr.app.test.common.TestDataFactory;
import com.swibr.app.util.DefaultConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

import rx.observers.TestSubscriber;

import static junit.framework.Assert.assertEquals;

/**
 * Unit tests integration with a SQLite Database using Robolectric
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = DefaultConfig.EMULATE_SDK)
public class DatabaseHelperTest {

    private final DatabaseHelper mDatabaseHelper =
            new DatabaseHelper(new DbOpenHelper(RuntimeEnvironment.application));

    @Before
    public void setUp() {
        mDatabaseHelper.clearTables().subscribe();
    }

    @Test
    public void setSwibrs() {
        Article article1 = TestDataFactory.createArticle("r1");
        Article article2 = TestDataFactory.createArticle("r2");
        List<Article> articles = Arrays.asList(article1, article2);

        TestSubscriber<Article> result = new TestSubscriber<>();
        mDatabaseHelper.setSwibrs(articles).subscribe(result);
        result.assertNoErrors();
        result.assertReceivedOnNext(articles);

        Cursor cursor = mDatabaseHelper.getBriteDb()
                .query("SELECT * FROM " + SwibrArticleTable.TABLE_NAME);
        assertEquals(2, cursor.getCount());
        for (Article art : articles) {
            cursor.moveToNext();
            assertEquals(art, SwibrArticleTable.parseCursor(cursor));
        }
    }

    @Test
    public void getSwibrs() {
        Article article1 = TestDataFactory.createArticle("r1");
        Article article2 = TestDataFactory.createArticle("r2");
        List<Article> articles = Arrays.asList(article1, article2);

        mDatabaseHelper.setSwibrs(articles).subscribe();

        TestSubscriber<List<Article>> result = new TestSubscriber<>();
        mDatabaseHelper.getSwibrs().subscribe(result);
        result.assertNoErrors();
        result.assertValue(articles);
    }

}