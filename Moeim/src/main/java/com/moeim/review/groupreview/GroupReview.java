package com.moeim.review.groupreview;


import com.moeim.global.BaseTimeEntity;
import com.moeim.group.Group;
import com.moeim.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "group_reviews",
        // 복합 유니크 키: 한 유저가 한 그룹에 한 번만 평가 가능
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_group_user_target",
                        columnNames = {"user_id", "group_id"}
                )
        }
)
public class GroupReview extends BaseTimeEntity { // BaseTimeEntity 상속

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_review_id")
    private Long id;

    @Column(nullable = false)
    private int scoreReliability;   // 신뢰도/참여도 점수

    @Column(nullable = false)
    private int scorePreparation;   // 준비/운영 점수

    @Column(nullable = false)
    private int scoreSatisfaction;  // 만족도/재미 점수

    @Column(nullable = false)
    private double averageScore;    // 평균 점수 (저장 시 계산)

    // 후기 내용
    @Column(length = 300)
    private String description;

    // 작성자 (평가하는 사람)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 평가 대상 (소모임)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group targetGroup;

    @Builder
    public GroupReview(int scoreReliability, int scorePreparation, int scoreSatisfaction, String description, User user, Group targetGroup) {
        this.scoreReliability = scoreReliability;
        this.scorePreparation = scorePreparation;
        this.scoreSatisfaction = scoreSatisfaction;
        this.description = description;
        this.user = user;
        this.targetGroup = targetGroup;

        // 평균 점수 자동 계산
        this.averageScore = (scoreReliability + scorePreparation + scoreSatisfaction) / 3.0;
    }
}