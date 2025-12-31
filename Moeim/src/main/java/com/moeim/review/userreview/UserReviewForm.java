package com.moeim.review.userreview;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserReviewForm {
    @NotNull(message = "매너 점수를 입력하셔야 합니다.")
    @Min(value = 1, message = "매너 점수는 1점 이상이어야 합니다.")
    @Max(value = 5, message = "매너 점수는 5점 이하이어야 합니다.")
    private Integer scoreManner;   // 유저: 매너/태도 점수

    @NotNull(message = "기여도 점수를 입력하셔야 합니다.")
    @Min(value = 1, message = "기여도 점수는 1점 이상이어야 합니다.")
    @Max(value = 5, message = "기여도 점수는 5점 이하이어야 합니다.")
    private Integer scoreContribution;   // 유저: 기여도 점수

    @NotNull(message = "시간엄수 점수를 입력하셔야 합니다.")
    @Min(value = 1, message = "시간엄수 점수는 1점 이상이어야 합니다.")
    @Max(value = 5, message = "시간엄수 점수는 5점 이하이어야 합니다.")
    private Integer scorePunctuality;  // 유저: 시간 엄수/약속 이행 점수

    @Size(max = 300, message = "후기는 300자를 넘을 수 없습니다.")
    private String description;
}
