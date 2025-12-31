package com.moeim.review.groupreview;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupReviewStatsDTO {
    private long reviewCount; // 총 리뷰 개수
    private double averageTotal; // 전체 평균
    private double averageReliability; // 신뢰도 평균
    private double averagePreparation; // 준비성 평균
    private double averageSatisfaction; // 만족도 평균
}