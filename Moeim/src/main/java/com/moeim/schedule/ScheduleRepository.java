package com.moeim.schedule;

import com.moeim.group.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    // 일정 검색해서 시작날짜로 오름차순 정렬해서 보여줌
    List<Schedule> findByGroupOrderByStartDateAsc(Group group);

    // 특정 날짜 사이의 startdate로 시작하는 일정 검색
    List<Schedule> findByGroupAndStartDateBetween(Group group, LocalDateTime start, LocalDateTime end);

    // 키워드 포함하는 일정 검색
    List<Schedule> findByGroupAndTitleContaining(Group group, String titleKeyword);

    // 특정 그룹의 일정 찾기
    List<Schedule> findAllByGroup(Group group);

    List<Schedule> findAllByGroupIn(List<Group> groups);

    boolean existsByGroup(Group group);

    @Modifying(clearAutomatically = true)
    @Query("update Schedule s set s.title = :title, s.description = :description, s.startDate = :startDate, s.endDate = :endDate where s.id = :id")
    void updateSchedule(@Param("id") Long id, @Param("title") String title, @Param("description") String description, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // 특정 기간(시작~끝) 사이에 시작하는 일정 조회
    List<Schedule> findAllByStartDateBetween(LocalDateTime start, LocalDateTime end);

    void deleteByGroup(Group group);

}