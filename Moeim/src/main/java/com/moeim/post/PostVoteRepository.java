package com.moeim.post;

import com.moeim.global.enums.VoteType;
import com.moeim.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostVoteRepository extends JpaRepository<PostVote, Long> {

    // 특정 사용자가 특정 게시글에 투표했는지 여부를 Optional로 조회
    Optional<PostVote> findByPostAndUser(Post post, User user);

    // 특정 사용자가 특정 게시글에 투표했는지 여부를 boolean으로 조회 (Id로)
    boolean existsByPostIdAndUserId(Long postId, Long userId);

    // 특정 포스트에 대한 투표를 개수를 셈
    Long countByPost(Post post);

    // 특정 포스트에 대한 특정 타입의 투표 개수를 셈
    Long countByPostAndType(Post post, VoteType type);
    Long countByPostIdAndType(Long postId, VoteType type);

    // 특정 게시글에 대한 모든 투표를 리스트로 조회
    List<PostVote> findAllByPost(Post post);

    // 특정 사용자가 투표한 모든 기록을 조회 (마이페이지 등)
    List<PostVote> findAllByUser(User user);
}