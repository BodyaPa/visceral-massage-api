package com.example.visceralmassageapi.repository;

import com.example.visceralmassageapi.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, Integer> {
}
