package com.moeim.schedule;

import com.moeim.group.Group;
import com.moeim.group.GroupService;
import com.moeim.schedule.dto.ScheduleDTO;
import com.moeim.user.User;
import com.moeim.user.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleAPIController {

    private final ScheduleService scheduleService;
    private final GroupService groupService;
    private final UserService userService;   // ✅ 추가

    // 그룹별 일정 (URL: /api/schedule?groupId=1)
    @GetMapping
    public List<ScheduleDTO> getGroupSchedules(@RequestParam Long groupId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        Group group = groupService.findById(groupId);
        return scheduleService.getSchedulesByGroup(group, user);
    }

    // 내 일정 (URL: /api/schedule/mine)
    @GetMapping("/mine")
    public List<ScheduleDTO> getMySchedules(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return List.of();
        return scheduleService.getMySchedules(user);
    }

    //  특정 유저 일정 (URL: /api/schedule/user/{userId})
    @GetMapping("/user/{userId}")
    public List<ScheduleDTO> getUserSchedules(@PathVariable Long userId,
                                              HttpSession session) {

        User viewer = (User) session.getAttribute("user");
        if (viewer == null) {
            // 로그인 안 된 상태에서 남 일정은 못 봄
            return List.of();
        }

        User target = userService.getUserById(userId);

        boolean isOwner = viewer.getId().equals(target.getId());
        boolean canView = isOwner || target.isProfilePublic(); // 프로필 공개 허용 시

        if (!canView) {
            // 권한 없으면 빈 리스트
            return List.of();
        }

        // target 유저 기준으로 일정 조회
        return scheduleService.getMySchedules(target);
    }
}