package com.jhpj.Web01.repository;

import com.jhpj.Web01.entity.EmailChangeToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailChangeTokenRepository extends JpaRepository<EmailChangeToken, Long> {
    Optional<EmailChangeToken> findByToken(String token);
    /** 동일 사용자의 기존 변경 요청 삭제 (중복 요청 방지) */
    void deleteByUser_Id(Long userId);
}