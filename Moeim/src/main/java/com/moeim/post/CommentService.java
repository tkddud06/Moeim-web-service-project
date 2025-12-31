package com.moeim.post;

import com.moeim.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;

    public Optional<Comment> getComment(Long id) {
        return commentRepository.findById(id);
    }

    public List<Comment> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
    }

    public Page<Comment> getCommentsByUserId(Long userId, int page) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by("createdAt").descending());
        return commentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public long countCommentsByPostId(Long postId) {
        return commentRepository.countByPostId(postId);
    }


    public void create(Post post, User user, String text) {
        Comment comment = Comment.builder().post(post).user(user).text(text).build();
        commentRepository.save(comment);
    }

    public void delete(Comment comment) {
        commentRepository.delete(comment);
    }
}
