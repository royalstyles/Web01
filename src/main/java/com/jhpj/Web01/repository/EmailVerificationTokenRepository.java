package com.jhpj.Web01.repository;

import com.jhpj.Web01.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * 회원가입 이메일 인증 토큰 데이터 접근 레이어 — Spring Data JPA 자동 구현
 * 회원가입 인증 흐름: 토큰 저장 → 메일 발송 → 링크 클릭 시 토큰 조회/검증 → 삭제
 */
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    /** 인증 링크에 포함된 UUID 토큰으로 레코드 조회 */
    Optional<EmailVerificationToken> findByToken(String token);

    /** 사용자 ID 기준으로 기존 토큰 삭제 — 인증 메일 재발송 시 중복 방지 */
    void deleteByUser_Id(Long userId);
}
