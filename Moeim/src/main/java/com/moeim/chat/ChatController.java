package com.moeim.chat;

import com.moeim.user.User;
import com.moeim.user.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatRoomService chatRoomService;
    private final UserService userService;

    @GetMapping("/list")
    public String list() {
        return "chat/chat_list";
    }

    @GetMapping("/view")
    public String view() {
        return "chat/chat_view";
    }

    // 1:1 채팅 진입
    @GetMapping("/direct/{targetUserId}")
    public String directChat(@PathVariable Long targetUserId,
                             HttpSession session,
                             Model model) {

        User loginUser = (User) session.getAttribute("user");
        if (loginUser == null) {
            return "redirect:/user/login_form";
        }

        User targetUser = userService.getUserById(targetUserId);
        if (targetUser == null) {
            // 없는 유저일 때는 적당히 홈이나 에러 페이지로
            return "redirect:/";
        }

        // 동일인 방지
        if (loginUser.getId().equals(targetUser.getId())) {
            return "redirect:/user/mypage";
        }

        // 두 사람 사이 1:1 방 찾거나 생성
        ChatRoom room = chatRoomService.getOrCreateDirectRoom(loginUser, targetUser);

        // 이 방 정보와 상대 정보 전달
        model.addAttribute("room", room);
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("targetUser", targetUser);

        // chat_view.html 안에서 room.id, targetUser.nickname 등을 가지고
        // WebSocket 연결 / 메시지 UI 구성하면 됨
        return "chat/chat_view";
    }
}
