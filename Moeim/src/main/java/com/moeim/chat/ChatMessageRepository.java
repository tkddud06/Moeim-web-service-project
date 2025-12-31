package com.moeim.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 방의 전체 메시지 (오래된 순)
    List<ChatMessage> findByChatRoom_IdOrderByCreatedAtAsc(Long roomId);

    // 방의 마지막 메시지 (최근 1개)
    ChatMessage findTop1ByChatRoom_IdOrderByCreatedAtDesc(Long roomId);

    // 내가 읽은 마지막 ID 이후의 메시지 개수 (안 읽은 개수)
    long countByChatRoom_IdAndIdGreaterThan(Long roomId, Long lastReadMessageId);

    // 벌크 삭제 메소드
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ChatMessage cm WHERE cm.chatRoom = :room")
    void bulkDeleteByChatRoom(@Param("room") ChatRoom chatRoom);
}
