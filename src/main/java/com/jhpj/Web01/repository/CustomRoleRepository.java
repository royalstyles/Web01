package com.jhpj.Web01.repository;

import com.jhpj.Web01.entity.CustomRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 커스텀 역할 데이터 접근 레이어 — Spring Data JPA 자동 구현
 */
public interface CustomRoleRepository extends JpaRepository<CustomRole, Long> {

    /** 역할명 중복 확인 — 역할 추가 시 사용 */
    boolean existsByName(String name);

    /** 역할명으로 단건 조회 */
    Optional<CustomRole> findByName(String name);

    /** 전체 역할 목록 — 생성일 오름차순 */
    List<CustomRole> findAllByOrderByCreatedAtAsc();
}
