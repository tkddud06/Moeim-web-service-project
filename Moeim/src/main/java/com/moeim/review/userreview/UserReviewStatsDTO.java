package com.moeim.review.userreview;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserReviewStatsDTO {
    private long reviewCount; // 총 리뷰 개수
    private double averageTotal; // 전체 평균
    private double averageManner; // 매너 평균
    private double averageContribution; // 기여도 평균
    private double averagePunctuality; // 시간엄수 평균
}
