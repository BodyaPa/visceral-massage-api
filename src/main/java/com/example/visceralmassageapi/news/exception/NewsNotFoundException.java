package com.example.visceralmassageapi.news.exception;

import com.example.visceralmassageapi.common.exception.NotFoundException;

public class NewsNotFoundException extends NotFoundException {
    public NewsNotFoundException(Integer id) {
        super("News item not found: " + id);
    }
}
