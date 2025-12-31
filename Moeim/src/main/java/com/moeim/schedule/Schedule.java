// src/main/java/com/moeim/schedule/Schedule.java
package com.moeim.schedule;

import com.moeim.group.Group;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "schedules")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;  // (group-schedule 1:n)

    @Column(nullable = false, length = 100)
    private String title;

    @Lob
    @Column(nullable = false)
    @org.hibernate.annotations.ColumnDefault("''")
    private String description;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Builder
    public Schedule(Group group, String title, String description, LocalDateTime startDate, LocalDateTime endDate) {
        this.group = group;
        this.title = title;
        this.description = (description != null) ? description : "";
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
