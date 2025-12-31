package com.moeim.schedule;

import com.moeim.group.Group;
import com.moeim.group.GroupService;
import com.moeim.schedule.dto.ScheduleForm;
import com.moeim.user.User;
import com.moeim.user.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@RequestMapping("/schedule")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final UserService userService;
    private final GroupService groupService;

    @GetMapping("/{groupId}/create")
    public String createForm(
            @PathVariable Long groupId,
            @RequestParam(required = false) String date,
            Model model
    ) {
        ScheduleForm form = new ScheduleForm();

        // 날짜 클릭으로 들어온 경우 시작 시간 자동 세팅 (오전 9시)
        if (date != null) {
            LocalDateTime start = LocalDate.parse(date).atTime(9, 0);
            form.setStartDate(start);
            form.setEndDate(start.plusHours(1));
        }

        model.addAttribute("form", form);
        model.addAttribute("groupId", groupId);

        return "schedule/schedule_form";
    }

    @PostMapping("/{groupId}/create")
    public String saveSchedule(
            @PathVariable Long groupId,
            @ModelAttribute ScheduleForm form,
            HttpSession session) {

        Group group = groupService.findById(groupId);

        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/user/login_form";
        }

        // 최신 유저 정보 갖고오기
        User user = userService.getUserById(sessionUser.getId());

//          TODO 유효성 검사 실패시
//        if (bindingResult.hasErrors()) {
//            return handleBindingErrors(bindingResult, response);
//        }

        scheduleService.create(group, user, form);
        return "redirect:/group/settings/" + groupId + "#schedule-section";
    }

    @GetMapping("/{groupId}/{scheduleId}/modify")
    public String modify(@PathVariable Long groupId, @PathVariable Long scheduleId, Model model, HttpSession session) {
        Schedule schedule = scheduleService.getSchedule(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("스케줄이 존재하지 않습니다"));

        // 로그인 체크
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/user/login_form";
        }

        // 최신 유저 정보 갖고와서 체크
        User user = userService.getUserById(sessionUser.getId());

        ScheduleForm form = new ScheduleForm();
        form.setTitle(schedule.getTitle());
        form.setDescription(schedule.getDescription());
        form.setStartDate(schedule.getStartDate());
        form.setEndDate(schedule.getEndDate());

        model.addAttribute("form", form);
        model.addAttribute("scheduleId", scheduleId);   // 수정 대상 ID
        model.addAttribute("groupId", groupId); // 취소/완료 시 돌아갈 그룹 ID

        return "schedule/schedule_form";
    }

    @PostMapping("/{groupId}/{scheduleId}/modify")
    public String modify(
            @PathVariable Long groupId,
            @PathVariable Long scheduleId,
            @ModelAttribute ScheduleForm scheduleForm,
            HttpSession session
    ) {
        Schedule schedule = scheduleService.getSchedule(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("스케줄이 존재하지 않습니다"));

        // 로그인 체크
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/user/login_form";
        }

        // 최신 유저 정보 갖고와서 체크
        User user = userService.getUserById(sessionUser.getId());

        //          TODO 유효성 검사 실패시
//        if (bindingResult.hasErrors()) {
//            return handleBindingErrors(bindingResult, response);
//        }

        scheduleService.modify(groupService.findById(groupId), user, schedule, scheduleForm.getTitle(), scheduleForm.getDescription(), scheduleForm.getStartDate(), scheduleForm.getEndDate());

        return "redirect:/group/settings/" + groupId + "#schedule-section";
    }

    @PostMapping("/{groupId}/{scheduleId}/delete")
    public String deleteSchedule(@PathVariable Long groupId, @PathVariable Long scheduleId, HttpSession session) {

        Schedule schedule = scheduleService.getSchedule(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("스케줄이 존재하지 않습니다"));

        // 로그인 체크
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return "redirect:/user/login_form";
        }

        // 최신 유저 정보 갖고와서 체크
        User user = userService.getUserById(sessionUser.getId());

        scheduleService.delete(groupService.findById(groupId), user, schedule);

        return "redirect:/group/settings/" + groupId + "#schedule-section";
    }

    // 그룹 캘린더 내보내기용
    @GetMapping("/group/{groupId}/export")
    public ResponseEntity<String> exportGroupSchedule(@PathVariable Long groupId) {
        String icalContent = scheduleService.generateGroupIcs(groupId);

        String filename = "moeim_group_" + groupId + ".ics";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/calendar"))
                .body(icalContent);
    }

    // 마이페이지용 내보내기
    @GetMapping("/my/export")
    public ResponseEntity<?> exportMySchedules(HttpSession session) {
        User sessionUser = (User) session.getAttribute("user");

        // 로그인 체크
        if (sessionUser == null) {
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create("/user/login_form")); // 이동할 주소
            return new ResponseEntity<>(headers, HttpStatus.FOUND); // 302 리다이렉트 응답
        }

        // 최신 유저 정보 갖고와서 체크
        User user = userService.getUserById(sessionUser.getId());

        String icalContent = scheduleService.generateUserIcs(user.getId());

        String filename = "moeim_my_schedules.ics";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/calendar"))
                .body(icalContent);
    }
}