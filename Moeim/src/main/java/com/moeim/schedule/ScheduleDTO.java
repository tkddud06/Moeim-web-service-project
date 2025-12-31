package com.moeim.schedule.dto;

import com.moeim.schedule.Schedule;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ScheduleDTO {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime start;
    private LocalDateTime end;

    private String groupTitle;    // 마이페이지에서 어느 그룹 일정인지 구분용
    private Long groupId;

    private boolean editable;

    public ScheduleDTO(Schedule schedule, boolean canEdit) {
        this.id = schedule.getId();
        this.title = schedule.getTitle();
        this.description = schedule.getDescription();
        this.start = schedule.getStartDate();
        this.end = schedule.getEndDate();
        if(schedule.getGroup() != null) {
            this.groupTitle = schedule.getGroup().getTitle();
            this.groupId = schedule.getGroup().getId();
        }
        this.editable = false; // 풀캘린더 내부 기능으로 수정 여부(드래그 등)
    }
}