// src/main/java/com/moeim/post/Post.java
package com.moeim.post;

import com.moeim.category.Category;
import com.moeim.global.BaseTimeEntity;
import com.moeim.global.enums.VoteType;
import com.moeim.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;        // (category-post 1:n)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;              // (user-post 1:n)

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    @Column(nullable = false)
    @org.hibernate.annotations.ColumnDefault("0")
    private long viewCount = 0L;

    // Post-Comment 1:n, Post-PostVote 1:n (cascade로 hard delete)
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostVote> votes = new ArrayList<>();

    @Builder
    public Post(Category category, User user, String title, String text) {
        this.category = category;
        this.user = user;
        this.title = title;
        this.text = text;
    }
}
