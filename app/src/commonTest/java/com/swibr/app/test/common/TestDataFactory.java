package com.swibr.app.test.common;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.swibr.app.data.model.Name;
import com.swibr.app.data.model.Profile;
import com.swibr.app.data.model.Swibr;

/**
 * Factory class that makes instances of data models with random field values.
 * The aim of this class is to help setting up test fixtures.
 */
public class TestDataFactory {

    public static String randomUuid() {
        return UUID.randomUUID().toString();
    }

    public static Swibr makeSwibr(String uniqueSuffix) {
        return new Swibr(makeProfile(uniqueSuffix));
    }

    public static List<Swibr> makeListSwibrs(int number) {
        List<Swibr> swibrs = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            swibrs.add(makeSwibr(String.valueOf(i)));
        }
        return swibrs;
    }

    public static Profile makeProfile(String uniqueSuffix) {
        Profile profile = new Profile();
        profile.email = "email" + uniqueSuffix + "@ribot.co.uk";
        profile.name = makeName(uniqueSuffix);
        profile.dateOfBirth = new Date();
        profile.hexColor = "#0066FF";
        profile.avatar = "http://api.ribot.io/images/" + uniqueSuffix;
        profile.bio = randomUuid();
        return profile;
    }

    public static Name makeName(String uniqueSuffix) {
        Name name = new Name();
        name.first = "Name-" + uniqueSuffix;
        name.last = "Surname-" + uniqueSuffix;
        return name;
    }

}