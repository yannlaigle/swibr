package com.swibr.app.test.common;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.swibr.app.data.model.Article;

/**
 * Factory class that makes instances of data models with random field values.
 * The aim of this class is to help setting up test fixtures.
 */
public class TestDataFactory {

    public static String randomUuid() {
        return UUID.randomUUID().toString();
    }

    public static Article createArticle(String uniqueSuffix) {
        return new Article(makeArticle(uniqueSuffix));
    }

    public static List<Article> makeListArticles(int number) {
        List<Article> articles = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            articles.add(createArticle(String.valueOf(i)));
        }
        return articles;
    }

    public static Article makeArticle(String uniqueSuffix) {
        Article article = new Article();
//        article.email = "email" + uniqueSuffix + "@ribot.co.uk";
//        article.name = makeName(uniqueSuffix);
//        article.dateOfBirth = new Date();
//        article.hexColor = "#0066FF";
//        article.avatar = "http://api.ribot.io/images/" + uniqueSuffix;
//        article.bio = randomUuid();

        article.imgUrl = "http://api.ribot.io/images/" + uniqueSuffix;
        article.urlWeb = "http://Urlweb";
        article.urlOrigin = "";
        article.description = "Test description";
        article.title = "Test title";
        article.id = 0;

        return article;
    }
//
//    public static Name makeName(String uniqueSuffix) {
//        Name name = new Name();
//        name.first = "Name-" + uniqueSuffix;
//        name.last = "Surname-" + uniqueSuffix;
//        return name;
//    }

}