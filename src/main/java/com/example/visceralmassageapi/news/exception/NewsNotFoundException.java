package com.example.visceralmassageapi.news.exception;

public class NewsNotFoundException extends RuntimeException {
    public NewsNotFoundException(Integer id) {
        super("News item not found: " + id);
    }
}
