//GroupRepository.java

package com.moeim.group;

import java.util.List;

import com.moeim.category.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupRepository extends JpaRepository<Group, Long> {

    Page<Group> findByCategory(Category category, Pageable pageable);
    Page<Group> findByCategory_Id(Long categoryId, Pageable pageable);

    boolean existsByCategoryAndTitle(Category category, String title);

    @Query("""
    SELECT g FROM Group g
    WHERE (:categoryId IS NULL OR g.category.id = :categoryId)
    AND (:keyword IS NULL OR 
        LOWER(CAST(g.title AS string)) LIKE LOWER(CAST(CONCAT('%', :keyword, '%') AS string))
    )
    AND (:userId IS NULL OR g.id NOT IN (
        SELECT gu.group.id FROM GroupUser gu WHERE gu.user.id = :userId
    ))
    """)
    Page<Group> searchGroups(
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            @Param("userId") Long userId,
            Pageable pageable
    );

    //  메인 페이지용: 모집중인 모임 조회 (nowCount < maxCount) (비로그인)
    @Query("SELECT g FROM Group g " +
            "WHERE g.nowCount < g.maxCount " +
            "ORDER BY g.createdAt DESC")
    List<Group> findRecruitingGroups(Pageable pageable);

    // (로그인 유저용) 일반 모집글
    // 관심사가 없거나 추천 모임이 없을 때 보여줄 일반 목록에서도, 내가 가입한 건 빼야 함
    @Query("SELECT g FROM Group g " +
            "WHERE g.nowCount < g.maxCount " +
            "AND g.id NOT IN (SELECT gu.group.id FROM GroupUser gu WHERE gu.user.id = :userId) " +
            "ORDER BY g.createdAt DESC")
    List<Group> findRecruitingGroupsExcludeUser(@Param("userId") Long userId, Pageable pageable);

    // (로그인 유저용) 내 관심사 카테고리에 해당하는 모집 중인 모임 찾기
    @Query("SELECT g FROM Group g " +
            "WHERE g.category.id IN :categoryIds " +
            "AND g.nowCount < g.maxCount " +
            "AND g.id NOT IN (SELECT gu.group.id FROM GroupUser gu WHERE gu.user.id = :userId) " +
            "ORDER BY g.createdAt DESC")
    List<Group> findRecommendedGroups(@Param("categoryIds") List<Long> categoryIds, @Param("userId") Long userId, Pageable pageable);

    //  메인 페이지용: 모집 중인 모임 개수
    @Query("SELECT COUNT(g) FROM Group g WHERE g.nowCount < g.maxCount")
    long countRecruitingGroups();

    @Query("""
    SELECT g FROM Group g
    WHERE (:categoryId IS NULL OR g.category.id = :categoryId)
    AND (:keyword IS NULL OR LOWER(g.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
    AND (g.maxCount - g.nowCount) > 0
    AND (:userId IS NULL OR g.id NOT IN (
        SELECT gu.group.id FROM GroupUser gu WHERE gu.user.id = :userId
    ))
    ORDER BY (g.maxCount - g.nowCount) ASC, g.id DESC
    """)
    Page<Group> searchGroupsDeadline(
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            @Param("userId") Long userId,
            Pageable pageable
    );


}
