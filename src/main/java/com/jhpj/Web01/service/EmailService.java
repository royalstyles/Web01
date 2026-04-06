package com.jhpj.Web01.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import jakarta.mail.internet.MimeMessage;

/**
 * 이메일 발송 서비스
 * application-secret.properties 의 spring.mail.* 설정으로 Gmail SMTP 를 사용
 * 회원가입 인증 메일과 이메일 변경 인증 메일 두 가지 유형을 발송
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /** 발신자 주소 — application-secret.properties 의 spring.mail.username */
    @Value("${spring.mail.username}")
    private String fromEmail;

    /** 회원가입 이메일 인증 */
    public void sendVerificationEmail(String toEmail, String verifyUrl) {
        sendMail(toEmail,
                "[Web01] 이메일 인증을 완료해주세요",
                buildVerificationHtml(verifyUrl));
    }

    /** 관리자 비밀번호 초기화 — 임시 비밀번호를 회원 이메일로 발송 */
    public void sendPasswordResetEmail(String toEmail, String username, String tempPassword) {
        sendMail(toEmail,
                "[Web01] 임시 비밀번호가 발급되었습니다",
                buildPasswordResetHtml(username, tempPassword));
    }

    /** 이메일 변경 인증 */
    public void sendEmailChangeVerification(String toEmail, String username, String verifyUrl) {
        sendMail(toEmail,
                "[Web01] 이메일 변경 인증을 완료해주세요",
                buildEmailChangeHtml(username, verifyUrl));
    }

    // ── 공통 발송 ──────────────────────────────────────────
    private void sendMail(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("이메일 발송 실패: " + e.getMessage());
        }
    }

    // ── HTML 템플릿 ────────────────────────────────────────
    private String buildPasswordResetHtml(String username, String tempPassword) {
        return """
            <div style="font-family:'Segoe UI',sans-serif;max-width:480px;margin:0 auto;
                        padding:40px;background:#f0f2f5;border-radius:12px;">
              <h2 style="color:#dc2626;text-align:center;">🔑 임시 비밀번호 발급</h2>
              <p style="color:#555;text-align:center;margin:20px 0;">
                <strong>%s</strong>님의 비밀번호가 관리자에 의해 초기화되었습니다.
              </p>
              <div style="background:white;border-radius:8px;padding:20px;text-align:center;
                          border:2px dashed #dc2626;margin:20px 0;">
                <p style="color:#6b7280;font-size:13px;margin:0 0 8px;">임시 비밀번호</p>
                <p style="font-size:24px;font-weight:700;color:#1e1b4b;letter-spacing:4px;margin:0;">
                  %s
                </p>
              </div>
              <p style="color:#dc2626;text-align:center;font-size:14px;font-weight:600;">
                ⚠️ 로그인 후 반드시 비밀번호를 변경해주세요.
              </p>
              <p style="color:#999;text-align:center;font-size:12px;margin-top:16px;">
                본인이 요청하지 않은 경우 즉시 관리자에게 문의하세요.
              </p>
            </div>
            """.formatted(username, tempPassword);
    }

    private String buildVerificationHtml(String verifyUrl) {
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

    private String buildEmailChangeHtml(String username, String verifyUrl) {
        return """
            <div style="font-family:'Segoe UI',sans-serif;max-width:480px;margin:0 auto;
                        padding:40px;background:#f0f2f5;border-radius:12px;">
              <h2 style="color:#4f46e5;text-align:center;">✉️ 이메일 변경 인증</h2>
              <p style="color:#555;text-align:center;margin:20px 0;">
                <strong>%s</strong>님의 이메일 변경 요청이 접수되었습니다.<br>
                아래 버튼을 클릭하면 이메일 주소가 변경됩니다.<br>
                링크는 <strong>24시간</strong> 동안 유효합니다.
              </p>
              <div style="text-align:center;">
                <a href="%s"
                   style="display:inline-block;padding:14px 32px;background:#4f46e5;
                          color:white;text-decoration:none;border-radius:8px;font-size:16px;">
                  이메일 변경 확인하기
                </a>
              </div>
              <p style="color:#999;text-align:center;margin-top:20px;font-size:13px;">
                본인이 요청하지 않았다면 이 메일을 무시하세요.
              </p>
            </div>
            """.formatted(username, verifyUrl);
    }
}