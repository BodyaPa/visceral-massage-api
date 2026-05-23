package com.example.visceralmassageapi.news.repository;

import com.example.visceralmassageapi.news.entity.NewsItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsRepository extends JpaRepository<NewsItem, Integer> {
}
