package com.swibr.app.data.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

public class Swibr implements Comparable<Swibr>, Parcelable {

    public Article article;

    public Swibr() {

    }

    public Swibr(Article article) {
        this.article = article;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Swibr swibr = (Swibr) o;

        return article.equals(swibr.article);
    }

    @Override
    public int hashCode() {
        return article != null ? article.hashCode() : 0;
    }

    @Override
    public int compareTo(@NonNull Swibr another) {
        return article.title.compareToIgnoreCase(another.article.title);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable((Parcelable) this.article, 0);
    }

    protected Swibr(Parcel in) {
        this.article = in.readParcelable(Article.class.getClassLoader());
    }

    public static final Parcelable.Creator<Swibr> CREATOR = new Parcelable.Creator<Swibr>() {
        public Swibr createFromParcel(Parcel source) {
            return new Swibr(source);
        }

        public Swibr[] newArray(int size) {
            return new Swibr[size];
        }
    };
}

