// src/main/java/com/moeim/post/PostVote.java
package com.moeim.post;

import com.moeim.global.BaseTimeEntity;
import com.moeim.global.enums.VoteType;
import com.moeim.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "post_votes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_post_vote_post_user",
                        columnNames = {"post_id", "user_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_vote_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;    // (post-postvote 1:n)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;    // (user-postvote 1:n)

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private VoteType type; // UP, DOWN

    @Builder
    public PostVote(Post post, User user, VoteType type) {
        this.post = post;
        this.user = user;
        this.type = type;
    }
}
