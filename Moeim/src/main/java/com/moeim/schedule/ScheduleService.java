package com.moeim.schedule;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import com.moeim.global.enums.PositionType;
import com.moeim.group.*;
import com.moeim.global.EmailService;
import com.moeim.schedule.dto.ScheduleForm;
import com.moeim.schedule.dto.ScheduleDTO;
import com.moeim.user.User;
import com.moeim.user.UserRepository;
import com.moeim.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final GroupRepository groupRepository;
    private final GroupUserRepository groupUserRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public Optional<Schedule> getSchedule(Long scheduleId) {
        return scheduleRepository.findById(scheduleId);
    }

    // 특정 그룹의 일정 조회
    public List<ScheduleDTO> getSchedulesByGroup(Group group, User currentUser) {

        // 현재 접속자가 이 그룹의 ADMIN인지 확인 (수정 권한 체크)
        boolean isAdmin = checkAdmin(group, currentUser);

        return scheduleRepository.findAllByGroup(group).stream()
                .map(s -> new ScheduleDTO(s, isAdmin))
                .collect(Collectors.toList());
    }

    // 내가 속한 모든 그룹의 일정 조회 (마이페이지용)
    public List<ScheduleDTO> getMySchedules(User user) {
        // 내가 가입한 그룹 리스트 가져오기
        List<Group> myGroups = groupUserRepository.findByUser(user).stream()
                .map(GroupUser::getGroup)
                .collect(Collectors.toList());

        // 그 그룹들의 모든 일정 가져오기
        return scheduleRepository.findAllByGroupIn(myGroups).stream()
                .map(s -> {
                    boolean isAdmin = checkAdmin(s.getGroup(), user);
                    return new ScheduleDTO(s, isAdmin);
                })
                .collect(Collectors.toList());
    }

    // 일정 생성 (권한 체크 필수)
    @Transactional
    public void create(Group group, User user, ScheduleForm form) {
        // 권한 체크: ADMIN이 아니면 에러
        if (!checkAdmin(group, user)) {
            throw new IllegalArgumentException("일정 등록 권한이 없습니다.");
        }

        Schedule schedule = Schedule.builder().group(group).title(form.getTitle()).description(form.getDescription()).startDate(form.getStartDate()).endDate(form.getEndDate()).build();
        scheduleRepository.save(schedule);

        //그룹 멤버들에게 알림 메일 발송
        notifyGroupMembers(group, schedule);
    }

    // 일정 수정 (권한 체크 필수)
    @Transactional
    public void modify(Group group, User user, Schedule schedule, String title, String description, LocalDateTime startDate, LocalDateTime endDate) {
        if (!checkAdmin(group, user)) {
            throw new IllegalArgumentException("일정 등록 권한이 없습니다.");
        }
        scheduleRepository.updateSchedule(schedule.getId(), title, description, startDate, endDate);

        // 객체 상태 동기화
        schedule.setTitle(title);
        schedule.setDescription(description);
        schedule.setStartDate(startDate);
        schedule.setEndDate(endDate);

//       Schedule newSchedule = Schedule.builder().group(group).title(title).description(description).startDate(startDate).endDate(endDate).build();

        // 그룹 멤버들에게 알림 메일 발송 (수정 알림)
        notifyGroupMembers(group, schedule);
    }

    // 일정 삭제 (권한 체크 필수)
    @Transactional
    public void delete(Group group, User user, Schedule schedule) {
        if (!checkAdmin(group, user)) {
            throw new IllegalArgumentException("일정 등록 권한이 없습니다.");
        }
        scheduleRepository.delete(schedule);
    }

    // (내부 메서드) 관리자 권한 확인
    private boolean checkAdmin(Group group, User user) {
        if (user == null) return false;
        return groupUserRepository.findByGroupAndUser(group, user)
                .map(gu -> gu.getPosition() == PositionType.ADMIN)
                .orElse(false);
    }

    // 알림 발송 헬퍼 메서드
    private void notifyGroupMembers(Group group, Schedule schedule) {
        Group managedGroup = groupRepository.findById(group.getId())
                .orElseThrow(() -> new IllegalArgumentException("그룹을 찾을 수 없습니다."));

        // 현재 시간과 일정 시작 시간 가져오기
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduleStart = schedule.getStartDate();

        // 이미 지난 일정이면 메일 보내지 않음
        if (scheduleStart.isBefore(now)) {
            return; // 반복문 내부라면 'continue;' 를 사용하세요
        }

        // 일정이 아직 시작하지 않은 경우
        // 기준 날짜 설정
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate scheduleDate = schedule.getStartDate().toLocalDate();

        // 오늘, 내일 여부 확인
        boolean isToday = scheduleDate.equals(today);
        boolean isTomorrow = scheduleDate.equals(tomorrow);

        // 날짜 문자열 생성
        String dateStr = schedule.getStartDate().toString().replace("T", " ");

        // 강조 문구 추가 (오늘인 경우를 우선 표시)
        if (isToday) {
            dateStr += " (!! \uD83D\uDEA8 D-Day 오늘입니다 !!)";
        } else if (isTomorrow) {
            dateStr += " (!! \uD83D\uDD25 내일입니다 !!)";
        }

        List<GroupUser> members = managedGroup.getMembers(); // Group 엔티티에 members 리스트가 있다고 가정
        for (GroupUser gu : members) {
            String email = gu.getUser().getEmail();
            // 제목 수정 (새 일정 -> 일정 알림)
            String subject = "[MOEIM] '" + group.getTitle() + "' 모임의 일정이 등록/수정되었습니다.";

            String text = String.format(
                    "안녕하세요, %s님.\n\n'%s' 모임의 일정이 업데이트 되었습니다.\n\n제목: %s\n일시: %s\n\n자세한 내용은 사이트에서 확인하세요.",
                    gu.getUser().getNickname(),
                    group.getTitle(),
                    schedule.getTitle(),
                    dateStr
            );
                emailService.sendEmail(email, subject, text);
            }
        }

    public String generateGroupIcs(Long groupId) {
        // 그룹 정보와 일정 리스트 조회
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹이 없습니다."));

        List<Schedule> schedules = scheduleRepository.findAllByGroup(group);

        ICalendar ical = new ICalendar();

        // 달력 이름 설정 TODO 작동여부 잘 확인
        ical.addExperimentalProperty("X-WR-CALNAME", "MOEIM - " + group.getTitle());

        // 리스트를 돌면서 이벤트를 캘린더에 넣기
        for (Schedule schedule : schedules) {
            VEvent event = new VEvent();

            event.setSummary(schedule.getTitle());
            event.setDescription(schedule.getDescription());

            Date start = Date.from(schedule.getStartDate().atZone(ZoneId.systemDefault()).toInstant());
            Date end = Date.from(schedule.getEndDate().atZone(ZoneId.systemDefault()).toInstant());

            event.setDateStart(start);
            event.setDateEnd(end);

            // 만든 이벤트를 달력에 추가
            ical.addEvent(event);
        }

        // 전체 묶음을 문자열로 변환
        return Biweekly.write(ical).go();
    }

    // 내 전체 일정 내보내기
    public String generateUserIcs(Long userId) {
        // 유저 정보 조회 (달력 이름 등을 위해)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자가 없습니다."));

        // 내가 속한 모든 그룹의 일정 조회
        // 내가 가입한 그룹 리스트 가져오기
        List<Group> myGroups = groupUserRepository.findByUser(user).stream()
                .map(GroupUser::getGroup)
                .collect(Collectors.toList());

        // 그 그룹들의 모든 일정 가져오기
        List<Schedule> schedules = scheduleRepository.findAllByGroupIn(myGroups);

        ICalendar ical = new ICalendar();

        // 달력 이름: "MOEIM - 홍길동님의 일정"
        ical.addExperimentalProperty("X-WR-CALNAME", "MOEIM - " + user.getNickname() + "님의 일정");

        // 반복문으로 일정 추가
        for (Schedule schedule : schedules) {
            VEvent event = new VEvent();

            String summary = "[" + schedule.getGroup().getTitle() + "] " + schedule.getTitle();

            event.setSummary(summary);
            event.setDescription(schedule.getDescription());

            Date start = Date.from(schedule.getStartDate().atZone(ZoneId.systemDefault()).toInstant());
            Date end = Date.from(schedule.getEndDate().atZone(ZoneId.systemDefault()).toInstant());

            event.setDateStart(start);
            event.setDateEnd(end);

            ical.addEvent(event);
        }

        return Biweekly.write(ical).go();
    }

    }