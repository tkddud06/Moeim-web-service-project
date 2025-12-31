package com.moeim.review.groupreview;


import com.moeim.group.Group;
import com.moeim.group.GroupService;
import com.moeim.group.GroupUserService;
import com.moeim.user.User;
import com.moeim.user.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/review/group") // 그룹 평가 전용 기본 경로
@RequiredArgsConstructor
public class GroupReviewController {

    private final GroupReviewService groupReviewService;
    private final UserService userService;
    private final GroupService groupService;
    private final GroupUserService groupUserService;


    // 그룹 평가. URL: /review/group/{groupId}
    @GetMapping("/{groupId}")
    public String showGroupReviewForm(
            Model model,
            @PathVariable Long groupId,
            HttpSession session,
            HttpServletResponse response) {

        // 로그인 체크
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/user/login_form";
        }

        // 최신 유저 정보 갖고와서 체크
        User user = userService.getUserById(sessionUser.getId());

        // 평가 대상 그룹 조회
        Group group = groupService.findById(groupId);
        // 해당 그룹 유저인지 체크
        if (!groupUserService.isUserExistsInGroup(group, user)) {
            throw new IllegalArgumentException("관계를 찾을 수 없습니다.");
        }

        if (groupReviewService.isexistsByUserAndTargetGroup(user, group)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 평가에 참여하였습니다.");
        }


        model.addAttribute("group", group);
        model.addAttribute("groupReviewForm", new GroupReviewForm());

        return "review/group_review_form";
    }

    // 그룹 평가 저장. URL: /review/group/{groupId}
    @PostMapping("/{groupId}")
    public String createGroupReview(
            Model model,
            @PathVariable Long groupId,
            @Valid GroupReviewForm groupReviewForm, // GroupReviewForm DTO 사용
            BindingResult bindingResult,
            HttpSession session,
            HttpServletResponse response) throws IOException {

        // 로그인 체크
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/user/login_form";
        }

        // 최신 유저 정보 갖고와서 체크
        User user = userService.getUserById(sessionUser.getId());

        // 평가 대상 그룹 조회
        Group group = groupService.findById(groupId);
        // 해당 그룹 유저인지 체크
        if (!groupUserService.isUserExistsInGroup(group, user)) {
            throw new IllegalArgumentException("관계를 찾을 수 없습니다.");
        }

        // 유효성 검사 실패 시 (데이터 손실 없이 폼으로 돌아감)
        if (bindingResult.hasErrors()) {
            // 모든 에러 메시지를 "\n"(줄바꿈)으로 연결하여 하나의 문자열로 만듦
            String errorMessage = bindingResult.getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.joining("\\n")); // 자바스크립트 줄바꿈 문자

            // 경고창 띄우기
            response.setContentType("text/html; charset=utf-8");
            PrintWriter out = response.getWriter();
            out.println("<script>alert('" + errorMessage + "'); history.back();</script>");
            out.flush();
            return null;
        }

        String redirectUrl = "/group/home/" + groupId;

        try {
            groupReviewService.createReview(group, user, groupReviewForm);
        } catch (ResponseStatusException e) {
            // 중복 투표 에러
            response.setContentType("text/html; charset=utf-8");
            PrintWriter out = response.getWriter();
            out.println("<script>alert('" + e.getReason() + "'); window.location.href='" + redirectUrl + "';</script>");
            out.flush();
            return null;
        } catch (Exception e) {
            // 기타 에러
            response.setContentType("text/html; charset=utf-8");
            PrintWriter out = response.getWriter();
            out.println("<script>alert('오류가 발생했습니다.'); window.location.href='" + redirectUrl + "';</script>");
            out.flush();
            return null;
        }

        return "redirect:/group/home/" + groupId;
    }

    // 그룹 리뷰 리스트 페이지
    @GetMapping("/{groupId}/list")
    public String listGroupReviews(Model model, @PathVariable Long groupId,
                                   @RequestParam(value="page", defaultValue="0") int page) {
        Group group = groupService.findById(groupId);
        Page<GroupReview> reviewPaging = groupReviewService.getReviewsByGroup(groupId, page);

        model.addAttribute("group", group);
        model.addAttribute("reviewPaging", reviewPaging);

        return "review/group_review_list";
    }
}