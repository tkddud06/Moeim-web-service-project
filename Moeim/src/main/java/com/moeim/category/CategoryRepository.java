package com.moeim.category;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    // 특정 제목의 카테고리 찾기
    Optional<Category> findByTitle(String title);

}
