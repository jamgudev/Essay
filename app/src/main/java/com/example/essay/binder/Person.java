package com.example.essay.binder;

import android.os.Parcel;
import android.os.Parcelable;
import com.jamgu.home.Schemes.MainPage;
import com.jamgu.krouter.annotation.KRouter;

public class Person implements Parcelable {

    public int gender;
    public String name;

    protected Person(Parcel in) {
        gender = in.readInt();
        name = in.readString();
    }

    public static final Creator<Person> CREATOR = new Creator<Person>() {
        @Override
        public Person createFromParcel(Parcel in) {
            return new Person(in);
        }

        @Override
        public Person[] newArray(int size) {
            return new Person[size];
        }
    };

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Person(int gender, String name) {
        this.gender = gender;
        this.name = name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(gender);
        dest.writeString(name);
    }
}
