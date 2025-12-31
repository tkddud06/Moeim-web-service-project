package com.moeim.post;

import com.moeim.global.enums.VoteType;
import com.moeim.user.User;
import com.moeim.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostVoteService {
    private final PostVoteRepository postVoteRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    
    // 특정 포스트에 특정 타입의 vote 수 세기
    public long getVoteCount(Long postId, VoteType type) {
        return postVoteRepository.countByPostIdAndType(postId, type);
    }
    // html용
    public long getVoteCount(Long postId, String typeStr) {
        return getVoteCount(postId, VoteType.valueOf(typeStr));
    }

    // 특정 포스트에 특정 유저가 투표했는지 확인
    public boolean isVoted(Long userId, Long postId) {
        return postVoteRepository.existsByPostIdAndUserId(postId, userId);
    }


    @Transactional
    public void create(Post post, User user, VoteType type) {
        if(postVoteRepository.existsByPostIdAndUserId(post.getId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 투표에 참여하였습니다.");
        }
        else {
            PostVote postvote = PostVote.builder().post(post).user(user).type(type).build();
            postVoteRepository.save(postvote);
        }

    }
}
