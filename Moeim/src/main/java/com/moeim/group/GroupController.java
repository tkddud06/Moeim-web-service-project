//GroupController.java

package com.moeim.group;

import com.moeim.category.CategoryRepository;
import com.moeim.chat.ChatRoom;
import com.moeim.chat.ChatRoomService;
import com.moeim.global.enums.PositionType;
import com.moeim.review.groupreview.GroupReviewService;
import com.moeim.review.groupreview.GroupReviewStatsDTO;
import com.moeim.review.userreview.UserReviewService;
import com.moeim.schedule.Schedule;
import com.moeim.schedule.ScheduleRepository;
import com.moeim.user.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Controller
@RequestMapping("/group")
@RequiredArgsConstructor
public class GroupController {
    private final GroupJoinRequestRepository groupJoinRequestRepository;
    private final GroupService groupService;
    private final CategoryRepository categoryRepository;
    private final GroupUserRepository groupUserRepository;
    private final ScheduleRepository scheduleRepository;
    private final GroupReviewService groupReviewService;
    private final UserReviewService userReviewService;
    private final ChatRoomService chatRoomService;
    private final GroupUserService groupUserService;

    private User requireLogin(HttpSession session) {
        User loginUser = (User) session.getAttribute("user");
        if (loginUser == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        return loginUser;
    }

    @GetMapping("/form")
    public String form(HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/user/login_form";
        }
        return "group/group_form";
    }

    @PostMapping("/form")
    public String create(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam int maxCount,
            @RequestParam Long categoryId,
            @RequestParam(required = false) MultipartFile bannerFile,
            HttpSession session
    ) throws IOException {

        User loginUser;
        try {
            loginUser = requireLogin(session);
        } catch (IllegalStateException e) {
            return "redirect:/user/login_form";
        }

        Group group = new Group();
        group.setTitle(title);
        group.setDescription(description);
        group.setMaxCount(maxCount);
        group.setNowCount(1);


        group.setCategory(
                categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new IllegalArgumentException("잘못된 카테고리"))
        );

        if (bannerFile == null || bannerFile.isEmpty()) {

            ClassPathResource defaultImg = new ClassPathResource("static/images/default_banner.jpg");
            // getInputStream()을 사용하여 JAR 내부의 데이터를 직접 읽어옵니다.
            try (InputStream is = defaultImg.getInputStream()) {
                byte[] imgBytes = is.readAllBytes();
                group.setBannerImage(imgBytes);
            }
            group.setBannerImageType("image/jpeg");  // png면 image/png
        } else {
            // 업로드된 파일 저장
            group.setBannerImage(bannerFile.getBytes());
            group.setBannerImageType(bannerFile.getContentType());
        }

        group = groupService.save(group);

        GroupUser gu = new GroupUser();
        gu.setGroup(group);
        gu.setUser(loginUser);
        gu.setPosition(PositionType.ADMIN);
        groupUserRepository.save(gu);

        ChatRoom chatRoom = chatRoomService.createGroupRoom(group);
        chatRoomService.joinGroupChat(group, loginUser);
        return "redirect:/group/home/" + group.getId();
    }

    @GetMapping("/banner/{id}")
    public ResponseEntity<byte[]> getBanner(@PathVariable Long id) {
        Group group = groupService.findById(id);

        if (group.getBannerImage() == null) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders header = new HttpHeaders();
        header.setContentType(MediaType.parseMediaType(group.getBannerImageType()));
        return new ResponseEntity<>(group.getBannerImage(), header, HttpStatus.OK);
    }

    @GetMapping("/view")
    public String view(@RequestParam("id") Long id,
                       HttpSession session,
                       Model model) {

        // 1) 그룹 정보
        Group group = groupService.findById(id);
        model.addAttribute("group", group);

        // 2) 정원 확인
        boolean isFull = group.getNowCount() >= group.getMaxCount();
        model.addAttribute("isFull", isFull);

        // 3) 로그인 유저
        User loginUser;
        try {
            loginUser = requireLogin(session);
        } catch (IllegalStateException e) {
            return "redirect:/user/login_form";
        }

        // 리뷰 작성 조건에 필요한 변수들
        boolean isUserExist = false;
        boolean isPending = false;// 이 그룹의 멤버인지
        boolean isReviewed = false;    // 리뷰를 남겼는지

        // 4) 로그인한 유저가 그룹 멤버인지 체크
        if (loginUser != null) {

            Optional<GroupUser> gu = groupUserRepository.findByGroupAndUser(group, loginUser);

            if (gu.isPresent()) {
                isUserExist = true; // 멤버 맞음

                // 5) 이미 리뷰했는지 체크
                if (groupReviewService.isexistsByUserAndTargetGroup(loginUser, group)) {
                    isReviewed = true;
                }
            } else {
                // 멤버가 아닐 때만 확인하면 됨
                isPending = groupJoinRequestRepository.existsByGroupAndUser(group, loginUser);
            }
        }

        model.addAttribute("isUserExist", isUserExist);
        model.addAttribute("isReviewed", isReviewed);
        model.addAttribute("isPending", isPending);

        // 6) 리뷰 통계 데이터
        GroupReviewStatsDTO stats = groupReviewService.getReviewStats(group.getId());
        model.addAttribute("stats", stats);

        return "group/group_view";
    }


    @GetMapping("/list")
    public String list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "popular") String sort,
            @RequestParam(defaultValue = "false") boolean hideJoined,
            @RequestParam(defaultValue = "0") int page,
            HttpSession session,
            Model model
    ) {

        if (session.getAttribute("user") == null) {
            return "redirect:/user/login_form";
        }

        User loginUser = (User) session.getAttribute("user");
        int size = 9;

        Page<Group> groupPage =
                groupService.getGroupPage(categoryId, keyword, sort, hideJoined, loginUser.getId(), page, size);

        model.addAttribute("groups", groupPage.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("sort", sort);
        model.addAttribute("page", groupPage);
        model.addAttribute("hideJoined", hideJoined);
        return "group/group_list";
    }

    @GetMapping("/signup")
    public String signupForm(@RequestParam Long id,
                             HttpSession session,
                             Model model) {

        User loginUser;
        try {
            loginUser = requireLogin(session);
        } catch (IllegalStateException e) {
            return "redirect:/user/login_form";
        }

        Group group = groupService.findById(id);
        model.addAttribute("group", group);

        return "group/group_signup";
    }

    @PostMapping("/signup")
    public String signup(
            @RequestParam Long id,
            @RequestParam(required = false) String message,
            HttpSession session,
            RedirectAttributes rttr
    ) {
        User loginUser;
        try {
            loginUser = requireLogin(session);
        } catch (IllegalStateException e) {
            return "redirect:/user/login_form";
        }
        Group group = groupService.findById(id);

        // 이미 가입된 멤버인지 체크
        if (groupUserRepository.existsByGroupAndUser(group, loginUser)) {
            return "redirect:/group/signup?id=" + id + "&result=duplicate";
        }

        // 정원 초과 체크 → 신청도 막음
        if (group.getNowCount() >= group.getMaxCount()) {
            return "redirect:/group/signup?id=" + id + "&result=full";
        }

        // 이미 신청한 적 있는지 체크
        if (groupJoinRequestService.existsByGroupAndUser(group, loginUser)) {
            return "redirect:/group/signup?id=" + id + "&result=alreadyRequested";
        }

        // 가입 "신청"만 저장
        groupJoinRequestService.saveJoinRequest(id, loginUser, message);

        return "redirect:/group/signup?id=" + id + "&result=success";

    }

    @PostMapping("/join/{requestId}/approve/{groupId}")
    @Transactional
    public String approveRequest(
            @PathVariable Long requestId,
            @PathVariable Long groupId,
            RedirectAttributes ra
    ) {

        GroupJoinRequest req = groupJoinRequestService.findById(requestId);
        Group group = req.getGroup();
        User user = req.getUser();
        // 1) 이미 멤버인지 체크
        if (groupUserRepository.existsByGroupAndUser(group, user)) {
            ra.addFlashAttribute("alert", "이미 가입된 회원입니다.");
            groupJoinRequestService.delete(requestId);  // 중복 신청 삭제
            return "redirect:/group/settings/" + groupId;
        }

        // 2) 정원 초과 체크
        long nowCount = groupUserRepository.countByGroup(group);
        if (nowCount >= group.getMaxCount()) {
            ra.addFlashAttribute("alert", "정원이 이미 가득 찼습니다.");
            return "redirect:/group/settings/" + groupId;
        }

        // 3) 멤버 추가
        GroupUser gu = new GroupUser();
        gu.setGroup(group);
        gu.setUser(user);
        gu.setPosition(PositionType.MEMBER);
        groupUserRepository.save(gu);

        // 4) 현재 인원 증가
        group.setNowCount((int) nowCount + 1);
        groupService.save(group);

        // 5) 신청 기록 삭제
        groupJoinRequestService.delete(requestId);

        ra.addFlashAttribute("alert", "승인 완료되었습니다!");

        //채팅방에 추가
        chatRoomService.joinGroupChat(group, user);
        return "redirect:/group/settings/" + groupId;
    }


    @PostMapping("/join/{requestId}/reject/{groupId}")
    public String rejectRequest(
            @PathVariable Long requestId,
            @PathVariable Long groupId
    ) {
        groupJoinRequestService.delete(requestId);
        return "redirect:/group/settings/" + groupId;
    }


    @GetMapping("/home/{id}")
    public String groupHome(@PathVariable Long id,
                            HttpSession session,
                            Model model) {


        // 로그인 유저
        User loginUser;
        try {
            loginUser = requireLogin(session);
        } catch (IllegalStateException e) {
            return "redirect:/user/login_form";
        }

        // 1) 그룹 정보
        Group group = groupService.findById(id);
        model.addAttribute("group", group);

        // 로그인 유저가 그 그룹의 회원인지 확인
        if (!groupUserService.isUserExistsInGroup(group, loginUser)) {
            throw new IllegalStateException("잘못된 접근입니다.");
        }

        // 2) 멤버 목록
        List<GroupUser> members = groupUserRepository.findByGroup(group);
        model.addAttribute("members", members);

        // 3) 일정 목록 (오름차순 → 처음 3개만)
        List<Schedule> schedules = scheduleRepository.findByGroupOrderByStartDateAsc(group);

        if (schedules.size() > 3) {
            schedules = schedules.subList(0, 3);  // 처음 3개만 자르기
        }

        model.addAttribute("schedules", schedules);

        // 4) 관리자 여부 체크
        boolean isAdmin = false;

        // 자기 그룹인지 체크용 플래그 (그룹리뷰 버튼 위해 추가)
        boolean isUserExist = false;

        if (loginUser != null) {
            GroupUser gu = groupUserRepository.findByGroupAndUser(group, loginUser)
                    .orElse(null);

            if (gu != null && gu.getPosition() == PositionType.ADMIN) {
                isAdmin = true;
                isUserExist = true; // TODO 어드민은 평가못하게 막아야 하려나?
            }

            // 자기 그룹인지 체크 (그룹리뷰 버튼 위해 추가)
            else if (gu != null) {
                isUserExist = true;
            }
        }


        // 평가 했는지 중복 체크용 플래그 (그룹리뷰 버튼 위해 추가)
        boolean isReviewed = false;
        if (groupReviewService.isexistsByUserAndTargetGroup(loginUser, group)) {
            isReviewed = true;
        }

        model.addAttribute("isAdmin", isAdmin);

        // (그룹리뷰 버튼 위해 추가)
        GroupReviewStatsDTO stats = groupReviewService.getReviewStats(group.getId());
        model.addAttribute("isUserExist", isUserExist);
        model.addAttribute("isReviewed", isReviewed);
        model.addAttribute("stats", stats);

        List<Long> reviewedTargetIds = new ArrayList<>();
        if (loginUser != null) {
            // service.getReviewedTargetIds(groupId, reviewerId) 구현 필요
            // 예: select target_user_id from user_review where group_id = ? and reviewer_id = ?
            reviewedTargetIds = userReviewService.getReviewedTargetIds(group.getId(), loginUser.getId());
        }
        model.addAttribute("reviewedTargetIds", reviewedTargetIds);

        return "group/group_home";
    }

    @PostMapping("/kick/{groupId}/{userId}")
    public String kickMember(@PathVariable Long userId,
                             @PathVariable Long groupId,
                             HttpSession session,
                             RedirectAttributes ra) {

        User loginUser = (User) session.getAttribute("user");
        if (loginUser == null) {
            ra.addFlashAttribute("error", "not_login");
            return "redirect:/group/settings/" + groupId;
        }

        Group group = groupService.findById(groupId);
        if (group == null) {
            ra.addFlashAttribute("error", "group_not_found");
            return "redirect:/group/settings/" + groupId;
        }

        GroupUser loginGU = groupUserRepository.findByGroupAndUser(group, loginUser)
                .orElse(null);

        if (loginGU == null || loginGU.getPosition() != PositionType.ADMIN) {
            ra.addFlashAttribute("error", "not_admin");
            return "redirect:/group/settings/" + groupId;
        }

        GroupUser targetGU = groupUserRepository
                .findByGroup_IdAndUser_Id(groupId, userId)
                .orElse(null);

        if (targetGU == null) {
            ra.addFlashAttribute("error", "not_found_user");
            return "redirect:/group/settings/" + groupId;
        }

        if (targetGU.getPosition() == PositionType.ADMIN) {
            ra.addFlashAttribute("error", "cannot_kick_admin");
            return "redirect:/group/settings/" + groupId;
        }

        groupUserRepository.delete(targetGU);

        group.setNowCount(group.getNowCount() - 1);
        groupService.save(group);

        ra.addFlashAttribute("kick", "success");

        return "redirect:/group/settings/" + groupId;
    }


    private final GroupJoinRequestService groupJoinRequestService;

    @GetMapping("/settings/{id}")
    public String groupSettings(
            @PathVariable Long id,
            HttpSession session,
            Model model
    ) {
        User loginUser;
        try {
            loginUser = requireLogin(session);
        } catch (IllegalStateException e) {
            return "redirect:/user/login_form";
        }
        Group group = groupService.findById(id);

        // 그룹장 체크 (owner 필드 없으므로 GroupUser에서 LEADER 조회)
        GroupUser leader = groupUserRepository
                .findByGroupAndPosition(group, PositionType.ADMIN)
                .orElse(null);


        if (leader == null || !leader.getUser().getId().equals(loginUser.getId())) {
            return "redirect:/group/home/" + id;
        }

        // 1) 소모임 정보
        model.addAttribute("group", group);

        // 2) 기존 그룹원 목록
        List<GroupUser> members = groupUserRepository.findByGroup(group);
        model.addAttribute("members", members);
        model.addAttribute("membersCount", members.size());

        // 3) 가입 신청 목록
        List<GroupJoinRequest> requests = groupJoinRequestService.findRequests(id);
        model.addAttribute("requests", requests);

        return "group/group_settings";
    }

    @PostMapping("/settings/{id}")
    public String updateSettings(
            @PathVariable Long id,
            @RequestParam String description,
            @RequestParam int maxCount,
            @RequestParam(required = false) MultipartFile bannerFile,
            HttpSession session,
            RedirectAttributes ra
    ) throws IOException {

        User loginUser;
        try {
            loginUser = requireLogin(session);
        } catch (IllegalStateException e) {
            return "redirect:/user/login_form";
        }
        Group group = groupService.findById(id);

        // 관리자 체크
        GroupUser leader = groupUserRepository
                .findByGroupAndPosition(group, PositionType.ADMIN)
                .orElse(null);

        if (leader == null || !leader.getUser().getId().equals(loginUser.getId())) {
            ra.addFlashAttribute("alert", "수정 권한이 없습니다.");
            return "redirect:/group/settings/" + id;
        }

        // 현재 인원보다 적게 설정 불가
        if (maxCount < group.getNowCount()) {
            ra.addFlashAttribute("alert",
                    "현재 멤버 수(" + group.getNowCount() + "명)보다 적게 설정할 수 없습니다.");
            return "redirect:/group/settings/" + id;
        }

        if (bannerFile != null && !bannerFile.isEmpty()) {
            group.setBannerImage(bannerFile.getBytes());
            group.setBannerImageType(bannerFile.getContentType());
        }

        // 정보 수정
        group.setDescription(description);
        group.setMaxCount(maxCount);
        groupService.save(group);

        ra.addFlashAttribute("alert", "소모임 정보가 성공적으로 수정되었습니다!");

        return "redirect:/group/settings/" + id;
    }

    @Transactional
    @PostMapping("/delete/{id}")
    public String deleteGroup(@PathVariable Long id,
                              HttpSession session,
                              RedirectAttributes ra) {

        User loginUser = (User) session.getAttribute("user");
        if (loginUser == null) {
            ra.addFlashAttribute("error", "not_login");
            return "redirect:/group/settings/" + id;
        }

        Group group = groupService.findById(id);

        if (group == null) {
            ra.addFlashAttribute("error", "group_not_found");
            return "redirect:/group/settings/" + id;
        }

        // 관리자 확인
        GroupUser leader = groupUserRepository
                .findByGroupAndUserAndPosition(group, loginUser, PositionType.ADMIN)
                .orElse(null);

        if (leader == null) {
            ra.addFlashAttribute("error", "not_admin");
            return "redirect:/group/settings/" + id;
        }

        // 삭제 순서 매우 중요!!
        groupService.deleteGroup(group.getId());


        ra.addFlashAttribute("alert", "소모임이 성공적으로 삭제되었습니다.");

        return "redirect:/group/list";
    }


    @PostMapping("/leave/{groupId}")
    @Transactional
    public String leaveGroup(@PathVariable Long groupId,
                             HttpSession session,
                             RedirectAttributes ra) {

        User loginUser = (User) session.getAttribute("user");
        if (loginUser == null) {
            ra.addFlashAttribute("alert", "로그인이 필요합니다.");
            return "redirect:/user/login_form";
        }

        Group group = groupService.findById(groupId);
        if (group == null) {
            ra.addFlashAttribute("alert", "존재하지 않는 소모임입니다.");
            return "redirect:/group/list";
        }

        // 멤버인지 확인
        GroupUser gu = groupUserRepository
                .findByGroupAndUser(group, loginUser)
                .orElse(null);

        if (gu == null) {
            ra.addFlashAttribute("alert", "이 소모임의 멤버가 아닙니다.");
            return "redirect:/group/view?id=" + groupId;
        }

        // ADMIN(모임장)은 탈퇴 불가
        if (gu.getPosition() == PositionType.ADMIN) {
            ra.addFlashAttribute("alert", "모임장은 탈퇴할 수 없습니다. 설정에서 소모임 삭제 또는 권한 위임을 먼저 진행해주세요.");
            return "redirect:/group/home/" + groupId;
        }

        // 그룹 채팅방 나가기
        groupService.leaveGroup(groupId, loginUser);

        // 멤버 삭제
        groupUserRepository.delete(gu);

        // 인원수 감소
        group.setNowCount(group.getNowCount() - 1);
        groupService.save(group);

        ra.addFlashAttribute("alert", "소모임에서 탈퇴되었습니다.");

        return "redirect:/group/list";
    }

}





