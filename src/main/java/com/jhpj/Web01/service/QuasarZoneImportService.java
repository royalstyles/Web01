package com.jhpj.Web01.service;

import com.jhpj.Web01.entity.Category;
import com.jhpj.Web01.entity.Post;
import com.jhpj.Web01.entity.User;
import com.jhpj.Web01.repository.CategoryRepository;
import com.jhpj.Web01.repository.PostRepository;
import com.jhpj.Web01.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.jsoup.HttpStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

/**
 * 퀘이사존 핫딜 게시판 자동 수집 서비스
 * 대상: https://quasarzone.com/bbs/qe_sale
 *
 * 동작 방식:
 *   1. 목록 페이지에서 게시글 링크 수집
 *   2. DB에 없는(source_url 기준) 신규 게시글만 상세 페이지 크롤링
 *   3. 제목 + 본문(#new_contents) + 원문 링크를 붙여 게시글 생성
 *
 * 설정 (application.properties):
 *   import.quasarzone.enabled=true/false   — 자동 수집 ON/OFF
 *   import.quasarzone.interval-ms=3600000  — 수집 주기 (ms, 기본 1시간)
 *   import.author.username=admin           — 수집 글 작성자 아이디
 *   import.category.name=퀘이사존          — 수집 글 카테고리명
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuasarZoneImportService {

    private static final String BASE_URL   = "https://quasarzone.com";
    private static final String LIST_URL   = BASE_URL + "/bbs/qe_sale";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    /** 읽기 타임아웃 (ms) */
    private static final int TIMEOUT_MS  = 30_000;
    /** 게시글 수집 요청 간 딜레이 (서버 부하 방지) */
    private static final int DELAY_MS    = 2_000;
    /** 타임아웃/실패 시 최대 재시도 횟수 */
    private static final int MAX_RETRIES = 3;

    private final PostRepository     postRepository;
    private final UserRepository     userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder    passwordEncoder;

    @Value("${import.quasarzone.enabled:false}")
    private boolean enabled;

    @Value("${import.author.username:admin}")
    private String importAuthorUsername;

    @Value("${import.category.name:퀘이사존}")
    private String importCategoryName;

    /** 마지막 수집 결과 — 관리자 화면 표시용 */
    private volatile String        lastRunResult = "아직 실행되지 않음";
    private volatile LocalDateTime lastRunAt     = null;

    // ── 자동 스케줄러 ─────────────────────────────────────────

    /**
     * 설정된 주기마다 자동 수집 (import.quasarzone.enabled=true 일 때만 동작)
     * fixedDelayString: 이전 실행 완료 후 interval-ms 대기 후 재실행
     */
    @Scheduled(fixedDelayString = "${import.quasarzone.interval-ms:3600000}")
    public void scheduledImport() {
        if (!enabled) return;
        log.info("[퀘이사존] 자동 수집 시작");
        runImport(1);
    }

    // ── 수동 트리거 (관리자 UI) ───────────────────────────────

    /**
     * 관리자 화면에서 수동 수집 트리거
     *
     * @param pages 수집할 페이지 수 (1 = 최신 1페이지, 최대 5페이지 권장)
     * @return 수집 결과 요약 문자열
     */
    @Transactional
    public String runImport(int pages) {
        int total = 0;
        try {
            for (int p = 1; p <= pages; p++) {
                total += importPage(p);
                if (p < pages) delay(DELAY_MS);
            }
            lastRunResult = total + "건 수집 완료";
        } catch (Exception e) {
            lastRunResult = "수집 오류: " + e.getMessage();
            log.error("[퀘이사존] 수집 중 오류 발생", e);
        }
        lastRunAt = LocalDateTime.now();
        log.info("[퀘이사존] 수집 종료 — {}", lastRunResult);
        return lastRunResult;
    }

    // ── 내부 처리 ─────────────────────────────────────────────

    /** 목록 페이지 한 장 크롤링 → 신규 게시글 상세 수집 */
    private int importPage(int pageNum) {
        int count = 0;
        Document listDoc;
        try {
            listDoc = fetchDocument(LIST_URL + "?page=" + pageNum, BASE_URL);
        } catch (IOException e) {
            log.error("[퀘이사존] {}페이지 목록 수집 실패 — 건너뜀: {}", pageNum, e.getMessage());
            return 0;
        }

        // 응답 HTML 앞부분으로 봇 차단 여부 확인
        log.info("[퀘이사존] 응답 title={}, body 앞 200자={}",
                listDoc.title(),
                listDoc.body() != null
                        ? listDoc.body().text().substring(0, Math.min(200, listDoc.body().text().length()))
                        : "(body 없음)");

        // 목록: <ul class="event-img-list event-img-list-j"> 안의 링크
        Elements links = listDoc.select("ul.event-img-list li a[href*='/views/']");
        log.info("[퀘이사존] {}페이지 게시글 {}건 발견 (셀렉터: ul.event-img-list li a[href*='/views/'])", pageNum, links.size());

        // 셀렉터 미매칭 시 대체 후보 로그로 힌트 제공
        if (links.isEmpty()) {
            Elements allLinks = listDoc.select("a[href*='/bbs/']");
            log.warn("[퀘이사존] 셀렉터 매칭 0건 — /bbs/ 포함 링크 {}건 발견: {}",
                    allLinks.size(),
                    allLinks.stream().limit(5).map(e -> e.attr("href")).toList());
        }

        for (Element link : links) {
            String href    = link.attr("href");
            String fullUrl = BASE_URL + href;

            // source_url 기준 중복 체크 — 이미 수집된 글 건너뜀
            if (postRepository.existsBySourceUrl(fullUrl)) continue;

            if (importDetail(fullUrl)) count++;
            delay(DELAY_MS);
        }
        return count;
    }

    /** 게시글 상세 페이지 크롤링 → Post 저장 */
    private boolean importDetail(String url) {
        try {
            Document doc = fetchDocument(url, LIST_URL);

            // ── 제목 추출 ──────────────────────────────────────
            String title = Optional.ofNullable(
                            doc.selectFirst("p.subject-link, p.title.subject-link, .view-title p"))
                    .map(Element::text)
                    .filter(t -> !t.isBlank())
                    .orElse("(제목 없음)");

            // ── 본문 추출 ──────────────────────────────────────
            Element contentEl = doc.selectFirst("#new_contents");
            if (contentEl == null) {
                log.warn("[퀘이사존] 본문 영역 없음 — 건너뜀: {}", url);
                return false;
            }

            // 이미지 src 상대경로 → 절대경로 변환
            contentEl.select("img[src]").forEach(img -> {
                String src = img.attr("src");
                if (src.startsWith("//"))      img.attr("src", "https:" + src);
                else if (src.startsWith("/"))  img.attr("src", BASE_URL + src);
            });
            // 링크도 절대경로로 변환
            contentEl.select("a[href]").forEach(a -> {
                String href = a.attr("href");
                if (href.startsWith("/")) a.attr("href", BASE_URL + href);
            });

            // 원문 출처 링크를 본문 하단에 추가
            String content = contentEl.html()
                    + "<hr style='margin:24px 0;border-color:#e2e8f0'>"
                    + "<p style='font-size:12px;color:#9ca3af'>"
                    + "출처: <a href='" + url + "' target='_blank' rel='noopener'>" + url + "</a></p>";

            // ── 작성자/카테고리 조회 (없으면 자동 생성) ──────────
            User     author   = findImportAuthor();
            Category category = findOrCreateCategory();

            Post post = Post.builder()
                    .author(author)
                    .category(category)
                    .title(title)
                    .content(content)
                    .sourceUrl(url)
                    .build();

            postRepository.save(post);
            log.info("[퀘이사존] 수집: {}", title);
            return true;

        } catch (HttpStatusException e) {
            // 비밀글(403)은 예상된 케이스이므로 DEBUG 레벨로만 기록
            if (e.getStatusCode() == 403) {
                log.debug("[퀘이사존] 비밀글 — 건너뜀: {}", url);
            } else {
                log.warn("[퀘이사존] 게시글 수집 실패 (HTTP {}): {} — {}", e.getStatusCode(), url, e.getMessage());
            }
            return false;
        } catch (IOException e) {
            log.warn("[퀘이사존] 게시글 수집 실패: {} — {}", url, e.getMessage());
            return false;
        }
    }

    /**
     * Jsoup HTTP 요청 — 브라우저처럼 보이는 헤더 + 재시도 로직 포함
     * 타임아웃/네트워크 오류 시 MAX_RETRIES 횟수만큼 재시도 (지수 백오프)
     */
    private Document fetchDocument(String url, String referrer) throws IOException {
        IOException lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return Jsoup.connect(url)
                        .userAgent(USER_AGENT)
                        .referrer(referrer)
                        .header("Accept",                    "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                        .header("Accept-Language",           "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                        .header("Connection",                "keep-alive")
                        .header("Upgrade-Insecure-Requests", "1")
                        .header("Cache-Control",             "max-age=0")
                        .timeout(TIMEOUT_MS)
                        .get();
            } catch (HttpStatusException e) {
                // 403(비밀글/접근 불가)은 재시도해도 소용 없으므로 즉시 상위로 던짐
                if (e.getStatusCode() == 403) throw e;
                lastException = e;
                log.warn("[퀘이사존] 요청 실패 (시도 {}/{}): {} — {}",
                        attempt, MAX_RETRIES, url, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    // 지수 백오프: 3초 → 6초 → 9초
                    delay((long) attempt * 3_000);
                }
            } catch (IOException e) {
                lastException = e;
                log.warn("[퀘이사존] 요청 실패 (시도 {}/{}): {} — {}",
                        attempt, MAX_RETRIES, url, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    delay((long) attempt * 3_000);
                }
            }
        }
        throw lastException;
    }

    /**
     * 수집 글 작성자 조회 — 계정이 없으면 자동 생성
     *
     * 생성 조건:
     *  - username: import.author.username (기본 "퀘이사존")
     *  - password: 랜덤 UUID (BCrypt 해시) — 사실상 로그인 불가
     *  - emailVerified: false — 이메일 미인증 상태로 로그인 명시적 차단
     *  - role: ROLE_USER (최소 권한)
     */
    private User findImportAuthor() {
        return userRepository.findByUsername(importAuthorUsername)
                .orElseGet(() -> {
                    log.info("[퀘이사존] 수집용 계정 '{}' 자동 생성", importAuthorUsername);
                    return userRepository.save(User.builder()
                            .username(importAuthorUsername)
                            // 랜덤 비밀번호 해시 — 실제 비밀번호를 알 수 없으므로 로그인 불가
                            .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                            // 시스템 전용 더미 이메일 (외부 충돌 방지용 UUID 포함)
                            .email(importAuthorUsername + "-" + UUID.randomUUID() + "@system.local")
                            .emailVerified(false)   // 로그인 차단
                            .role(User.Role.ROLE_USER)
                            .build());
                });
    }

    /**
     * 수집 글 카테고리 조회 — 카테고리가 없으면 자동 생성
     */
    private Category findOrCreateCategory() {
        return categoryRepository.findByName(importCategoryName)
                .orElseGet(() -> {
                    log.info("[퀘이사존] 카테고리 '{}' 자동 생성", importCategoryName);
                    return categoryRepository.save(Category.builder()
                            .name(importCategoryName)
                            .sortOrder(100) // 기존 카테고리 뒤에 배치
                            .build());
                });
    }

    /** 서버 부하 방지 / 재시도 대기 딜레이 */
    private void delay(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ── 관리자 UI용 상태 조회 ─────────────────────────────────

    public String getLastRunResult() { return lastRunResult; }

    public String getLastRunAt() {
        return lastRunAt == null ? "-"
                : lastRunAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
