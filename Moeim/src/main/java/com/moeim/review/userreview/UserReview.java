package com.moeim.review.userreview;


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
        name = "user_reviews",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_group_target_user",
                        columnNames = {"user_id", "group_id", "target_user_id"}
                )
        }
)
public class UserReview extends BaseTimeEntity { // BaseTimeEntity 상속

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_review_id")
    private Long id;

    @Column(nullable = false)
    private int scoreManner;        // 매너/태도 점수

    @Column(nullable = false)
    private int scoreContribution;  // 기여도 점수

    @Column(nullable = false)
    private int scorePunctuality;   // 시간 엄수/약속 이행 점수

    @Column(nullable = false)
    private double averageScore;    // 평균 점수 (저장 시 계산)

    // 후기 내용
    @Column(length = 300)
    private String description;

    // 그룹 (평가가 이루어지는 단위)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = true)
    private Group group;

    // 작성자 (평가하는 사람)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 평가 대상 (평가받는 사람)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    private User targetUser;

    @Builder
    public UserReview(int scoreManner, int scoreContribution, int scorePunctuality, String description, Group group, User user, User targetUser) {
        this.scoreManner = scoreManner;
        this.scoreContribution = scoreContribution;
        this.scorePunctuality = scorePunctuality;
        this.description = description;
        this.group = group;
        this.user = user;
        this.targetUser = targetUser;

        // 평균 점수 자동 계산
        this.averageScore = (scoreManner + scoreContribution + scorePunctuality) / 3.0;
    }
}