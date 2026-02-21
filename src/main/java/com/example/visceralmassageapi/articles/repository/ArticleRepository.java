package com.example.visceralmassageapi.articles.repository;

import com.example.visceralmassageapi.articles.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, Integer> {
}
