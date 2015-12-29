package com.swibr.app.data.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Swibr implements Comparable<Swibr>, Parcelable {

    public Profile profile;

    public Swibr() {
    }

    public Swibr(Profile profile) {
        this.profile = profile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Swibr swibr = (Swibr) o;

        return !(profile != null ? !profile.equals(swibr.profile) : swibr.profile != null);
    }

    @Override
    public int hashCode() {
        return profile != null ? profile.hashCode() : 0;
    }

    @Override
    public int compareTo(Swibr another) {
        return profile.name.first.compareToIgnoreCase(another.profile.name.first);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.profile, 0);
    }

    protected Swibr(Parcel in) {
        this.profile = in.readParcelable(Profile.class.getClassLoader());
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

