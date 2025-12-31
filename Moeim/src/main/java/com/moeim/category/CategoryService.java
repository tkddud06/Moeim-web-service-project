package com.moeim.category;

import com.moeim.group.Group;
import com.moeim.post.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryService {
    // 카테고리 Id로 카테고리 찾기
    private final CategoryRepository categoryRepository;
    public Optional<Category> getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId);
    }

    // 여러 카테고리 Id로 카테고리 찾기
    public List<Category> getCategoriesByIds(List<Integer> categoryIds) {
        // 널이나 빈 리스트면
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Integer -> Long 변환
        List<Long> longIds = categoryIds.stream()
                .map(Long::valueOf)
                .toList();

        // DB에서 조회 후 리턴
        return categoryRepository.findAllById(longIds);
    }

    // 모든 카테고리 찾기
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }
}
