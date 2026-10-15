package com.chuctupk.repository;

import com.chuctupk.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    Optional<Article> findBySlug(String slug);
    Page<Article> findByIsPublishedTrueOrderByPublishedAtDesc(Pageable pageable);
    Page<Article> findByCategoryIdAndIsPublishedTrueOrderByPublishedAtDesc(Long categoryId, Pageable pageable);
    boolean existsBySlug(String slug);
}
