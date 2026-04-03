package com.jhpj.Web01.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 로그인 실패 횟수 추적 및 계정 잠금 서비스
 * 메모리(ConcurrentHashMap) 기반으로 동작하므로 서버 재시작 시 기록이 초기화됨
 * SecurityConfig 의 successHandler/failureHandler 에서 호출되어 잠금 여부를 결정
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;           // 최대 실패 횟수 — 초과 시 계정 잠금
    private static final long LOCK_TIME_MINUTES = 10;    // 잠금 시간 (분) — 경과 후 자동 해제

    // username → [실패횟수, 마지막실패시간(ms)] 형태로 관리
    private final ConcurrentHashMap<String, long[]> attemptsCache = new ConcurrentHashMap<>();

    /** 로그인 실패 시 호출 */
    public void loginFailed(String username) {
        long[] data = attemptsCache.getOrDefault(username, new long[]{0, 0});
        data[0]++;                          // 실패 횟수 증가
        data[1] = System.currentTimeMillis(); // 마지막 실패 시간
        attemptsCache.put(username, data);
    }

    /** 로그인 성공 시 호출 */
    public void loginSucceeded(String username) {
        attemptsCache.remove(username);
    }

    /** 계정 잠금 여부 확인 */
    public boolean isBlocked(String username) {
        long[] data = attemptsCache.get(username);
        if (data == null) return false;

        // 잠금 시간이 지났으면 초기화
        long elapsed = System.currentTimeMillis() - data[1];
        if (elapsed > TimeUnit.MINUTES.toMillis(LOCK_TIME_MINUTES)) {
            attemptsCache.remove(username);
            return false;
        }

        return data[0] >= MAX_ATTEMPTS;
    }

    /**
     * 만료된 로그인 실패 기록 주기 정리 (1시간마다)
     * isBlocked() 호출 없이도 메모리가 지속 증가하는 문제를 방지
     */
    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void cleanExpiredEntries() {
        long lockMs = TimeUnit.MINUTES.toMillis(LOCK_TIME_MINUTES);
        attemptsCache.entrySet().removeIf(entry ->
                System.currentTimeMillis() - entry.getValue()[1] > lockMs);
    }

    /** 남은 잠금 시간(초) 반환 */
    public long getRemainingLockSeconds(String username) {
        long[] data = attemptsCache.get(username);
        if (data == null) return 0;
        long elapsed = System.currentTimeMillis() - data[1];
        long remaining = TimeUnit.MINUTES.toMillis(LOCK_TIME_MINUTES) - elapsed;
        return remaining > 0 ? remaining / 1000 : 0;
    }
}

