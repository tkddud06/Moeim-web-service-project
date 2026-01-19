package com.moeim.user;

import com.moeim.chat.ChatParticipant;
import com.moeim.global.BaseTimeEntity;
import com.moeim.group.GroupUser;
import com.moeim.post.Comment;
import com.moeim.post.Post;
import com.moeim.review.userreview.UserReview;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET is_deleted = true WHERE user_id = ?")
@Where(clause = "is_deleted = false")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA용 기본 생성자
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    @Column(nullable = false, name="is_deleted")
    @org.hibernate.annotations.ColumnDefault("FALSE")
    private boolean isDeleted = false;

    @Column(nullable = false, length = 255)
    @org.hibernate.annotations.ColumnDefault("''")
    private String bio = "";

    @Column(nullable = false, length = 255)
    @org.hibernate.annotations.ColumnDefault("'/images/defaultProfilePicture.png'")
    private String profilePicture;

    @Setter
    @Column(nullable = false, name = "is_profile_public")
    @org.hibernate.annotations.ColumnDefault("TRUE")
    private boolean profilePublic = true;
    
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "user_interests",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "category_id")
    private Set<Integer> interestCategoryIds = new HashSet<>();

    // 관심사 업데이트용 편의 메소드
    public void updateInterests(Set<Integer> newCategoryIds) {
        this.interestCategoryIds.clear();
        if (newCategoryIds != null) {
            this.interestCategoryIds.addAll(newCategoryIds);
        }
    }

    // === getter/setter ===
    @Setter
    @Column(name = "profile_image", columnDefinition = "BYTEA")
    private byte[] profileImage;

    @Setter
    @Column(name = "profile_image_type")
    private String profileImageType;   // "image/png", "image/jpeg" 같은 값

    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<Post> posts = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<GroupUser> groupMemberships = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<ChatParticipant> chatParticipations = new ArrayList<>();

    @OneToMany(mappedBy = "targetUser", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private List<UserReview> userReviews = new ArrayList<>();

//    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
//    private List<PostVote> votes = new ArrayList<>();

//    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
//    private List<ChatMessage> chatMessages = new ArrayList<>();
    // 불필요한 기능이라고 판단해 생략

    @Builder
    public User(String password, String email, String nickname, boolean isDeleted, String bio, String profilePicture) {
        this.password = password;
        this.email = email;
        this.nickname = nickname;
        this.isDeleted = isDeleted;
        this.bio = (bio != null) ? bio : "";
        this.profilePicture = (profilePicture != null && !profilePicture.isBlank()) ? profilePicture : "/images/defaultProfilePicture.png";
    }

    // 프로필수정
    public void updateProfile(String nickname, String bio, MultipartFile profileImage) {

        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }

        this.bio = (bio != null) ? bio : "";

//        if (profilePicture != null && !profilePicture.isBlank()) {
//            this.profilePicture = profilePicture;
//        }
    }

    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }

    public void changePassword(String newPassword) {
        this.password = newPassword;
    }
}
