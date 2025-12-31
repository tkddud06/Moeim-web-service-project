package com.moeim.schedule;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 메일 전송 스케줄러 테스트용 임시 컨트롤러
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class SchedulerTestController {

    private final ScheduleScheduler scheduleScheduler;

    // 접속 주소: http://localhost:8080/test/mail/send
    @GetMapping("/mail/send")
    public String triggerMail() {
        // 스케줄러 메서드를 강제로 호출
        scheduleScheduler.sendDailyScheduleReminders();
        return "메일 발송 로직이 실행되었습니다. 로그와 메일함을 확인하세요.";
    }
}