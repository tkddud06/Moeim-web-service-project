// src/main/java/com/moeim/category/Category.java
package com.moeim.category;

import com.moeim.global.enums.CategoryType;
import com.moeim.group.Group;
import com.moeim.post.Post;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    // BOARD, CATEGORY 일반게시판(소모임, 스케줄 기능 미제공) / 카테고리
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryType type;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Post> posts = new ArrayList<>();

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Group> groups = new ArrayList<>();
    
    @Builder
    public Category(String title, CategoryType type) {
        this.title = title;
        this.type = type;
    }
}


