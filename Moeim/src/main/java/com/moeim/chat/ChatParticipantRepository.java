package com.moeim.chat;

import com.moeim.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

    // 내가 참여한 모든 참가 관계
    List<ChatParticipant> findByUser(User user);

    // 특정 방의 모든 참가자
    List<ChatParticipant> findByChatRoom_Id(Long roomId);

    // 특정 방 + 특정 유저
    Optional<ChatParticipant> findByChatRoom_IdAndUser_Id(Long roomId, Long userId);


    // 벌크 삭제 메소드
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ChatParticipant cp WHERE cp.chatRoom = :room")
    void bulkDeleteByChatRoom(@Param("room") ChatRoom chatRoom);
}
