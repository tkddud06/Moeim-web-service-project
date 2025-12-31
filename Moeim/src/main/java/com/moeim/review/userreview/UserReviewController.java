package com.moeim.review.userreview;

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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/review/user")
@RequiredArgsConstructor
public class UserReviewController {
    private final UserReviewService userReviewService;
    private final UserService userService;
    private final GroupUserService groupUserService;
    private final GroupService groupService;

    // 평가 기준 : 한번의 모임에서, 한 유저에 대해 한 번의 평가만 가능함.

    // 유저 평가. URL: /review/user/{groupId}/{userId}
    @GetMapping("/{groupId}/{targetUserId}") // TODO 그룹홈 페이지 쪽에 유저 눌러서 접근 가능하게 html 수정
    public String showUserReviewForm(
            Model model,
            @PathVariable Long groupId,
            @PathVariable Long targetUserId,
            HttpSession session,
            HttpServletResponse response) {

        // 로그인 체크
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/user/login_form";
        }

        // 최신 유저 정보 갖고와서 체크
        User user = userService.getUserById(sessionUser.getId());

        // 타겟 유저 정보 갖고와서 체크
        User targetUser = userService.getUserById(targetUserId);

        // 평가 단위 그룹 조회
        Group group = groupService.findById(groupId);

        // 작성자가 해당 그룹 유저인지 체크
        if (!groupUserService.isUserExistsInGroup(group, user)) {
            throw new IllegalArgumentException("관계를 찾을 수 없습니다.");
        }

        // 이미 평가했는지 체크
        if (userReviewService.isexistsByUserAndTargetUserAndGroup(user, targetUser, group)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 평가에 참여하였습니다.");
        }


        model.addAttribute("group", group);
        model.addAttribute("targetUser", targetUser);
        model.addAttribute("userReviewForm", new UserReviewForm());

        return "review/user_review_form";
    }

    // 유저 평가 저장. URL: /review/user/{groupId}/{targetUserId}
    @PostMapping("/{groupId}/{targetUserId}")
    public String createUserReview(
            Model model,
            @PathVariable Long groupId,
            @PathVariable Long targetUserId,
            @Valid UserReviewForm userReviewForm, // UserReviewForm DTO 사용
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

        // 타겟 유저 정보 갖고와서 체크
        User targetUser = userService.getUserById(targetUserId);

        // 평가 단위 그룹 조회
        Group group = groupService.findById(groupId);

        // 작성자가 해당 그룹 유저인지 체크
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
            userReviewService.createReview(targetUser, user, group, userReviewForm);
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

    // 유저 리뷰 리스트 페이지
    @GetMapping("/{userId}/list")
    public String listUserReviews(Model model, @PathVariable Long userId,
                                  @RequestParam(value="page", defaultValue="0") int page, HttpSession session ) {
        User sessionUser = (User) session.getAttribute("user");
        if(sessionUser == null) {
            return "redirect:/user/login_form";
        }

        User targetUser = userService.getUserById(userId) ;
        if(targetUser == null) {
            return "redirect:/";
        }

        boolean isOwner = sessionUser.getId().equals(userId);
        boolean canView = isOwner || targetUser.isProfilePublic();

        if(!canView) {
            return "user/mypage_private" ;
        }

        User user = userService.getUserById(userId);
        Page<UserReview> reviewPaging = userReviewService.getReviewsByUser(userId, page);

        model.addAttribute("targetUser", user);
        model.addAttribute("reviewPaging", reviewPaging);

        return "review/user_review_list";
    }

}
