package com.moeim.user;

import com.moeim.category.Category;
import com.moeim.category.CategoryService;
import com.moeim.group.GroupService;
import com.moeim.group.GroupUser;
import com.moeim.group.GroupUserService;
import com.moeim.post.Comment;
import com.moeim.post.CommentService;
import com.moeim.post.Post;
import com.moeim.post.PostService;
import com.moeim.review.userreview.UserReviewService;
import com.moeim.review.userreview.UserReviewStatsDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final UserReviewService userReviewService;
    private final CategoryService categoryService;
    private final GroupService groupService;
    private final GroupUserService groupUserService;
    private final PostService postService;
    private final CommentService commentService;

    // ==============================
    // 로그인 페이지
    // ==============================
    @GetMapping("/login_form")
    public String login_form(@RequestParam(value = "redirectURL", required = false) String redirectURL,
                             HttpServletRequest request,
                             Model model) {
        // 넘어온 파라미터가 없으면, 이전 페이지(Referer) 정보를 헤더에서 가져옴
        if (redirectURL == null || redirectURL.isEmpty()) {
            String referrer = request.getHeader("Referer");

            // 이전 페이지가 로그인 페이지거나 회원가입, 비밀번호 찾기이면 메인으로 가도록 필터링
            if (referrer != null && !referrer.contains("/login") && !referrer.contains("/signup") && !referrer.contains("findPassword")) {
                redirectURL = referrer;
            } else {
                redirectURL = "/"; // 기본값은 메인
            }
        }

        model.addAttribute("redirectURL", redirectURL);

        return "user/login_form";
    }

    // ==============================
    // 로그인 처리
    // ==============================
    @PostMapping("/login")
    public String login(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(defaultValue = "/") String redirectURL, // 숨겨둔 주소 받기
            HttpSession session,
            Model model
    ) {
        User user = userService.login(email, password);

        if (user == null) {
            model.addAttribute("error", "이메일 또는 비밀번호가 올바르지 않습니다.");
            model.addAttribute("redirectURL", redirectURL);
            return "user/login_form";
        }

        session.setAttribute("user", user);
        return "redirect:" + redirectURL;
    }

    // ==============================
    // 로그아웃
    // ==============================
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }


    // =============================
    // 마이페이지
    // ============================
    @GetMapping("/mypage")
    public String mypage(HttpSession session, Model model) {

        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/user/login_form";
        }

        return loadMypage(sessionUser.getId(), session, model);
    }

    // 다른사람 정보 확인
    @GetMapping("/mypage/{id}")
    public String otherMypage(@PathVariable("id") Long targetUserId,
                              HttpSession session,
                              Model model) {
        return loadMypage(targetUserId, session, model);
    }

    //공통된 부분
    private String loadMypage(Long targetUserId, HttpSession session, Model model) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/user/login_form";
        }

        User targetUser = userService.getUserById(targetUserId);
        if (targetUser == null) {
            return "redirect:/";
        }

        boolean isOwner = sessionUser.getId().equals(targetUserId);
        boolean canView = isOwner || targetUser.isProfilePublic();

        model.addAttribute("user", targetUser);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("canView", canView);

        model.addAttribute("loginUser", sessionUser);
        model.addAttribute("myPageUser", targetUser);

        if (!canView) {
            return "user/mypage_private";
        }

        int joinedGroupCount = (targetUser.getGroupMemberships() != null) ? targetUser.getGroupMemberships().size() : 0;
        int postCount = (targetUser.getPosts() != null) ? targetUser.getPosts().size() : 0;
        int commentCount = (targetUser.getComments() != null) ? targetUser.getComments().size() : 0;
        // int createdGroupCount = 0 ; //TODO: 그연결로직 구현

        model.addAttribute("joinedGroupCount", joinedGroupCount);
        model.addAttribute("postCount", postCount);
        model.addAttribute("commentCount", commentCount);
        // model.addAttribute("createdGroupCount", createdGroupCount);

        // 최근 게시글 5개
        List<Post> recentPosts = targetUser.getPosts().stream()
                .sorted(
                        Comparator.comparing(
                                Post::getCreatedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())
                        ).reversed()
                )
                .limit(12)
                .toList();
        model.addAttribute("myPosts", recentPosts);

        // 최근 댓글 5개
        List<Comment> recentComments = targetUser.getComments().stream()
                .sorted(
                        Comparator.comparing(
                                Comment::getCreatedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())
                        ).reversed()
                )
                .limit(12)
                .toList();
        model.addAttribute("myComments", recentComments);

        // 참여한 그룹 5개 (GroupUser)
        List<GroupUser> recentGroups = targetUser.getGroupMemberships().stream()
                .sorted(
                        Comparator.comparing(
                                GroupUser::getCreatedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())
                        ).reversed()
                )
                .limit(12)
                .toList();
        model.addAttribute("joinedGroups", recentGroups);

        // CategoryService를 통해 ID 리스트에 해당하는 Category 객체들을 가져옴
        List<Category> userInterests = categoryService.getCategoriesByIds(new ArrayList<>(targetUser.getInterestCategoryIds()));
        model.addAttribute("userInterests", userInterests);

        // 내 평가 통계 가져오기
        UserReviewStatsDTO stats = userReviewService.getReviewStats(targetUserId);
        model.addAttribute("stats", stats);

        return "user/mypage";
    }
    // =============================


    // ==============================
    // 회원가입 폼
    // ==============================
    @GetMapping("/signup_form")
    public String signup_form() {
        return "user/signup_form";
    }

    // ==============================
    // 아이디 찾기 (폼)
    // ==============================
    @GetMapping("/findId")
    public String findIdForm() {
        return "user/findId";
    }

    // ==============================
    // 아이디 찾기 처리
    // ==============================
    @PostMapping("/findId")
    public String findIdSubmit(@RequestParam("nickname") String nickname, Model model) {

        String loginId = userService.findLoginIdByNickname(nickname);

        // 사용자가 입력했던 닉네임 유지
        model.addAttribute("inputNickname", nickname);

        if (loginId == null) {
            model.addAttribute("errorMessage", "입력하신 닉네임과 일치하는 계정을 찾을 수 없습니다.");
        } else {
            // 그대로 보여줘도 되고, 가려서 보여줘도 됨
            model.addAttribute("foundId", maskEmail(loginId));
        }

        return "user/findId";
    }

    // 아이디 마스킹
    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "***" + email.substring(at); // 너무 짧으면 앞 한 글자만 숨김

        String prefix = email.substring(0, 2);  // 앞 2글자만 보여줌
        return prefix + "****" + email.substring(at);
    }

    // ==================
    // 비밀번호 찾기
    // =================
    @GetMapping("/findPassword")
    public String findPasswordForm() {
        return "user/findPassword";
    }

    @PostMapping("/findPassword")
    public String findPasswordVerify(@RequestParam("nickname") String nickname,
                                     @RequestParam("email") String email,
                                     Model model) {

        model.addAttribute("inputNickname", nickname);
        model.addAttribute("inputEmail", email);

        Optional<User> userOpt = userService.findByEmailAndNickname(email, nickname);

        if (userOpt.isEmpty()) {
            model.addAttribute("errorMessage", "입력하신 정보와 일치하는 계정을 찾을 수 없습니다.");
            return "user/findPassword";
        }

        User user = userOpt.get();

        // 2단계(비밀번호 변경 폼)를 열기 위한 정보
        model.addAttribute("verified", true);
        model.addAttribute("targetUserId", user.getId());

        return "user/findPassword";
    }

    // ==================
    // 비밀번호 재설정
    // ==================
    @PostMapping("/resetPassword")
    public String resetPassword(@RequestParam("userId") Long userId,
                                @RequestParam("newPassword") String newPassword,
                                @RequestParam("newPasswordConfirm") String newPasswordConfirm,
                                Model model) {

        if (!newPassword.equals(newPasswordConfirm)) {
            model.addAttribute("verified", true);
            model.addAttribute("targetUserId", userId);
            model.addAttribute("errorMessage", "비밀번호와 비밀번호 확인이 일치하지 않습니다.");
            return "user/findPassword";
        }

        try {
            userService.resetPassword(userId, newPassword);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "user/findPassword";
        }

        // 성공하면 로그인 페이지로 이동
        return "redirect:/user/login_form";
    }

    // ==============================
    // 회원가입 처리
    // ==============================
    @PostMapping("/signup")
    public String signup(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String passwordConfirm,
            @RequestParam String nickname,
            @RequestParam(required = false) String bio,
            @RequestParam(value = "categoryIds", required = false) List<Integer> categoryIds,
            Model model
    ) {
        if (!password.equals(passwordConfirm)) {
            model.addAttribute("error", "비밀번호와 비밀번호 확인이 일치하지 않습니다.");
            return "user/signup_form";
        }

        try {
            userService.signup(email, password, nickname, bio, categoryIds);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "user/signup_form";
        }

        return "redirect:/user/login_form";
    }

    // ============================
    // 회원정보 수정
    // ============================
    @GetMapping("/edit")
    public String editForm(HttpSession session, Model model) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/user/login_form";
        }

        User user = userService.getUserById(sessionUser.getId());
        model.addAttribute("user", user);
        return "user/edit";
    }

    @PostMapping("/edit")
    public String edit(
            @RequestParam String nickname,
            @RequestParam(required = false) String bio,
            @RequestParam(required = false) MultipartFile profileImage,
            @RequestParam(name = "profilePublic", defaultValue = "false") boolean profilPublic,
            @RequestParam(name = "categoryIds", required = false) List<Integer> categoryIds,
            HttpSession session,
            Model model
    ) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/user/login_form";
        }

        // 파일 형식 검사
        if (profileImage != null && !profileImage.isEmpty()) {
            String contentType = profileImage.getContentType();

            // 허용할 타입들만 통과
            boolean allowed =
                    "image/jpeg".equalsIgnoreCase(contentType) ||
                            "image/png".equalsIgnoreCase(contentType) ||
                            "image/gif".equalsIgnoreCase(contentType) ||
                            "image/webp".equalsIgnoreCase(contentType);

            if (!allowed) {
                model.addAttribute("user", sessionUser);
                model.addAttribute("error", "JPG, PNG, GIF, WEBP 형식의 이미지 파일만 업로드할 수 있습니다.");
                return "user/edit";
            }
        }

        try {
            User updated = userService.updateProfile(
                    sessionUser.getId(),
                    nickname,
                    bio,
                    profileImage,
                    profilPublic,
                    categoryIds
            );

            session.setAttribute("user", updated); // 세션 최신화
            model.addAttribute("user", updated);
            model.addAttribute("message", "프로필이 성공적으로 수정되었습니다.");
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }

        return "user/edit";
    }


    // 비밀번호 변경 페이지
    @GetMapping("/change-password")
    public String changePasswordForm(HttpSession session, Model model) {

        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/user/login_form";
        }

        // 그냥 화면만 띄우면 되므로 user를 넘기거나 말거나 선택
        model.addAttribute("user", sessionUser);
        return "user/change-password";
    }

    // 비밀번호 변경 처리
    @PostMapping("/change-password")
    public String changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String newPasswordConfirm,
            HttpSession session,
            Model model
    ) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/user/login_form";
        }

        if (!newPassword.equals(newPasswordConfirm)) {
            model.addAttribute("error", "새 비밀번호와 비밀번호 확인이 일치하지 않습니다.");
            return "user/change-password";
        }

        try {
            userService.changePassword(sessionUser.getId(), currentPassword, newPassword);
            model.addAttribute("message", "비밀번호가 성공적으로 변경되었습니다.");
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
        }

        return "user/change-password";
    }
    // ======================================


    // user/profile-image/{id} 로 이미지 내리는 api
    @GetMapping("/profile-image/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> getProfileImage(@PathVariable("id") Long userId) {
        User user = userService.getUserById(userId);
        if (user == null || user.getProfileImage() == null) {
            // 이미지 없으면 404. (위에서 기본 이미지로 표시하니까 상관 없음)
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        String contentType = user.getProfileImageType();  // 엔티티에 저장해 둔 MIME 타입
        if (contentType == null || contentType.isBlank()) {
            contentType = "image/png";
        }
        headers.setContentType(MediaType.parseMediaType(contentType));

        return new ResponseEntity<>(user.getProfileImage(), headers, HttpStatus.OK);
    }

    @GetMapping("/grouplist/{userId}")
    public String myGroupList(Model model,
                              @PathVariable("userId") Long userId,
                              @RequestParam(defaultValue = "0") int page,
                              HttpSession session) {

        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/user/login_form";
        }

        User targetUser = userService.getUserById(userId);
        if (targetUser == null) {
            return "redirect:/";
        }

        boolean isOwner = sessionUser.getId().equals(targetUser.getId());

        // 내 참여 모임 페이징 조회
        Page<GroupUser> groupPaging = groupUserService.getJoinedGroups(userId, page);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("groupPaging", groupPaging);
        model.addAttribute("user", targetUser); // 상단 마이페이지 링크용

        return "user/my_group_list";
    }

    // 내 게시글 목록
    @GetMapping("/postlist/{userId}")
    public String myPostList(Model model,
                             @PathVariable("userId") Long userId,
                             @RequestParam(defaultValue = "0") int page,
                             HttpSession session) {

        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/user/login_form";
        }

        User targetUser = userService.getUserById(userId);
        if (targetUser == null) {
            return "redirect:/";
        }

        boolean isOwner = sessionUser.getId().equals(targetUser.getId());

        Page<Post> postPaging = postService.getPostsByUserId(userId, page);

        model.addAttribute("isOwner", isOwner);
        model.addAttribute("postPaging", postPaging);
        model.addAttribute("user", targetUser);

        return "user/my_post_list";
    }

    // 내 댓글 목록
    @GetMapping("/commentlist/{userId}")
    public String myCommentList(Model model,
                                @PathVariable("userId") Long userId,
                                @RequestParam(defaultValue = "0") int page,
                                HttpSession session) {

        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/user/login_form";
        }

        User targetUser = userService.getUserById(userId);
        if (targetUser == null) {
            return "redirect:/";
        }

        boolean isOwner = sessionUser.getId().equals(targetUser.getId());

        Page<Comment> commentPaging = commentService.getCommentsByUserId(userId, page);

        model.addAttribute("isOwner", isOwner);
        model.addAttribute("commentPaging", commentPaging);
        model.addAttribute("user", targetUser);

        return "user/my_comment_list";
    }
}

