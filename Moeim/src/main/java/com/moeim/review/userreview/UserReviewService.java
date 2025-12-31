package com.moeim.review.userreview;

import com.moeim.group.Group;
import com.moeim.user.User;
import com.moeim.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserReviewService {

    private final UserReviewRepository userReviewRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createReview(User targetUser, User user, Group group, UserReviewForm form) {

        // 중복 평가 체크
        if (userReviewRepository.existsByUserAndTargetUserAndGroup(user, targetUser, group)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 평가에 참여하였습니다.");
        }
        else {
            UserReview userReview = UserReview.builder()
                    .scoreManner(form.getScoreManner())
                    .scoreContribution(form.getScoreContribution())
                    .scorePunctuality(form.getScorePunctuality())
                    .description(form.getDescription())
                    .group(group)
                    .user(user)
                    .targetUser(targetUser)
                    .build();

            userReviewRepository.save(userReview);
        }
    }

    // 이미 평가를 했는지 중복 검사용 메소드
    public boolean isexistsByUserAndTargetUserAndGroup(User user, User targetUser, Group group) {
        return userReviewRepository.existsByUserAndTargetUserAndGroup(user, targetUser, group);
    }

    // 특정 유저의 리뷰 통계 가져오기
    public UserReviewStatsDTO getReviewStats(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        List<UserReview> reviews = userReviewRepository.findAllByTargetUser(user);

        if (reviews.isEmpty()) {
            return new UserReviewStatsDTO(0, 0.0, 0.0, 0.0, 0.0);
        }

        long count = reviews.size();
        double sumManner = reviews.stream().mapToInt(UserReview::getScoreManner).sum();
        double sumContribution = reviews.stream().mapToInt(UserReview::getScoreContribution).sum();
        double sumPunctuality = reviews.stream().mapToInt(UserReview::getScorePunctuality).sum();
        double sumTotal = reviews.stream().mapToDouble(UserReview::getAverageScore).sum();

        return new UserReviewStatsDTO(
                count,
                Math.round((sumTotal / count) * 10) / 10.0, // 소수점 한자리 반올림
                Math.round((sumManner / count) * 10) / 10.0,
                Math.round((sumContribution / count) * 10) / 10.0,
                Math.round((sumPunctuality / count) * 10) / 10.0
        );
    }

    // 특정 유저의 리뷰 목록 가져오기 (페이징)
    public Page<UserReview> getReviewsByUser(Long userId, int page) {
        User user = userRepository.findById(userId).orElseThrow();
        Pageable pageable = PageRequest.of(page, 5, Sort.by("createdAt").descending());
        return userReviewRepository.findAllByTargetUser(user, pageable);
    }
    // 특정 그룹에서 이미 리뷰를 작성한 멤버들의 ID 목록 가져오기
    public List<Long> getReviewedTargetIds(Long groupId, Long userId) {
        return userReviewRepository.findReviewedTargetIds(groupId, userId);
    }
}
