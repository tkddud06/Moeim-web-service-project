package com.moeim.group;

import com.moeim.global.BaseTimeEntity;
import com.moeim.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "group_join_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GroupJoinRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "join_request_id")
    private Long id;

    // 어떤 그룹에 대한 신청인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    // 누가 신청했는지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 신청할 때 적은 글
    @Column(columnDefinition = "TEXT", nullable = false, length = 300)
    private String text;
}
