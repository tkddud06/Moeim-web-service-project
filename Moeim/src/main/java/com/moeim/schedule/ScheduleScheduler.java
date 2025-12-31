package com.moeim.schedule;

import com.moeim.global.EmailService;
import com.moeim.group.GroupUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleScheduler {

    private final ScheduleRepository scheduleRepository;
    private final EmailService emailService;

    // 매일 오전 9시에 실행 (초 분 시 일 월 요일)
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional(readOnly = true)
    public void sendDailyScheduleReminders() {
        log.info("일정 리마인더 스케줄러 시작...");

        // "내일"의 범위 구하기 (내일 00:00:00 ~ 내일 23:59:59)
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime startOfDay = tomorrow.atStartOfDay();
        LocalDateTime endOfDay = LocalDateTime.of(tomorrow, LocalTime.MAX);

        // 내일 시작하는 모든 일정 조회
        List<Schedule> schedules = scheduleRepository.findAllByStartDateBetween(startOfDay, endOfDay);

        for (Schedule schedule : schedules) {
            // 각 일정의 그룹 멤버들에게 메일 발송
            List<GroupUser> members = schedule.getGroup().getMembers();

            for (GroupUser gu : members) {
                String email = gu.getUser().getEmail();
                String subject = "[MOEIM] 내일 예정된 모임이 있습니다: " + schedule.getTitle();
                String text = String.format(
                        "안녕하세요, %s님.\n\n내일 예정된 모임 일정이 있어 알려드립니다.\n\n모임: %s\n일정: %s\n시간: %s\n\n잊지 말고 참여해주세요!",
                        gu.getUser().getNickname(),
                        schedule.getGroup().getTitle(),
                        schedule.getTitle(),
                        schedule.getStartDate().toString().replace("T", " ")
                );

                emailService.sendEmail(email, subject, text);
            }
        }

        log.info("총 {}개의 일정에 대한 알림 발송 완료.", schedules.size());
    }
}