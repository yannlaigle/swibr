package com.swibr.app.data.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Article implements Comparable<Article>, Parcelable {

    public int id = 0;
    public int userid = 0;
    public String title = "";
    public String description = "";
    public String urlWeb = "";
    public String imgUrl = "";
    public String urlOrigin = "";
    public String hexColor = "#0066FF";

    public Article() {
    }

    public Article(Parcel p) {
        id = p.readInt();
        userid = p.readInt();
        title = p.readString();
        description = p.readString();
        urlWeb = p.readString();
        imgUrl = p.readString();
        urlOrigin = p.readString();
    }

    public static final Parcelable.Creator<Article> CREATOR = new Parcelable.Creator<Article>() {
        @Override
        public Article createFromParcel(Parcel in) {
            return new Article(in);
        }

        @Override
        public Article[] newArray(int size) {
            return new Article[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel p, int flags) {
        p.writeInt(id);
        p.writeInt(userid);
        p.writeString(title);
        p.writeString(description);
        p.writeString(urlWeb);
        p.writeString(imgUrl);
        p.writeString(urlOrigin);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o.getClass() != getClass()) return false;
        Article a = (Article) o;

        return (id == a.id) &&
                (userid == a.userid) &&
                title.equals(a.title) &&
                description.equals(a.description) &&
                urlWeb.equals(a.urlWeb) &&
                urlOrigin.equals(a.urlOrigin) &&
                imgUrl.equals(a.imgUrl);

    }

    @Override
    public int compareTo(Article another) {
        return 0;
    }
}
