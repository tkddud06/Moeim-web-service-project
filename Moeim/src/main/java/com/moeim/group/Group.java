/// / src/main/java/com/moeim/group/Group.java

package com.moeim.group;

import com.moeim.category.Category;
import com.moeim.chat.ChatRoom;
import com.moeim.global.BaseTimeEntity;
import com.moeim.review.groupreview.GroupReview;
import com.moeim.schedule.Schedule;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "groups")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Group extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    //    @Column(name = "banner_picture")
//    private String bannerPicture;
    @Lob
    @Column(name = "banner_image")
    private byte[] bannerImage;

    @Column(name = "banner_image_type")
    private String bannerImageType;

    @Column(name = "now_count", nullable = false)
    private long nowCount = 1;

    @Column(name = "max_count", nullable = false)
    private long maxCount = 2;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, orphanRemoval = true)
    private List<GroupUser> members = new ArrayList<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Schedule> schedules = new ArrayList<>();

    @OneToOne(mappedBy = "group", cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.LAZY)
    // @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @OneToMany(mappedBy = "targetGroup", cascade = {CascadeType.ALL}, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<GroupReview> groupReviews;

    @OneToMany(mappedBy = "group", cascade = {CascadeType.ALL}, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<GroupJoinRequest> groupJoinRequests;

    @Builder
    public Group(Category category, String title, String description, long maxCount) {
        this.category = category;
        this.title = title;
        this.description = description != null ? description : "";
        this.maxCount = maxCount;
    }
//  TODO: 아래 필드 추개해 그룹한개랑 chatroom 묶기
//    @OneToOne(mappedBy = "group", fetch = FetchType.LAZY)
//    private ChatRoom chatRoom;
    // group 생성 시 chatRoomService.createGroupRoom(group) 호출
}

