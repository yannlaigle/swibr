package com.swiber.app;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import rx.Observable;
import com.swibr.app.data.DataManager;
import com.swibr.app.data.model.Article.Article;
import com.swibr.app.test.common.TestDataFactory;
import com.swibr.app.ui.main.MainMvpView;
import com.swibr.app.ui.main.MainPresenter;
import com.swibr.app.util.RxSchedulersOverrideRule;

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class MainPresenterTest {

    @Mock MainMvpView mMockMainMvpView;
    @Mock DataManager mMockDataManager;
    private MainPresenter mMainPresenter;

    @Rule
    public final RxSchedulersOverrideRule mOverrideSchedulersRule = new RxSchedulersOverrideRule();

    @Before
    public void setUp() {
        mMainPresenter = new MainPresenter(mMockDataManager);
        mMainPresenter.attachView(mMockMainMvpView);
    }

    @After
    public void tearDown() {
        mMainPresenter.detachView();
    }

    @Test
    public void loadSwibrsReturnsSwibrs() {
        List<Article> articles = TestDataFactory.makeListArticles(10);
        doReturn(Observable.just(articles))
                .when(mMockDataManager)
                .getSwibrs();

        mMainPresenter.loadSwibrs();
        verify(mMockMainMvpView).showSwibrs(articles);
        verify(mMockMainMvpView, never()).showSwibrsEmpty();
        verify(mMockMainMvpView, never()).showError();
    }

    @Test
    public void loadSwibrsReturnsEmptyList() {
        doReturn(Observable.just(Collections.emptyList()))
                .when(mMockDataManager)
                .getSwibrs();

        mMainPresenter.loadSwibrs();
        verify(mMockMainMvpView).showSwibrsEmpty();
        verify(mMockMainMvpView, never()).showSwibrs(anyListOf(Article.class));
        verify(mMockMainMvpView, never()).showError();
    }

    @Test
    public void loadSwibrsFails() {
        doReturn(Observable.error(new RuntimeException()))
                .when(mMockDataManager)
                .getSwibrs();

        mMainPresenter.loadSwibrs();
        verify(mMockMainMvpView).showError();
        verify(mMockMainMvpView, never()).showSwibrsEmpty();
        verify(mMockMainMvpView, never()).showSwibrs(anyListOf(Article.class));
    }
}
