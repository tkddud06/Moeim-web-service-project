package com.moeim.chat;

import com.moeim.global.enums.ChatRoomType;
import com.moeim.group.Group;
import com.moeim.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;

    // 방 조회
    @Transactional(readOnly = true)
    public Optional<ChatRoom> findById(Long id) {
        return chatRoomRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<ChatRoom> getRoom(Long roomId) {
        return chatRoomRepository.findById(roomId);
    }

    // 내가 참가자인 방인지 확인
    @Transactional(readOnly = true)
    public boolean isParticipant(ChatRoom room, User user) {
        return chatParticipantRepository
                .findByChatRoom_IdAndUser_Id(room.getId(), user.getId())
                .isPresent();
    }

    // 그룹 생성 시 함께 호출: 그룹 채팅방 생성
    public ChatRoom createGroupRoom(Group group) {
        Optional<ChatRoom> exist = chatRoomRepository.findByGroup_Id(group.getId());
        if (exist.isPresent()) {
            return exist.get();
        }

        ChatRoom room = ChatRoom.builder()
                .type(ChatRoomType.GROUP)
                .name(group.getTitle())
                .group(group)
                .build();

        return chatRoomRepository.save(room);
    }

    // 그룹채팅방에 유저 추가
    public void joinGroupChat(Group group, User user) {
        ChatRoom room = createGroupRoom(group);

        //이미 참가자인지
        boolean exist = isParticipant(room, user);

        if (exist) return;

        ChatParticipant participant = ChatParticipant.builder()
                .chatRoom(room)
                .user(user)
                .build();

        chatParticipantRepository.save(participant);
    }

    // 1:1 채팅방 조회/생성
    public ChatRoom getOrCreateDirectRoom(User user1, User user2) {

        if (user1.getId().equals(user2.getId())) {
            throw new IllegalArgumentException("자기 자신에게는 메시지를 보낼 수 없습니다.");
        }

        Long a = user1.getId();
        Long b = user2.getId();

        Long minId = Math.min(a, b);
        Long maxId = Math.max(a, b);

        String roomKey = "DIRECT_" + minId + "_" + maxId;

        // 이미 방이 있으면 그대로 사용
        return chatRoomRepository.findByRoomKey(roomKey)
                .orElseGet(() -> {
                    // 없으면 새로 생성
                    ChatRoom room = ChatRoom.builder()
                            .name(user1.getNickname() + " · " + user2.getNickname())
                            .type(ChatRoomType.DIRECT)
                            .roomKey(roomKey)
                            .build();

                    chatRoomRepository.save(room);

                    // 참가자 2명 등록
                    ChatParticipant p1 = ChatParticipant.builder()
                            .chatRoom(room)
                            .user(user1)
                            .build();
                    ChatParticipant p2 = ChatParticipant.builder()
                            .chatRoom(room)
                            .user(user2)
                            .build();

                    chatParticipantRepository.save(p1);
                    chatParticipantRepository.save(p2);

                    return room;
                });
    }

    // 랜덤 채팅방 생성 (매칭 큐에서 두 유저를 가져왔다는 가정)
    public ChatRoom createRandomRoom(User u1, User u2) {
        ChatRoom room = ChatRoom.builder()
                .type(ChatRoomType.RANDOM)
                .name("랜덤 채팅")
                .build();
        chatRoomRepository.save(room);

        ChatParticipant p1 = ChatParticipant.builder()
                .chatRoom(room)
                .user(u1)
                .build();
        ChatParticipant p2 = ChatParticipant.builder()
                .chatRoom(room)
                .user(u2)
                .build();

        chatParticipantRepository.save(p1);
        chatParticipantRepository.save(p2);

        room.getParticipants().add(p1);
        room.getParticipants().add(p2);

        return room;
    }

    // 특정 유저의 DIRECT 방 리스트
    @Transactional(readOnly = true)
    public List<ChatRoom> getMyDirectRooms(User me) {
        List<ChatParticipant> participants = chatParticipantRepository.findByUser(me);

        return participants.stream()
                .map(ChatParticipant::getChatRoom)
                .filter(room -> room.getType() == ChatRoomType.DIRECT)
                .distinct()
                .toList();
    }

    // 특정 DIRECT 방에서 상대 찾기
    @Transactional(readOnly = true)
    public User getDirectPartner(ChatRoom room, User me) {
        List<ChatParticipant> participants =
                chatParticipantRepository.findByChatRoom_Id(room.getId());

        return participants.stream()
                .map(ChatParticipant::getUser)
                .filter(u -> !u.getId().equals(me.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("상대 참가자를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public List<ChatRoom> findRoomsByUser(User user) {
        return chatRoomRepository.findDistinctByParticipants_User(user);
    }

    /**
     * 방에서 나의 ChatParticipant 엔티티 가져오기
     */
    @Transactional(readOnly = true)
    public ChatParticipant getMyParticipant(ChatRoom room, User me) {
        return chatParticipantRepository
                .findByChatRoom_IdAndUser_Id(room.getId(), me.getId())
                .orElseThrow(() -> new IllegalArgumentException("이 방에 참가자가 아닙니다."));
    }

    /**
     * 방에서 '나'의 lastReadMessageId 업데이트
     */
    public void updateLastRead(ChatRoom room, User me, Long lastMessageId) {
        if (lastMessageId == null) return;

        ChatParticipant cp = getMyParticipant(room, me);
        Long current = cp.getLastReadMessageId();
        if (current == null || current < lastMessageId) {
            cp.setLastReadMessageId(lastMessageId);
        }
    }

    // 벌크 삭제
    @Transactional
    public void deleteChatRoom(Long chatRoomId) {

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        chatParticipantRepository.bulkDeleteByChatRoom(chatRoom);
        chatMessageRepository.bulkDeleteByChatRoom(chatRoom);

        chatRoomRepository.delete(chatRoom);
    }

    // 그룹 채팅방 나가기
    public void leaveGroupChatRoom(Group group, User user) {
        // 이 그룹에 연결된 채팅방 찾기
        Optional<ChatRoom> opt = chatRoomRepository.findByGroup_Id(group.getId());
        if (opt.isEmpty()) {
            return; // 아직 채팅방이 없을 수도 있음
        }

        ChatRoom room = opt.get();

        // 혹시 타입이 GROUP이 아니면 방어적으로 리턴
        if (room.getType() != ChatRoomType.GROUP) {
            return;
        }

        // 이 방에서 나(유저) 참가자 찾기
        chatParticipantRepository
                .findByChatRoom_IdAndUser_Id(room.getId(), user.getId())
                .ifPresent(cp -> {
                    chatParticipantRepository.delete(cp);
                    room.getParticipants().remove(cp);
                });

        // 아무도 안 남았으면 방 자체를 지워도 됨 (선택 사항)
        if (room.getParticipants().isEmpty()) {
            chatRoomRepository.delete(room);
        }
    }
}
