package com.moeim.group;

import com.moeim.global.enums.PositionType;
import com.moeim.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupUserRepository extends JpaRepository<GroupUser, Long> {

    // 특정 그룹의 모든 회원
    List<GroupUser> findByGroup(Group group);

    // 특정 유저가 가입한 모든 그룹
    List<GroupUser> findByUser(User user);

    // 특정 유저가 특정 그룹에 가입했는지 상세 조회
    Optional<GroupUser> findByGroupAndUser(Group group, User user);

    // 특정 유저가 특정 그룹에 가입했는지 여부만 체크
    boolean existsByGroupAndUser(Group group, User user);

    // 그룹 인원수
    Long countByGroup(Group group);

    // ================================
    //  groupId + userId 기반 조회
    // ================================
    boolean existsByGroup_IdAndUser_Id(Long groupId, Long userId);

    //  groupId + userId + 역할 기반 조회
    boolean existsByGroup_IdAndUser_IdAndPosition(Long groupId, Long userId, PositionType position);

    Optional<GroupUser> findByGroup_IdAndUser_Id(Long groupId, Long userId);

    Optional<GroupUser> findByGroupAndPosition(Group group, PositionType position);

    void deleteByGroup(Group group);

    Optional<GroupUser> findByGroupAndUserAndPosition(Group group, User user, PositionType position);

    // 한 개 이상의 모임에 참가한 사람의 수
    @Query("SELECT COUNT(DISTINCT gu.user.id) FROM GroupUser gu")
    long countDistinctUsers();

    @Query("SELECT gu FROM GroupUser gu JOIN FETCH gu.group WHERE gu.user.id = :userId")
    Page<GroupUser> findAllByUserId(@Param("userId") Long userId, Pageable pageable);
}
