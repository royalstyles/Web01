package com.jhpj.Web01.repository;

import com.jhpj.Web01.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByToken(String token);
    void deleteByUser_Id(Long userId); // 재발송 시 기존 토큰 삭제
}
