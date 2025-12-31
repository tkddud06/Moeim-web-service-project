package com.moeim.post;

import com.moeim.global.enums.VoteType;
import com.moeim.user.User;
import com.moeim.user.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.PrintWriter;

@Controller
@RequiredArgsConstructor
@RequestMapping("/vote")
public class PostVoteController {
    private final PostVoteService postVoteService;
    private final PostService postService;
    private final UserService userService;

    // 투표 기능
    @GetMapping("/{postId}")
    public String vote(@PathVariable("postId") Long postId,
                       @RequestParam(value = "type") VoteType voteType,
                       HttpSession session,
                       HttpServletResponse response) throws IOException {
        
        Post post = postService.getPost(postId)
                .orElseThrow(() -> new IllegalArgumentException("포스트 존재하지 않음"));
        String refererUrl = "/post/" + post.getCategory().getId() + "/" + postId;

        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/user/login_form";
        }

        // 최신 유저 정보 갖고오기
        User user = userService.getUserById(sessionUser.getId());

        try {
            postVoteService.create(post, user, voteType);
        } catch (ResponseStatusException e) {
            // 중복 투표 에러
            response.setContentType("text/html; charset=utf-8");
            PrintWriter out = response.getWriter();
            out.println("<script>alert('" + e.getReason() + "'); history.back();</script>");
            out.flush();
            return null;
        } catch (Exception e) {
            // 기타 에러
            response.setContentType("text/html; charset=utf-8");
            PrintWriter out = response.getWriter();
            out.println("<script>alert('오류가 발생했습니다.'); history.back();</script>");
            out.flush();
            return null;
        }

        return "redirect:" + refererUrl;
    }
}
