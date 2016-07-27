package com.swibr.app.ui.main;

import com.swibr.app.data.model.Article.Article;
import com.swibr.app.ui.base.MvpView;

import java.util.List;

public interface MainMvpView extends MvpView {

    void showSwibrs(List<Article> swibrs);

    void showSwibrsEmpty();

    void showError();

}
