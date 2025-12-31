package com.moeim.post;

import com.moeim.user.User;
import com.moeim.user.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("comment")
public class CommentController {
    private final CommentService commentService;
    private final PostService postService;
    private final UserService userService;

    @PostMapping("/{postId}/create")
    public String create(Model model,
                         @PathVariable("postId") Long postId,
                         @Valid CommentForm commentForm, BindingResult bindingResult,
                         HttpSession session, HttpServletResponse response) throws IOException {
        // 로그인 체크
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/user/login_form";
        }

        // 게시글 조회 (댓글이 달릴 부모 글)
        Post post = postService.getPost(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시물이 존재하지 않습니다"));

        // 유효성 검사 실패 시 (내용이 비었을 때, 너무 길 때)
        if (bindingResult.hasErrors()) {
            // (A) 모든 에러 메시지를 "\n"(줄바꿈)으로 연결하여 하나의 문자열로 만듦
            String errorMessage = bindingResult.getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.joining("\\n")); // 자바스크립트 줄바꿈 문자

            // (B) 경고창 띄우기
            response.setContentType("text/html; charset=utf-8");
            PrintWriter out = response.getWriter();
            out.println("<script>alert('" + errorMessage + "'); history.back();</script>");
            out.flush();
            return null;
        }

        // 최신 유저 정보 갖고오기
        User user = userService.getUserById(sessionUser.getId());

        // 저장
        commentService.create(post, user, commentForm.getText());

         return "redirect:/post/" + post.getCategory().getId() + "/" + postId;
    }

    // 댓글 삭제 기능
    @GetMapping("/delete/{commentId}")
    public String delete(@PathVariable("commentId") Long commentId, HttpSession session) {

        // 댓글 조회
        Comment comment = commentService.getComment(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다"));

        // 로그인 체크
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/user/login_form";
        }

        // 권한 체크 (작성자 본인인지) 
        // 이미 html 단계에서 한번 거르지만, 추가 검사
        if (!comment.getUser().getId().equals(sessionUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");
        }

        // 최신 유저 정보 갖고와서 체크
        User user = userService.getUserById(sessionUser.getId());

        // 삭제
        commentService.delete(comment);

        // 리다이렉트
        Post post = comment.getPost();
        return "redirect:/post/" + post.getCategory().getId() + "/" + post.getId();
    }
}