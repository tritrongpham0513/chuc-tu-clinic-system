package com.chuctupk.repository;

import com.chuctupk.entity.ArticleCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleCategoryRepository extends JpaRepository<ArticleCategory, Long> {
    Optional<ArticleCategory> findBySlug(String slug);
    List<ArticleCategory> findAllByOrderBySortOrderAsc();
    boolean existsBySlug(String slug);
}
