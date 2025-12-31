package com.moeim.global;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    /**
     * 비동기 메일 발송
     * @param to 수신자 이메일
     * @param subject 제목
     * @param text 내용
     */
    @Async
    public void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            javaMailSender.send(message);
            log.info("이메일 발송 성공: {}", to);

        } catch (Exception e) {
            log.error("이메일 발송 실패: {}", to, e);
            // 메일 발송 실패가 전체 로직을 롤백시키지 않도록 로그만 찍고 넘어감
        }
    }
}