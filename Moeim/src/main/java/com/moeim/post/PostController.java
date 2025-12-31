package com.moeim.post;

import com.moeim.category.Category;
import com.moeim.category.CategoryService;
import com.moeim.global.enums.VoteType;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/post")
public class PostController {
    private final PostService postService;
    private final CategoryService categoryService;
    private final PostVoteService postVoteService;
    private final CommentService commentService;
    private final UserService userService;

    // 바인딩 에러 처리용 메소드
    private String handleBindingErrors(BindingResult bindingResult, HttpServletResponse response) throws IOException {
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

    // 각 포스트 페이지
    @GetMapping("/{categoryId}/{postId}")
    public String view(Model model,
                       @PathVariable("categoryId") Long categoryId,
                       @PathVariable("postId") Long postId) {
        Post post = postService.getPost(postId)
                .orElseThrow(() -> new IllegalArgumentException("포스트 존재하지 않음")); //TODO 예외처리


        // URL의 카테고리 ID와 실제 글의 카테고리가 일치하는지 검증
        if (!post.getCategory().getId().equals(categoryId)) {
            // 다르면 링크의 카테고리 주소로 리다이렉트 (또는 필요시 에러 처리)
            return "redirect:/category/" + categoryId;
        }

        // 조회수 1 올리기
        postService.increaseViewCount(postId);

        model.addAttribute("post", post);


        // 투표 정보 가져오기 (PostVoteService 사용)
        long upCount = postVoteService.getVoteCount(postId, VoteType.UP);
        long downCount = postVoteService.getVoteCount(postId, VoteType.DOWN);

        model.addAttribute("upCount", upCount);     // 추천 수 전달
        model.addAttribute("downCount", downCount); // 비추천 수 전달

        // 댓글 정보 가져오기
        List<Comment> commentList = commentService.getCommentsByPostId(postId);

        model.addAttribute("commentList", commentList);
        model.addAttribute("commentForm", new CommentForm());

        return "Category/post_view";
    }

    // 새로운 포스트 생성 페이지
    @GetMapping("/{categoryId}/create")
    public String create(Model model,
                         @PathVariable("categoryId") Long categoryId, HttpSession session) {

        if(categoryId == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "잘못된 접근입니다.");
        }

        // 로그인 체크
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/user/login_form";
        }

        // 최신 유저 정보 갖고와서 체크
        User user = userService.getUserById(sessionUser.getId());

        Category category = categoryService.getCategoryById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("카테고리가 존재하지 않습니다."));

        model.addAttribute("targetCategoryId", category.getId());
        model.addAttribute("categoryTitle", category.getTitle());
        model.addAttribute("postForm", new PostForm());
        model.addAttribute("formActionUrl", "/post/" + categoryId + "/create");
        return "/Category/post_form";
    }

    @PostMapping("/{categoryId}/create")
    public String create(Model model, @PathVariable("categoryId") Long categoryId,
                         @Valid PostForm postForm, BindingResult bindingResult,
                         HttpSession session, HttpServletResponse response) throws IOException {

        if(categoryId == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "잘못된 접근입니다.");
        }

        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/user/login_form";
        }

        // 유효성 검사 실패시
        if (bindingResult.hasErrors()) {
            return handleBindingErrors(bindingResult, response);
        }

        // 최신 유저 정보 갖고오기
        User user = userService.getUserById(sessionUser.getId());

        Category category = categoryService.getCategoryById(categoryId).orElseThrow();
        
        Post post = postService.create(category, user, postForm.getTitle(), postForm.getText());

        return "redirect:/post/" + categoryId + "/" + post.getId();
    }

    // 포스트 삭제 기능
    @GetMapping("/{categoryId}/{postId}/delete")
    public String delete(@PathVariable("categoryId") Long categoryId,
                         @PathVariable("postId") Long postId,
                         HttpSession session) {

        if(categoryId == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "잘못된 접근입니다.");
        }

        // 게시글 조회
        Post post = postService.getPost(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다"));

        // 로그인 체크
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/user/login_form";
        }

        // 권한 체크 (작성자 본인인지)
        // 이미 html 단계에서 한번 거르지만, 추가 검사
        if (!post.getUser().getId().equals(sessionUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");
        }

        // 최신 유저 정보 갖고와서 체크
        User user = userService.getUserById(sessionUser.getId());

        // 삭제
        postService.delete(post);

        // 리다이렉트
        return "redirect:/category/" + categoryId;
    }

    // 게시글 수정
    @GetMapping("{postId}/modify")
    public String modify(Model model,
                         @PathVariable("postId") Long postId,
                         PostForm postForm,
                         HttpSession session) {
        // 게시글 조회
        Post post = postService.getPost(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다"));

        // 로그인 체크
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/user/login_form";
        }

        // 권한 체크 (작성자 본인인지)
        // 이미 html 단계에서 한번 거르지만, 추가 검사
        if (!post.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");
        }

        // 기존 값 포스트폼에 넣기
        postForm.setTitle(post.getTitle());
        postForm.setText(post.getText());


        model.addAttribute("targetCategoryId", post.getCategory().getId());
        model.addAttribute("categoryTitle", post.getCategory().getTitle());
        model.addAttribute("formActionUrl", "/post/" + postId + "/modify");

        return "Category/post_form";
    }

    // 게시글 수정
    @PostMapping("{postId}/modify")
    public String modify(Model model,
                         @PathVariable("postId") Long postId,
                         @Valid PostForm postForm, BindingResult bindingResult,
                         HttpSession session, HttpServletResponse response) throws IOException {
        // 게시글 조회
        Post post = postService.getPost(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다"));

        if (bindingResult.hasErrors()) {
            return handleBindingErrors(bindingResult, response);
        }

        // 로그인 체크
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/user/login_form";
        }

        // 권한 체크 (작성자 본인인지)
        // 이미 html 단계에서 한번 거르지만, 추가 검사
        if (!post.getUser().getId().equals(sessionUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");
        }

        // 최신 유저 정보 갖고와서 체크
        User user = userService.getUserById(sessionUser.getId());

        // 수정
        postService.modify(post, postForm.getTitle(), postForm.getText());

        // 리다이렉트
        return "redirect:/post/" + post.getCategory().getId() + "/" + postId;
    }
}
