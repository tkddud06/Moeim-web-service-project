package com.moeim.post;

import com.moeim.category.Category;
import com.moeim.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 카테고리별 게시글 목록 조회 (페이징)
    Page<Post> findByCategory(Category category, Pageable pageable);
    Page<Post> findByCategoryId(Long categoryId, Pageable pageable);

    // 정확하게 특정 유저가 작성한 글 목록 조회 (페이징) (검색 + 마이페이지에 활용 가능)
    Page<Post> findByUser(User user, Pageable pageable);
    Page<Post> findByUserId(Long userId, Pageable pageable);

    // User 안에 있는 nickname 필드를 바탕으로 키워드를 통해 포스트 검색 (페이징)
    Page<Post> findByUser_NicknameContaining(String nickname, Pageable pageable);
    Page<Post> findByCategoryIdAndUser_NicknameContaining(Long categoryId, String nickname, Pageable pageable);

    // 통합 키워드 검색 (제목 또는 내용) (페이징)
    Page<Post> findByTitleContainingOrTextContaining(String titleKeyword, String textKeyword, Pageable pageable);
    Page<Post> findByCategoryIdAndTitleContainingOrCategoryIdAndTextContaining(Long categoryId1, String titleKeyword, Long categoryId2, String textKeyword, Pageable pageable);


    // 제목 키워드 검색 (페이징)
    Page<Post> findByTitleContaining(String titleKeyword, Pageable pageable);
    Page<Post> findByCategoryIdAndTitleContaining(Long categoryId, String titleKeyword, Pageable pageable);

    // 조회수 상승 메소드
    @Modifying
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :id")
    int updateViewCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("update Post p set p.title = :title, p.text = :text, p.updatedAt = CURRENT_TIMESTAMP where p.id = :id")
    void updatePost(@Param("id") Long id, @Param("title") String title, @Param("text") String text);

}