//ChatRoom.java
package com.moeim.chat;

import com.moeim.global.BaseTimeEntity;
import com.moeim.global.enums.ChatRoomType;
import com.moeim.group.Group;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "chatrooms")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // GROUP / DIRECT / RANDOM
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomType type;

    // 방 이름 (그룹명, 1:1이면 "A - B" 등)
    private String name;

    // 그룹 채팅방인 경우 연결 (없으면 null)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    // 1:1 방 식별용
    @Column(unique = true, length = 100)
    private String roomKey;

    // 참여자 목록
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatParticipant> participants = new ArrayList<>();

    // 메시지 목록
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    public void addParticipant(ChatParticipant participant) {
        participants.add(participant);
        participant.setChatRoom(this);
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        message.setChatRoom(this);
    }
}
