package com.swibr.app.ui.main;

import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;
import com.swibr.app.data.DataManager;
import com.swibr.app.data.model.Swibr;
import com.swibr.app.ui.base.BasePresenter;

public class MainPresenter extends BasePresenter<MainMvpView> {

    private final DataManager mDataManager;
    private Subscription mSubscription;

    @Inject
    public MainPresenter(DataManager dataManager) {
        mDataManager = dataManager;
    }

    @Override
    public void attachView(MainMvpView mvpView) {
        super.attachView(mvpView);
    }

    @Override
    public void detachView() {
        super.detachView();
        if (mSubscription != null) {
            mSubscription.unsubscribe();
        }
    }

    public void loadSwibrs() {
        checkViewAttached();
        mSubscription = mDataManager.getSwibrs()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<List<Swibr>>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e, "There was an error loading the Swibrs.");
                        getMvpView().showError();
                    }

                    @Override
                    public void onNext(List<Swibr> swibrs) {
                        if (swibrs.isEmpty()) {
                            getMvpView().showSwibrsEmpty();
                        } else {
                            getMvpView().showSwibrs(swibrs);
                        }
                    }
                });
    }

}
