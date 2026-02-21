package com.example.visceralmassageapi.articles.exception;

public class ArticleNotFoundException extends RuntimeException {
    public ArticleNotFoundException(Integer id) {
        super("Article not found: " + id);
    }
}