package com.moeim.review.groupreview;


import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupReviewForm {

    @NotNull(message = "신뢰도 점수를 입력하셔야 합니다.")
    @Min(value = 1, message = "신뢰도 점수는 1점 이상이어야 합니다.")
    @Max(value = 5, message = "신뢰도 점수는 5점 이하이어야 합니다.")
    private Integer scoreReliability;   // 그룹: 신뢰도/참여도

    @NotNull(message = "준비성 점수를 입력하셔야 합니다.")
    @Min(value = 1, message = "준비성 점수는 1점 이상이어야 합니다.")
    @Max(value = 5, message = "준비성 점수는 5점 이하이어야 합니다.")
    private Integer scorePreparation;   // 그룹: 준비/운영 점수

    @NotNull(message = "만족도 점수를 입력하셔야 합니다.")
    @Min(value = 1, message = "만족도 점수는 1점 이상이어야 합니다.")
    @Max(value = 5, message = "만족도 점수는 5점 이하이어야 합니다.")
    private Integer scoreSatisfaction;  // 그룹: 만족도/재미 점수

    @Size(max = 300, message = "후기는 300자를 넘을 수 없습니다.")
    private String description;
}