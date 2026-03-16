package com.jhpj.Web01.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;           // 최대 실패 횟수
    private static final long LOCK_TIME_MINUTES = 10;    // 잠금 시간 (분)

    // username → [실패횟수, 마지막실패시간]
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

    /** 남은 잠금 시간(초) 반환 */
    public long getRemainingLockSeconds(String username) {
        long[] data = attemptsCache.get(username);
        if (data == null) return 0;
        long elapsed = System.currentTimeMillis() - data[1];
        long remaining = TimeUnit.MINUTES.toMillis(LOCK_TIME_MINUTES) - elapsed;
        return remaining > 0 ? remaining / 1000 : 0;
    }
}

