package com.moeim.post;

import com.moeim.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 특정 게시글에 달린 모든 댓글 조회 (작성일 오름차순 - 오래된 순)
    List<Comment> findByPostOrderByCreatedAtAsc(Post post);
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    // 특정 사용자가 작성한 모든 댓글 조회 (페이징 - 마이페이지용)
    Page<Comment> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    Page<Comment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 특정 게시글의 총 댓글 수 조회
    Long countByPost(Post post);
    Long countByPostId(Long postId);
}