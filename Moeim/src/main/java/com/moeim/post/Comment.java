// src/main/java/com/moeim/post/Comment.java
package com.moeim.post;

import com.moeim.global.BaseTimeEntity;
import com.moeim.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;        // (post-comment 1:n)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;      // (user-comment 1:n)

    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    @Builder
    public Comment(Post post, User user, String text) {
        this.post = post;
        this.user = user;
        this.text = text;
    }
}
