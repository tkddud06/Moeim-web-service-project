package com.moeim.post;

import com.moeim.category.Category;
import com.moeim.user.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;

    // 카테고리 Id로 포스트들 갖고오기
    public Page<Post> getPostsByCategoryId(Long categoryId, int page) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by("createdAt").descending());
        return postRepository.findByCategoryId(categoryId, pageable);
    }

    // 유저 Id로 포스트들 갖고오기
    public Page<Post> getPostsByUserId(Long UserId, int page) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by("createdAt").descending());
        return postRepository.findByUserId(UserId, pageable);
    }

    // 통합 검색용 메소드
    public Page<Post> search(Long categoryId, String searchType, String keyword, int page) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by("createdAt").descending());

        if (keyword == null || keyword.trim().isEmpty()) {
            // 검색어 없으면 빈 페이지(여기선 로직상 호출될 일 없음)
            return Page.empty();
        }

        if (categoryId == 0) { // 전체 게시판
            switch (searchType) {
                case "title": // 제목 검색
                    return postRepository.findByTitleContaining(keyword, pageable);
                case "user": // 작성자 검색
                    return postRepository.findByUser_NicknameContaining(keyword, pageable);
                default: // 나머지 혹은 제목+내용 검색
                    return postRepository.findByTitleContainingOrTextContaining(keyword, keyword, pageable);
            }
        }
        else { // 특정 게시판
            switch (searchType) {
                case "title": // 제목 검색
                    return postRepository.findByCategoryIdAndTitleContaining(categoryId, keyword, pageable);
                case "user": // 작성자 검색
                    return postRepository.findByCategoryIdAndUser_NicknameContaining(categoryId, keyword, pageable);
                default: // 나머지 혹은 제목+내용 검색
                    return postRepository.findByCategoryIdAndTitleContainingOrCategoryIdAndTextContaining(categoryId, keyword, categoryId, keyword, pageable);
            }
        }
    }

    // 모든 포스트 갖고오기 (페이징)
    public Page<Post> getAllPosts(int page) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by("createdAt").descending());
        return postRepository.findAll(pageable);
    }

    // 포스트 Id로 포스트 갖고오기
    public Optional<Post> getPost(Long postId) {
        Optional<Post> op = postRepository.findById(postId);
        return op;
    }

    @Transactional
    public void increaseViewCount(Long postId) {
        postRepository.updateViewCount(postId);
    }

    @Transactional
    public Post create(Category category, User user, String title, String text) {
        Post post = Post.builder().category(category).user(user).title(title).text(text).build();

        return postRepository.save(post);
    }

    @Transactional
    public void delete(Post post) {
        postRepository.delete(post);
    }

    @Transactional
    public void modify(Post post, String title, String text) {
        postRepository.updatePost(post.getId(), title, text);
    }
}
