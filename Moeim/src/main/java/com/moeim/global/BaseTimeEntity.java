package com.moeim.global;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;

    // 화면 출력용 날짜 포맷터
    public String getFormattedDate() {
        if (createdAt == null) return "";

        LocalDateTime now = LocalDateTime.now();

        // 1. 오늘 작성된 글이면 -> 시간만 표시 (HH:mm)
        if (createdAt.toLocalDate().equals(now.toLocalDate())) {
            return createdAt.format(DateTimeFormatter.ofPattern("HH:mm"));
        }
        // 2. 올해 작성된 글이면 -> 월.일 표시 (MM.dd)
        else if (createdAt.getYear() == now.getYear()) {
            return createdAt.format(DateTimeFormatter.ofPattern("MM.dd"));
        }
        // 3. 그 외(작년 등) -> 년.월.일 표시 (yyyy.MM.dd)
        else {
            return createdAt.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        }
    }

}
