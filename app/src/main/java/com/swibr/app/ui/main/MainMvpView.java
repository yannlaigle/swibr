package com.swibr.app.ui.main;

import java.util.List;

import com.swibr.app.data.model.Swibr;
import com.swibr.app.ui.base.MvpView;

public interface MainMvpView extends MvpView {

    void showSwibrs(List<Swibr> swibrs);

    void showSwibrsEmpty();

    void showError();

}
