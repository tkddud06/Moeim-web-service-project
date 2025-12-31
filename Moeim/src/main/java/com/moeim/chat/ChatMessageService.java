package com.moeim.chat;

import com.moeim.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;

    // 메시지 저장
    public ChatMessage sendMessage(ChatRoom room, User sender, String content) {
        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .content(content)
                .build();

        return chatMessageRepository.save(message);
    }

    // 방의 히스토리 조회 (roomId 사용)
    @Transactional(readOnly = true)
    public List<ChatMessage> getMessages(Long roomId) {
        return chatMessageRepository.findByChatRoom_IdOrderByCreatedAtAsc(roomId);
    }

    // 최근 메시지 한 줄(리스트용)
    @Transactional(readOnly = true)
    public String getLastMessagePreview(ChatRoom room) {
        ChatMessage last = chatMessageRepository.findTop1ByChatRoom_IdOrderByCreatedAtDesc(room.getId());
        if (last == null) return "";
        String text = last.getContent();
        if (text.length() > 30) {
            return text.substring(0, 30) + "...";
        }
        return text;
    }
}
