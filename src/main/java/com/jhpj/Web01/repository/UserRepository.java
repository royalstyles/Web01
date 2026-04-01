package com.jhpj.Web01.repository;

import com.jhpj.Web01.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 회원 데이터 접근 레이어 — Spring Data JPA 자동 구현
 * 로그인 인증, 회원가입 중복 검사, 프로필 조회 등에 사용
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** username 으로 사용자 조회 — Spring Security 인증 및 컨트롤러에서 사용 */
    Optional<User> findByUsername(String username);

    /** 회원가입 시 아이디 중복 검사 */
    boolean existsByUsername(String username);

    /** 회원가입 및 이메일 변경 시 이메일 중복 검사 */
    boolean existsByEmail(String email);
}
