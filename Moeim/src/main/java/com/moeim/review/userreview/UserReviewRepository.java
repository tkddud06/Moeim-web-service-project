package com.moeim.review.userreview;

import com.moeim.group.Group;
import com.moeim.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserReviewRepository extends JpaRepository<UserReview, Long> {

    // 특정 유저가 특정 그룹에서 특정 타겟 유저에 이미 평가했는지 조회
    Optional<UserReview> findByUserAndTargetUserAndGroup(User user, User targetUser, Group group);
    boolean existsByUserAndTargetUserAndGroup(User user, User targetUser, Group group);

    // 특정 유저에 속한 평가 모두 조회
    List<UserReview> findAllByTargetUser(User targetUser);
    Page<UserReview> findAllByTargetUser(User targetUser, Pageable pageable);

    // 특정 그룹 내에서 내가 리뷰를 남긴 대상(TargetUser)의 ID 목록 조회
    @Query("SELECT ur.targetUser.id FROM UserReview ur WHERE ur.group.id = :groupId AND ur.user.id = :userId")
    List<Long> findReviewedTargetIds(@Param("groupId") Long groupId,
                                     @Param("userId") Long userId);

    // 특정 그룹 ID를 가진 리뷰들의 그룹 정보를 NULL로 변경 (연관관계 끊기)
    @Modifying(clearAutomatically = true) // 쿼리 실행 후 영속성 컨텍스트 초기화
    @Query("UPDATE UserReview ur SET ur.group = NULL WHERE ur.group.id = :groupId")
    void setGroupNullByGroupId(@Param("groupId") Long groupId);
}
