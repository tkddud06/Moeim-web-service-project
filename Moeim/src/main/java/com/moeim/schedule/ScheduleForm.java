package com.moeim.schedule.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
public class ScheduleForm {
    private String title; // TODO empty 관련 제한, 글자수 제한
    private String description; // TODO 글자수 제한

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") // HTML datetime-local 형식
    private LocalDateTime startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endDate; // TODO 종료일자가 startDate보다 뒤로 가게 제한. 현재 자동으로 바뀌게 조정되서 문제는 없음.
}