package com.swiber.app;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import rx.Observable;
import rx.observers.TestSubscriber;

import com.swibr.app.data.DataManager;
import com.swibr.app.data.local.DatabaseHelper;
import com.swibr.app.data.local.PreferencesHelper;
import com.swibr.app.data.model.Article;
import com.swibr.app.data.remote.OcrService;
import com.swibr.app.data.remote.SwibrsService;
import com.swibr.app.test.common.TestDataFactory;
import com.swibr.app.util.EventPosterHelper;

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This test class performs local unit tests without dependencies on the Android framework
 * For testing methods in the DataManager follow this approach:
 * 1. Stub mock helper classes that your method relies on. e.g. RetrofitServices or DatabaseHelper
 * 2. Test the Observable using TestSubscriber
 * 3. Optionally write a SEPARATE test that verifies that your method is calling the right helper
 * using Mockito.verify()
 */
@RunWith(MockitoJUnitRunner.class)
public class DataManagerTest {

    @Mock
    DatabaseHelper mMockDatabaseHelper;
    @Mock
    PreferencesHelper mMockPreferencesHelper;
    @Mock
    SwibrsService mMockSwibrsService;
    @Mock
    EventPosterHelper mEventPosterHelper;
    private DataManager mDataManager;
    private OcrService mOcrService;

    @Before
    public void setUp() {
        mDataManager = new DataManager(mMockSwibrsService, mOcrService,
                mMockPreferencesHelper, mMockDatabaseHelper, mEventPosterHelper);
    }

    @Test
    public void syncSwibrsEmitsValues() {
        List<Article> articles = Arrays.asList(TestDataFactory.createArticle("r1"),
                TestDataFactory.createArticle("r2"));
        stubSyncSwibrsHelperCalls(articles);

        TestSubscriber<Article> result = new TestSubscriber<>();
        mDataManager.syncSwibrs().subscribe(result);
        result.assertNoErrors();
        result.assertReceivedOnNext(articles);
    }

    @Test
    public void syncSwibrsCallsApiAndDatabase() {
        List<Article> articles = Arrays.asList(TestDataFactory.createArticle("r1"),
                TestDataFactory.createArticle("r2"));
        stubSyncSwibrsHelperCalls(articles);

        mDataManager.syncSwibrs().subscribe();
        // Verify right calls to helper methods
        verify(mMockSwibrsService).getSwibrs();
        verify(mMockDatabaseHelper).setSwibrs(articles);
    }

    @Test
    public void syncSwibrsDoesNotCallDatabaseWhenApiFails() {
        when(mMockSwibrsService.getSwibrs())
                .thenReturn(Observable.<List<Article>>error(new RuntimeException()));

        mDataManager.syncSwibrs().subscribe(new TestSubscriber<Article>());
        // Verify right calls to helper methods
        verify(mMockSwibrsService).getSwibrs();
        verify(mMockDatabaseHelper, never()).setSwibrs(anyListOf(Article.class));
    }

    private void stubSyncSwibrsHelperCalls(List<Article> articles) {
        // Stub calls to the ribot service and database helper.
        when(mMockSwibrsService.getSwibrs())
                .thenReturn(Observable.just(articles));
        when(mMockDatabaseHelper.setSwibrs(articles))
                .thenReturn(Observable.from(articles));
    }

}
