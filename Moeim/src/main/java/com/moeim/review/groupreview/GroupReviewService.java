package com.moeim.review.groupreview;

import com.moeim.group.Group;
import com.moeim.group.GroupRepository;
import com.moeim.user.User;
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
@Transactional(readOnly = true)
public class GroupReviewService {

    private final GroupReviewRepository groupReviewRepository;
    private final GroupRepository groupRepository;


    @Transactional
    public void createReview(Group group, User user, GroupReviewForm form) {

        // 중복 평가 체크
        if (groupReviewRepository.existsByUserAndTargetGroup(user, group)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 평가에 참여하였습니다.");
        }
        else {
            GroupReview groupReview = GroupReview.builder()
                    .scoreReliability(form.getScoreReliability())
                    .scorePreparation(form.getScorePreparation())
                    .scoreSatisfaction(form.getScoreSatisfaction())
                    .description(form.getDescription())
                    .user(user)
                    .targetGroup(group)
                    .build();

            groupReviewRepository.save(groupReview);
        }
    }

    // 이미 평가를 했는지 중복 검사용 메소드
    public boolean isexistsByUserAndTargetGroup(User user, Group group) {
        return groupReviewRepository.existsByUserAndTargetGroup(user, group);
    }

    // 특정 그룹의 리뷰 통계 가져오기
    public GroupReviewStatsDTO getReviewStats(Long groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow();
        List<GroupReview> reviews = groupReviewRepository.findAllByTargetGroup(group);

        if (reviews.isEmpty()) {
            return new GroupReviewStatsDTO(0, 0.0, 0.0, 0.0, 0.0);
        }

        long count = reviews.size();
        double sumReliability = reviews.stream().mapToInt(GroupReview::getScoreReliability).sum();
        double sumPreparation = reviews.stream().mapToInt(GroupReview::getScorePreparation).sum();
        double sumSatisfaction = reviews.stream().mapToInt(GroupReview::getScoreSatisfaction).sum();
        double sumTotal = reviews.stream().mapToDouble(GroupReview::getAverageScore).sum();

        return new GroupReviewStatsDTO(
                count,
                Math.round((sumTotal / count) * 10) / 10.0, // 소수점 한자리 반올림
                Math.round((sumReliability / count) * 10) / 10.0,
                Math.round((sumPreparation / count) * 10) / 10.0,
                Math.round((sumSatisfaction / count) * 10) / 10.0
        );
    }

    // 특정 그룹의 리뷰 목록 가져오기 (페이징)
    public Page<GroupReview> getReviewsByGroup(Long groupId, int page) {
        Group group = groupRepository.findById(groupId).orElseThrow();
        Pageable pageable = PageRequest.of(page, 5, Sort.by("createdAt").descending());
        return groupReviewRepository.findAllByTargetGroup(group, pageable);
    }
}