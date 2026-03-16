package com.jhpj.Web01.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import jakarta.mail.internet.MimeMessage;



@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendVerificationEmail(String toEmail, String verifyUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);           // ✅ 추가
            helper.setTo(toEmail);
            helper.setSubject("[Web01] 이메일 인증을 완료해주세요");
            helper.setText(buildHtmlContent(verifyUrl), true); // true = HTML

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("이메일 발송 실패: " + e.getMessage());
        }
    }

    private String buildHtmlContent(String verifyUrl) {
        return """
            <div style="font-family:'Segoe UI',sans-serif;max-width:480px;margin:0 auto;
                        padding:40px;background:#f0f2f5;border-radius:12px;">
              <h2 style="color:#4f46e5;text-align:center;">📧 이메일 인증</h2>
              <p style="color:#555;text-align:center;margin:20px 0;">
                아래 버튼을 클릭하여 이메일 인증을 완료해주세요.<br>
                링크는 <strong>24시간</strong> 동안 유효합니다.
              </p>
              <div style="text-align:center;">
                <a href="%s"
                   style="display:inline-block;padding:14px 32px;background:#4f46e5;
                          color:white;text-decoration:none;border-radius:8px;font-size:16px;">
                  이메일 인증하기
                </a>
              </div>
            </div>
            """.formatted(verifyUrl);
    }
}