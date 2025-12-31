package com.moeim.chat;

import com.moeim.global.enums.ChatRoomType;
import com.moeim.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 1:1 채팅방
    Optional<ChatRoom> findByRoomKey(String roomKey);

    // 그룹 채팅방
    Optional<ChatRoom> findByGroup_Id(Long groupId);

    // 특정 유저가 참여한 방 목록
    List<ChatRoom> findDistinctByParticipants_User(User user);

    List<ChatRoom> findByType(ChatRoomType type);
}
