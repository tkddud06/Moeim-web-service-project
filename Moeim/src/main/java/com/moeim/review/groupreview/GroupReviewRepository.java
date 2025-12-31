package com.moeim.review.groupreview;


import com.moeim.group.Group;
import com.moeim.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface GroupReviewRepository extends JpaRepository<GroupReview, Long> {

    // 특정 유저가 특정 그룹에 이미 평가했는지 조회
    Optional<GroupReview> findByUserAndTargetGroup(User user, Group targetGroup);
    boolean existsByUserAndTargetGroup(User user, Group targetGroup);

    // 특정 그룹에 속한 평가 모두 조회
    List<GroupReview> findAllByTargetGroup(Group targetGroup);
    Page<GroupReview> findAllByTargetGroup(Group targetGroup, Pageable pageable);
}