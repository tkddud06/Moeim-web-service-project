package com.moeim.chat;

import com.moeim.global.enums.ChatRoomType;
import com.moeim.user.User;
import com.moeim.user.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatApiController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final UserService userService;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;

    // ───────────────── 공통: 로그인 유저 꺼내기 ─────────────────
    private User getLoginUser(HttpSession session) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        // 세션에 있는 건 오래될 수 있으니 한번 더 DB에서 읽어오기
        return userService.getUserById(sessionUser.getId());
    }

    // ───────────────── 1) 특정 방 메시지 목록 조회 ─────────────────
    // GET /api/chat/rooms/{roomId}/messages
    @GetMapping("/rooms/{roomId}/messages")
    public List<ChatMessageDTO> getMessages(
            @PathVariable Long roomId,
            HttpSession session
    ) {
        User loginUser = getLoginUser(session);

        //예외처리 추가
        if (loginUser == null) {
            throw new IllegalArgumentException("로그인이 필욯합니다.");
        }

        ChatRoom room = chatRoomService.getRoom(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."));

        Long meId = loginUser.getId();

        // 이 방의 참가자들
        List<ChatParticipant> participants =
                chatParticipantRepository.findByChatRoom_Id(roomId);

        // 1:1 처리 값
        Long minOthersLastRead = null;
        for (ChatParticipant cp : participants) {
            if (!cp.getUser().getId().equals(meId)) {
                Long lr = cp.getLastReadMessageId();
                if (lr != null) {
                    if (minOthersLastRead == null) {
                        minOthersLastRead = lr;
                    } else {
                        minOthersLastRead = Math.min(minOthersLastRead, lr);
                    }
                }
            }
        }
        final Long finalMinOthersLastRead = minOthersLastRead;
        final boolean isGroupRoom = room.getType() == ChatRoomType.GROUP;

        return chatMessageService.getMessages(roomId).stream()
                .map(m -> {
                    ChatMessageDTO dto = new ChatMessageDTO();
                    dto.setId(m.getId());
                    dto.setSenderId(m.getSender().getId());
                    dto.setSenderNickname(m.getSender().getNickname());
                    dto.setContent(m.getContent());
                    dto.setCreatedAt(m.getCreatedAt());

                    boolean mine = m.getSender().getId().equals(meId);
                    dto.setMine(mine);

                    // 1:1 읽음 처리 로직
                    boolean readByAll = false;
                    if (!isGroupRoom && finalMinOthersLastRead != null && finalMinOthersLastRead >= m.getId()) {
                        readByAll = true;
                    }
                    dto.setReadByAll(readByAll);

                    // 그룹 채팅 읽음 처리 로직
                    if (isGroupRoom) {
                        int unreadCount = 0;
                        Long senderId = m.getSender().getId();

                        for (ChatParticipant cp : participants) {
                            // 본인은 제외
                            if (cp.getUser().getId().equals(senderId)) {
                                continue;
                            }
                            Long lr = cp.getLastReadMessageId();
                            if (lr == null || lr < m.getId()) {
                                unreadCount++;
                            }
                        }
                        dto.setUnreadMemberCount(unreadCount);
                    } else {
                        dto.setUnreadMemberCount(null);
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

    // ───────────────── 2) 메시지 전송 ─────────────────
    // POST /api/chat/rooms/{roomId}/messages  body: { "content": "..." }
    @PostMapping("/rooms/{roomId}/messages")
    public ChatMessageDTO sendMessage(
            @PathVariable Long roomId,
            @RequestBody SendRequest req,
            HttpSession session
    ) {
        User loginUser = getLoginUser(session);

        if (req.getContent() == null || req.getContent().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "내용이 비어 있습니다.");
        }

        ChatRoom room = chatRoomService.getRoom(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."));

        ChatMessage message = chatMessageService.sendMessage(room, loginUser, req.getContent().trim());
        // 보낸 직후에는 상대가 아직 읽지 않았다고 보고 readByAll=false
        ChatMessageDTO dto = ChatMessageDTO.from(message, loginUser.getId());
        dto.setReadByAll(false);
        return dto;
    }

    // ───────────────── 3) 1:1 방 생성/조회 (마이페이지 버튼) ─────────────────
    // POST /api/chat/direct/{targetUserId}
    @PostMapping("/direct/{targetUserId}")
    public DirectRoomResponse openDirectRoom(
            @PathVariable Long targetUserId,
            HttpSession session
    ) {
        User loginUser = getLoginUser(session);

        User targetUser = userService.getUserById(targetUserId);
        if (targetUser == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "상대 사용자를 찾을 수 없습니다.");
        }

        ChatRoom room = chatRoomService.getOrCreateDirectRoom(loginUser, targetUser);

        return new DirectRoomResponse(room.getId(), targetUser.getNickname());
    }

    // ───────────────── 4) 내가 참여 중인 1:1 채팅방 목록 (left list) ─────────────────
    // GET /api/chat/my-direct
    @GetMapping("/my-direct")
    public List<DirectRoomListItem> myDirectRooms(HttpSession session) {
        User me = getLoginUser(session);

        // 내가 참여한 모든 참가 관계
        List<ChatParticipant> parts = chatParticipantRepository.findByUser(me);

        return parts.stream()
                .map(ChatParticipant::getChatRoom)
                .filter(r -> r.getType() == ChatRoomType.DIRECT)
                .distinct()
                .map(room -> {
                    // 나 자신의 participant
                    ChatParticipant myPart = chatParticipantRepository
                            .findByChatRoom_IdAndUser_Id(room.getId(), me.getId())
                            .orElseThrow();

                    Long lastReadId = myPart.getLastReadMessageId();
                    long baseId = (lastReadId == null ? 0L : lastReadId);

                    long unreadCount = chatMessageRepository
                            .countByChatRoom_IdAndIdGreaterThan(room.getId(), baseId);

                    User partner = chatRoomService.getDirectPartner(room, me);
                    String preview = chatMessageService.getLastMessagePreview(room);

                    ChatMessage lastMsg =
                            chatMessageRepository.findTop1ByChatRoom_IdOrderByCreatedAtDesc(room.getId());
                    LocalDateTime lastCreatedAt = lastMsg != null ? lastMsg.getCreatedAt() : null;

                    DirectRoomListItem dto = new DirectRoomListItem();
                    dto.setRoomId(room.getId());
                    dto.setPartnerNickname(partner != null ? partner.getNickname() : "상대방");
                    dto.setLastMessagePreview(preview);
                    dto.setUnreadCount(unreadCount);
                    dto.setLastMessageCreatedAt(lastCreatedAt);
                    return dto;
                })
                // 마지막 메시지 시간 기준 내림차순 정렬 (최근 대화 맨 위)
                .sorted(Comparator.comparing(
                        DirectRoomListItem::getLastMessageCreatedAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())
                ).reversed())
                .collect(Collectors.toList());
    }

    // 그룹채팅
    @GetMapping("/my-groups")
    public List<GroupRoomListItem> myGroupRooms(HttpSession session) {
        User me = getLoginUser(session);

        // 내가 참여한 모든 참가 관계
        List<ChatParticipant> parts = chatParticipantRepository.findByUser(me);

        return parts.stream()
                .map(ChatParticipant::getChatRoom)
                .filter(r -> r.getType() == ChatRoomType.GROUP)
                .distinct()
                .map(room -> {
                    // 나 자신의 participant
                    ChatParticipant myPart = chatParticipantRepository
                            .findByChatRoom_IdAndUser_Id(room.getId(), me.getId())
                            .orElseThrow();

                    Long lastReadId = myPart.getLastReadMessageId();
                    long baseId = (lastReadId == null ? 0L : lastReadId);

                    // 아직 안 읽은 메시지 개수
                    long unreadCount = chatMessageRepository
                            .countByChatRoom_IdAndIdGreaterThan(room.getId(), baseId);

                    // 마지막 메시지
                    ChatMessage lastMsg =
                            chatMessageRepository.findTop1ByChatRoom_IdOrderByCreatedAtDesc(room.getId());
                    LocalDateTime lastCreatedAt = lastMsg != null ? lastMsg.getCreatedAt() : null;

                    String preview = chatMessageService.getLastMessagePreview(room);

                    GroupRoomListItem dto = new GroupRoomListItem();
                    dto.setRoomId(room.getId());
                    dto.setGroupTitle(
                            room.getGroup() != null
                                    ? room.getGroup().getTitle()
                                    : room.getName()
                    );
                    dto.setName(room.getName());
                    dto.setLastMessagePreview(preview);
                    dto.setUnreadCount(unreadCount);
                    dto.setLastMessageCreatedAt(lastCreatedAt);
                    return dto;
                })
                // 마지막 메시지 시간 기준 내림차순 정렬 (최근 대화 맨 위)
                .sorted(Comparator.comparing(
                        GroupRoomListItem::getLastMessageCreatedAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())
                ).reversed())
                .collect(Collectors.toList());
    }

    // ───────────────── 5) 방 읽음 처리 ─────────────────
    // POST /api/chat/rooms/{roomId}/read  body: { "lastMessageId": 123 }
    @PostMapping("/rooms/{roomId}/read")
    public void markRoomRead(
            @PathVariable Long roomId,
            @RequestBody MarkReadRequest req,
            HttpSession session
    ) {
        User loginUser = getLoginUser(session);

        ChatRoom room = chatRoomService.getRoom(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        chatRoomService.updateLastRead(room, loginUser, req.getLastMessageId());
    }

    // ───────────────── 6) 레이아웃 상단 전체 안 읽은 개수 ─────────────────
    // GET /api/chat/unread-count
    @GetMapping("/unread-count")
    public UnreadCountDTO unreadCount(HttpSession session) {
        User me = (User) session.getAttribute("user");
        if (me == null) {
            UnreadCountDTO dto = new UnreadCountDTO();
            dto.setTotalUnread(0);
            return dto;
        }

        List<DirectRoomListItem> directs = myDirectRooms(session);
        long total = directs.stream()
                .mapToLong(DirectRoomListItem::getUnreadCount)
                .sum();

        UnreadCountDTO dto = new UnreadCountDTO();
        dto.setTotalUnread(total);
        return dto;
    }

    // ───────────────── DTO들 ─────────────────

    @Data
    public static class SendRequest {
        private String content;
    }

    @Data
    public static class ChatMessageDTO {
        private Long id;
        private Long senderId;
        private String senderNickname;
        private String senderProfileImageUrl;
        private String content;
        private LocalDateTime createdAt;
        private boolean mine;       // 내가 보낸 메시지인지
        private boolean readByAll;  // 1:1에서 상대가 읽었는지
        private Integer unreadMemberCount; // 그룹채팅용

        public static ChatMessageDTO from(ChatMessage m, Long myId) {
            ChatMessageDTO dto = new ChatMessageDTO();
            dto.id = m.getId();
            dto.senderId = m.getSender().getId();
            dto.senderNickname = m.getSender().getNickname();
            dto.senderProfileImageUrl = "/user/profile-image/" + m.getSender().getId();
            dto.content = m.getContent();
            dto.createdAt = m.getCreatedAt();
            dto.mine = m.getSender().getId().equals(myId);
            dto.readByAll = false; // 기본값, getMessages에서 다시 채워줌
            dto.unreadMemberCount = null; // 기본값

            return dto;
        }
    }

    @Data
    public static class DirectRoomResponse {
        private Long roomId;
        private String partnerNickname;

        public DirectRoomResponse(Long roomId, String partnerNickname) {
            this.roomId = roomId;
            this.partnerNickname = partnerNickname;
        }
    }

    //1대1 채팅
    @Data
    public static class DirectRoomListItem {
        private Long roomId;
        private String partnerNickname;
        private String lastMessagePreview;
        private long unreadCount;
        private LocalDateTime lastMessageCreatedAt;
    }

    //그룹 채팅
    @Data
    public static class GroupRoomListItem {
        private Long roomId;
        private String groupTitle;              // 화면에서 room.groupTitle 로 사용
        private String name;                    // fallback 용
        private String lastMessagePreview;
        private long unreadCount;
        private LocalDateTime lastMessageCreatedAt;
    }

    @Data
    public static class MarkReadRequest {
        private Long lastMessageId;
    }

    @Data
    public static class UnreadCountDTO {
        private long totalUnread;
    }
}
