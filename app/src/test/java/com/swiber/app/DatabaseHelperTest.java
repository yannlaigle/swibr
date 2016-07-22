package com.swibr.app;

import android.database.Cursor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

import rx.observers.TestSubscriber;
import com.swibr.app.data.local.DatabaseHelper;
import com.swibr.app.data.local.Db;
import com.swibr.app.data.local.DbOpenHelper;
import com.swibr.app.data.model.Swibr;
import com.swibr.app.test.common.TestDataFactory;
import com.swibr.app.util.DefaultConfig;

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
        Swibr swibr1 = TestDataFactory.makeSwibr("r1");
        Swibr swibr2 = TestDataFactory.makeSwibr("r2");
        List<Swibr> swibrs = Arrays.asList(swibr1, swibr2);

        TestSubscriber<Swibr> result = new TestSubscriber<>();
        mDatabaseHelper.setSwibrs(swibrs).subscribe(result);
        result.assertNoErrors();
        result.assertReceivedOnNext(swibrs);

        Cursor cursor = mDatabaseHelper.getBriteDb()
                .query("SELECT * FROM " + Db.SwibrArticleTable.TABLE_NAME);
        assertEquals(2, cursor.getCount());
        for (Swibr swibr : swibrs) {
            cursor.moveToNext();
            assertEquals(swibr.profile, Db.SwibrArticleTable.parseCursor(cursor));
        }
    }

    @Test
    public void getSwibrs() {
        Swibr swibr1 = TestDataFactory.makeSwibr("r1");
        Swibr swibr2 = TestDataFactory.makeSwibr("r2");
        List<Swibr> swibrs = Arrays.asList(swibr1, swibr2);

        mDatabaseHelper.setSwibrs(swibrs).subscribe();

        TestSubscriber<List<Swibr>> result = new TestSubscriber<>();
        mDatabaseHelper.getSwibrs().subscribe(result);
        result.assertNoErrors();
        result.assertValue(swibrs);
    }

}