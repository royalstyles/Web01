# Web01 — Spring Boot 웹 애플리케이션

Spring Boot 3 기반의 커뮤니티 웹 애플리케이션입니다.  
Oracle DB + Flyway 마이그레이션, Spring Security 인증 보호, 이메일 인증, 관리자 대시보드, 프로필 관리, **Quill.js 리치 텍스트 게시판** (이미지·동영상 업로드, 썸네일, 타입별 검색, 댓글, 좋아요), **알림 시스템**, **고정 공지 게시판**, **유저 사이드 패널**, **라이트/다크 테마**, GitHub Actions CI/CD, Docker 컨테이너 배포까지 포함한 풀스택 구성입니다.

---

## 📋 목차

- [기술 스택](#기술-스택)
- [프로젝트 구조](#프로젝트-구조)
- [주요 기능](#주요-기능)
- [회원가입 & 이메일 인증 흐름](#회원가입--이메일-인증-흐름)
- [게시판 구조 & 파일 업로드](#게시판-구조--파일-업로드)
- [DB 마이그레이션 (Flyway)](#db-마이그레이션-flyway)
- [시작하기](#시작하기)
- [환경 설정 & 시크릿 관리](#환경-설정--시크릿-관리)
- [CI/CD 파이프라인](#cicd-파이프라인)
- [보안 정책](#보안-정책)

---

## 🛠 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.1 |
| Security | Spring Security 6 |
| ORM | Spring Data JPA + Hibernate |
| DB Migration | Flyway (flyway-database-oracle) |
| Database | Oracle 19c (ORCLPDB1) |
| Template Engine | Thymeleaf + Layout Dialect |
| Rich Text Editor | Quill.js 2.x (CDN) |
| Build Tool | Gradle 8.11.1 |
| Container | Docker (eclipse-temurin:21) |
| CI/CD | GitHub Actions |
| Deployment | Ugreen NAS (Docker Compose) |
| Network | Tailscale VPN |
| Mail | Gmail SMTP (Spring Mail) |

---

## 📁 프로젝트 구조

```
src/main/java/com/jhpj/Web01/
├── Web01Application.java                  # 애플리케이션 진입점
├── ServletInitializer.java                # WAR 배포 설정 (지원)
├── IndexController.java                   # 기본 인덱스 엔드포인트
├── config/
│   ├── SecurityConfig.java                # Spring Security 설정
│   ├── PasswordEncoderConfig.java         # BCrypt 인코더 빈 등록
│   └── WebMvcConfig.java                  # 정적 파일 핸들러 (OS 경로 호환) + 스케줄러 활성화
├── controller/
│   ├── AuthController.java                # 로그인 / 회원가입 / 이메일 인증
│   ├── HomeController.java                # 홈 (게시판 목록 통합)
│   ├── BoardController.java               # 게시판 CRUD + 댓글 + 좋아요 + 공지 표시
│   ├── FileUploadController.java          # 이미지 · 동영상 업로드 API
│   ├── AdminController.java               # 관리자 대시보드 + 공지 관리 + 카테고리 관리
│   ├── MyPostsController.java             # 내 글 관리
│   ├── ProfileController.java             # 프로필 관리 + 이미지 업로드 + 비밀번호 재인증
│   └── NotificationController.java        # 알림 REST API
├── entity/
│   ├── User.java                          # 회원 엔티티 (UserDetails 구현)
│   ├── Post.java                          # 게시글 엔티티
│   ├── Comment.java                       # 댓글 엔티티
│   ├── Category.java                      # 게시판 카테고리 엔티티
│   ├── PostLike.java                      # 좋아요 엔티티 (복합 UNIQUE)
│   ├── PostFile.java                      # 파일 엔티티 (이미지/동영상)
│   ├── Notice.java                        # 공지 엔티티 (다대다 카테고리 연결)
│   ├── Notification.java                  # 알림 엔티티 (좋아요/댓글)
│   ├── EmailVerificationToken.java        # 회원가입 이메일 인증 토큰
│   └── EmailChangeToken.java              # 이메일 변경 인증 토큰
├── repository/
│   ├── UserRepository.java
│   ├── PostRepository.java                # fetch join 쿼리 + 제목/작성자/본문 검색 쿼리 포함
│   ├── CommentRepository.java
│   ├── CategoryRepository.java
│   ├── PostLikeRepository.java
│   ├── PostFileRepository.java            # 고아 파일 조회 쿼리 포함
│   ├── NoticeRepository.java              # 공지 조회 (EntityGraph, EXISTS 서브쿼리)
│   ├── NotificationRepository.java        # 알림 조회 + 일괄 읽음 처리
│   ├── EmailVerificationTokenRepository.java
│   └── EmailChangeTokenRepository.java
├── service/
│   ├── CustomUserDetailsService.java      # 인증 + 회원가입
│   ├── BoardService.java                  # 게시글/댓글/좋아요/파일 + 알림 발송 연동
│   ├── FileService.java                   # 파일 업로드 · 삭제 · 고아 정리 스케줄러
│   ├── EmailService.java                  # 이메일 발송 (인증 / 변경)
│   ├── AdminService.java                  # 관리자 회원 관리 + 공지 CRUD + 카테고리 CRUD
│   ├── ProfileService.java                # 프로필 변경 + 이미지 서비스 + 비밀번호 검증
│   ├── NotificationService.java           # 알림 생성/조회/읽음 처리
│   ├── QuasarZoneImportService.java       # 퀘이사존 특가/예판 크롤링 (1시간 주기)
│   └── LoginAttemptService.java           # 로그인 실패 횟수 관리
└── util/
    └── PasswordValidator.java             # 비밀번호 정책 검증

src/main/resources/
├── application.properties                 # 공통 설정
├── application-local.properties           # 로컬 개발용 (SQL 로그 + 업로드 경로)
├── application-secret.properties          # 🔒 시크릿 (Git 제외, 배포 시 주입)
├── db/migration/
│   ├── V1__init_users.sql
│   ├── V2__email_verification_tokens.sql
│   ├── V3__email_change_tokens.sql
│   ├── V4__board_categories_posts.sql
│   ├── V5__board_comments.sql
│   ├── V6__board_post_likes.sql           # 좋아요 증감 트리거 포함
│   ├── V7__board_post_files.sql
│   ├── V8__user_profile_image.sql
│   ├── V9__categories_sort_order_update.sql
│   ├── V10__post_reads.sql                # 게시글 읽음 표시
│   ├── V11__posts_add_source_url.sql      # 외부 크롤링 원본 URL
│   ├── V12__persistent_logins.sql         # Remember-Me 영속 세션
│   ├── V13__notifications.sql             # 알림 테이블
│   ├── V14__notices.sql                   # 공지 테이블
│   ├── V15__notices_add_category.sql      # 공지 단일 카테고리 (V16에서 대체)
│   └── V16__notice_categories_many_to_many.sql  # 공지-카테고리 다대다 조인 테이블
├── static/
│   ├── css/
│   │   └── theme.css                      # 라이트/다크 모드 CSS 변수 & 오버라이드
│   ├── js/
│   │   └── theme.js                       # FOUC 방지 테마 복원 스크립트
│   └── favicon.svg
└── templates/
    ├── admin.html                         # 관리자 대시보드 (공지/카테고리/회원 관리)
    ├── my-posts.html                      # 내 글 관리
    ├── profile.html                       # 프로필 수정
    ├── fragments/
    │   └── header.html                    # 공통 헤더 + 유저 사이드 패널 + 알림
    ├── auth/
    │   ├── login.html
    │   ├── signup.html
    │   ├── profile-verify.html            # 프로필 접근 전 비밀번호 재인증
    │   └── verify-result.html
    └── board/
        ├── board-list.html                # 게시판 목록 + 상단 고정 공지 + 홈
        ├── board-view.html                # 게시글 상세 (댓글, 좋아요 AJAX)
        └── board-write.html               # 게시글 작성/수정 (Quill.js 에디터)
```

---

## ✅ 주요 기능

### 회원 인증
- 커스텀 로그인 페이지 (`/auth/login`)
- BCrypt 비밀번호 암호화
- 로그아웃 시 세션 및 쿠키 완전 삭제 (CSRF 토큰 방식 POST)
- 아이디 기억하기 (localStorage 저장, `form.requestSubmit()` 이벤트 연동)
- Remember-Me 영속 로그인 (DB 기반 `persistent_logins`)

### 이메일 인증
- 회원가입 시 즉시 인증 메일 발송 (Gmail SMTP)
- 인증 전 계정은 `isEnabled() = false` → 로그인 차단 (`?unverified=true`)
- 인증 링크 유효시간: **24시간**

### 로그인 보호 (Brute-Force 방지)
- 5회 실패 시 **10분 계정 잠금**, 남은 시간(초) 안내
- 인메모리 `ConcurrentHashMap` 기반

### 권한 관리
| 권한 | 접근 가능 경로 |
|------|----------------|
| 비로그인 | `/`, `/home`, `/board/**` (읽기 전용), `/auth/**` |
| `ROLE_USER` | 비로그인 + 게시글 작성/수정/삭제, 댓글, 좋아요, `/profile/**`, `/my-posts/**` |
| `ROLE_ADMIN` | 전체 + `/admin/**` |

### 라이트/다크 테마
- 모든 페이지 공통 적용 (`fragments/header :: headerStyles` 프래그먼트)
- `theme.js`: 페이지 로드 직후 `<html data-theme>` 복원 → FOUC(화면 깜빡임) 방지
- `theme.css`: CSS 변수 기반 색상 관리, `[data-theme="dark"]` 오버라이드로 모든 컴포넌트 커버
- 헤더 우측 🌙/☀️ 버튼으로 즉시 전환, `localStorage`에 설정 영구 저장

### 게시판
- **목록/검색**: 카테고리 필터 + **검색 타입(제목 / 작성자 / 제목+본문)** 선택 키워드 검색, 10건 페이징
- **썸네일**: 본문 첫 번째 이미지 자동 추출 표시, 동영상만 있는 경우 🎬 아이콘 표시
- **Quill.js 리치 텍스트 에디터**: 폰트, 색상, 정렬, 코드블록, 이미지, 동영상 지원
- **이미지 업로드**: 툴바 클릭 → 서버 저장 → URL 삽입 (최대 20MB)
- **동영상 업로드**: 별도 버튼 → 서버 저장 → `<video controls>` 태그로 에디터 삽입 (최대 500MB)
- **댓글**: AJAX 비동기 등록/수정/삭제 (본인 + 관리자 수정·삭제 가능)
- **좋아요**: AJAX 토글, Oracle 트리거로 `POSTS.LIKE_COUNT` 자동 동기화
- **조회수**: 상세 페이지 접근 시 자동 증가
- **읽음 표시**: 읽은 게시글 목록에서 시각적으로 구분
- **수정/삭제**: 작성자 또는 관리자만 가능
- **비로그인 공개**: 목록 및 상세 읽기 가능, 작성/댓글/좋아요는 로그인 필요

### 공지 시스템
- 관리자가 게시판 목록 **상단에 고정 표시**되는 공지 등록/수정/삭제
- **노출 게시판 다중 선택**: 태그 UI로 카테고리 복수 선택 (선택 없음 = 전체 게시판)
- **정렬 순서 변경**: ▲▼ 버튼으로 공지 순서 조정
- **노출 여부 토글**: 삭제 없이 숨김/표시 전환
- 공지 클릭 시 모달로 내용 표시
- 1페이지에서만 공지 표시 (페이지 2 이상 미표시)

### 알림 시스템
- 내 게시글에 **댓글** 또는 **좋아요**가 달리면 실시간 알림 생성
- 헤더 유저 사이드 패널의 🔔 버튼으로 알림 드롭다운 표시
- 읽지 않은 알림 수 뱃지 표시
- 알림 클릭 → 해당 게시글로 이동 + 읽음 처리
- 전체 읽음 처리 지원
- 자기 자신 행동(자기 글에 좋아요/댓글)은 알림 생성 제외
- REST API: `GET /api/notifications`, `POST /api/notifications/{id}/read`, `POST /api/notifications/read-all`

### 유저 사이드 패널
- 로그인 시 화면 우측에 고정 표시 (스크롤 따라옴)
- 프로필 아바타, 아이디, 권한 뱃지, 내 작성글 링크, 알림 버튼, 로그아웃
- 메인 콘텐츠 영역 오른쪽 10px 위치에 동적 배치 (JS `getBoundingClientRect`)
- 공간 부족 시 자동 숨김

### 프로필 관리 (`/profile`)
- **비밀번호 재인증 게이트**: 프로필 접근 전 비밀번호 확인 (10분 세션 유지)
- **프로필 이미지**: 카메라 아이콘 클릭 → 즉시 업로드, 삭제 가능
- **아이디 변경**: 중복 검사 후 변경 → 자동 로그아웃 (세션 무효화)
- **비밀번호 변경**: 현재 비밀번호 확인 + 실시간 강도 표시 바
- **이메일 변경**: 새 이메일로 인증 링크 발송 → 클릭 후 변경 완료 (Race Condition 방지)

### 파일 업로드
- 저장 경로: `APP_UPLOAD_PATH` 환경변수 지정 (OS별 분리)
  - Docker 배포: `/app/uploads/`
  - 로컬 Windows: `C:/NAS Uploads/` (공백 포함 경로 `Paths.get().toUri()` 로 호환 처리)
- 하위 디렉토리: `images/`, `videos/`, `profiles/`
- UUID 기반 저장 파일명으로 중복 방지
- **고아 파일 자동 정리 스케줄러**: 매 1시간마다 게시글 미연결 임시 파일 삭제 (프로필 이미지 제외)
- 파일↔게시글 연결: 임시 업로드(`post=null`) → 게시글 저장 시 `attachFilesToPost()`로 연결

### 내 글 관리 (`/my-posts`)
- 본인이 작성한 게시글 목록 조회 및 관리

### 관리자 대시보드 (`/admin`)
- 통계 카드: 전체 회원 / 관리자 / 이메일 미인증 / 잠금 계정
- 권한 변경 (`ROLE_USER` ↔ `ROLE_ADMIN`), 계정 잠금 해제, 이메일 인증 강제 완료, 회원 삭제
- 자기 자신 권한 변경 및 삭제 방지
- **공지 관리**: 등록/수정/삭제, 순서 변경(▲▼), 노출 여부 토글, 노출 게시판 다중 선택
- **카테고리 관리**: 게시판 카테고리 추가/수정/삭제, 정렬 순서 관리
- **퀘이사존 수집**: 수동 트리거 + 자동 1시간 주기 크롤링

### 퀘이사존 특가/예판 크롤링
- 퀘이사존 특가·예판 게시글 자동 수집 (1시간 주기 스케줄러)
- 관리자 화면에서 수동 실행 가능 (최대 5페이지)
- 원본 URL 저장 (`SOURCE_URL`), 중복 수집 방지

---

## 📧 회원가입 & 이메일 인증 흐름

```
[사용자] 회원가입 폼 제출
    │
    ▼
[서버] 유효성 검사 (아이디·이메일 중복, 비밀번호 정책)
    │
    ▼
[서버] USERS 저장 (emailVerified = false)
    │
    ▼
[서버] UUID 토큰 생성 → EMAIL_VERIFICATION_TOKENS 저장 (24시간)
    │
    ▼
[서버] Gmail SMTP 인증 메일 발송
    │                          │
    ▼                          ▼
[사용자] 인증 링크 클릭       [미클릭 시 로그인 시도]
    │                          │
    ▼                          ▼
[서버] 토큰 검증               [서버] isEnabled() = false
    │                               → ?unverified=true 차단
    ▼
[서버] emailVerified = true 업데이트, 토큰 삭제
    │
    ▼
[사용자] 로그인 가능
```

---

## 📋 게시판 구조 & 파일 업로드

### 게시글 작성/수정 흐름

```
[사용자] 이미지/동영상 삽입 (Quill 툴바 or 동영상 버튼)
    │
    ▼
POST /api/upload/image  또는  /api/upload/video
    │
    ▼
[서버] FileService.upload() → {APP_UPLOAD_PATH}/{images|videos}/UUID.ext 저장
       PostFile(post=null) DB 저장 (임시 상태)
    │
    ▼
[응답] { url: "/uploads/{images|videos}/UUID.ext" }
       → 이미지: Quill 에디터에 <img> 삽입
       → 동영상: 커스텀 VideoBlot으로 <video controls> 삽입
            (기본 <iframe> blot 대신 사용 — X-Frame-Options: DENY 우회)
    │
    ▼
[사용자] 게시글 등록 버튼 클릭
    │
    ▼
[서버] 본문 HTML에서 storedName 추출 → attachFilesToPost() → post_id 연결
       (미연결 파일은 스케줄러가 1시간 후 자동 삭제)
```

### 파일 제한

| 구분 | 허용 형식 | 최대 크기 |
|------|-----------|-----------|
| 이미지 | jpg, png, gif, webp | 20MB |
| 동영상 | mp4, webm, ogg, mov | 500MB |
| 프로필 이미지 | jpg, png, gif, webp | 20MB |

### 동영상 렌더링 방식

Quill.js 기본 video blot은 `<iframe>`을 삽입하는데, Spring Security의 `X-Frame-Options: DENY` 헤더가 iframe 내 리소스 로딩을 차단합니다. 이를 해결하기 위해 커스텀 VideoBlot을 등록해 `<video controls>` 태그로 저장합니다.  
기존 DB에 `<iframe class="ql-video">`로 저장된 게시글은 `board-view.html`의 클라이언트 스크립트가 `<video>` 태그로 변환해 재생합니다.

---

## 🗄 DB 마이그레이션 (Flyway)

JPA `ddl-auto=validate`, **Flyway**로 DDL 관리. 앱 시작 시 `db/migration/` 파일 순서대로 실행.

| 버전 | 파일 | 내용 |
|------|------|------|
| V1 | `V1__init_users.sql` | USERS 테이블 + USERS_SEQ |
| V2 | `V2__email_verification_tokens.sql` | EMAIL_VERIFICATION_TOKENS + EVT_SEQ |
| V3 | `V3__email_change_tokens.sql` | EMAIL_CHANGE_TOKENS + ECT_SEQ |
| V4 | `V4__board_categories_posts.sql` | CATEGORIES, POSTS + 기본 카테고리 3개 |
| V5 | `V5__board_comments.sql` | COMMENTS (CASCADE DELETE) |
| V6 | `V6__board_post_likes.sql` | POST_LIKES + 좋아요 증감 트리거 |
| V7 | `V7__board_post_files.sql` | POST_FILES (임시 업로드 지원) |
| V8 | `V8__user_profile_image.sql` | USERS.PROFILE_IMAGE 컬럼 추가 |
| V9 | `V9__categories_sort_order_update.sql` | 카테고리 정렬 인덱스 추가 |
| V10 | `V10__post_reads.sql` | POST_READS 테이블 (읽음 표시) |
| V11 | `V11__posts_add_source_url.sql` | POSTS.SOURCE_URL 컬럼 (크롤링 원본 URL) |
| V12 | `V12__persistent_logins.sql` | PERSISTENT_LOGINS (Remember-Me) |
| V13 | `V13__notifications.sql` | NOTIFICATIONS 테이블 (좋아요/댓글 알림) |
| V14 | `V14__notices.sql` | NOTICES 테이블 + NOTICES_SEQ |
| V15 | `V15__notices_add_category.sql` | NOTICES.CATEGORY_ID 단일 컬럼 (V16에서 대체) |
| V16 | `V16__notice_categories_many_to_many.sql` | NOTICE_CATEGORIES 조인 테이블 (공지-카테고리 N:M) |

> ⚠️ 기존 DB에 처음 Flyway를 적용할 때는 `baseline-on-migrate=true`, `baseline-version=9`로 설정하세요.

### 주요 테이블 구조

**POSTS**
| 컬럼 | 타입 | 설명 |
|------|------|------|
| ID | NUMBER | PK (POSTS_SEQ) |
| USER_ID | NUMBER | FK → USERS.ID |
| CATEGORY_ID | NUMBER | FK → CATEGORIES.ID (삭제 시 NULL) |
| TITLE | VARCHAR2(200) | 제목 |
| CONTENT | CLOB | Quill.js HTML 본문 |
| VIEW_COUNT | NUMBER | 조회수 |
| LIKE_COUNT | NUMBER | 좋아요 수 (트리거 자동 동기화) |
| CREATED_AT | TIMESTAMP | 작성일 |

**POST_FILES**
| 컬럼 | 타입 | 설명 |
|------|------|------|
| ID | NUMBER | PK (POST_FILES_SEQ) |
| POST_ID | NUMBER | FK → POSTS.ID (NULL = 임시 업로드) |
| UPLOADER_ID | NUMBER | FK → USERS.ID |
| STORED_NAME | VARCHAR2(500) | UUID 기반 저장 파일명 (UNIQUE) |
| FILE_URL | VARCHAR2(1000) | 브라우저 접근 URL |
| FILE_TYPE | VARCHAR2(10) | IMAGE / VIDEO |
| FILE_SIZE | NUMBER | 바이트 단위 |

**NOTICES**
| 컬럼 | 타입 | 설명 |
|------|------|------|
| ID | NUMBER | PK (NOTICES_SEQ) |
| AUTHOR_ID | NUMBER | FK → USERS.ID (CASCADE DELETE) |
| TITLE | VARCHAR2(200) | 공지 제목 |
| CONTENT | CLOB | 공지 본문 |
| SORT_ORDER | NUMBER | 표시 순서 (낮을수록 상단) |
| ACTIVE | NUMBER(1) | 노출 여부 |
| CREATED_AT | TIMESTAMP | 작성일 |

**NOTICE_CATEGORIES** (공지-카테고리 N:M 조인 테이블)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| NOTICE_ID | NUMBER | FK → NOTICES.ID (CASCADE DELETE) |
| CATEGORY_ID | NUMBER | FK → CATEGORIES.ID (CASCADE DELETE) |

**NOTIFICATIONS**
| 컬럼 | 타입 | 설명 |
|------|------|------|
| ID | NUMBER | PK (NOTIFICATIONS_SEQ) |
| RECIPIENT_ID | NUMBER | FK → USERS.ID (CASCADE DELETE) |
| ACTOR_ID | NUMBER | FK → USERS.ID (SET NULL) |
| POST_ID | NUMBER | FK → POSTS.ID (CASCADE DELETE) |
| TYPE | VARCHAR2(20) | LIKE / COMMENT |
| MESSAGE | VARCHAR2(500) | 알림 메시지 |
| IS_READ | NUMBER(1) | 읽음 여부 |
| CREATED_AT | TIMESTAMP | 생성일 |

---

## 🚀 시작하기

### 사전 요구사항
- Java 21
- Oracle DB (또는 로컬 XE)
- Gmail 계정 + 앱 비밀번호
- Docker (선택)

### 로컬 실행

```bash
# 1. 저장소 클론
git clone https://github.com/<your-org>/Web01.git
cd Web01

# 2. 시크릿 파일 생성 (Git에 커밋되지 않음)
cat > src/main/resources/application-secret.properties << EOF
spring.datasource.password=<DB_PASSWORD>
spring.mail.password=<GMAIL_APP_PASSWORD>
EOF

# 3. IntelliJ Run Configuration → Environment variables 설정
#    APP_UPLOAD_PATH=C:/NAS Uploads        (Windows — 공백 포함 경로 지원)
#    APP_UPLOAD_URL_PREFIX=/uploads

# 4. 로컬 프로파일로 실행 (SQL 로그 활성화)
./gradlew bootRun --args='--spring.profiles.active=local'
```

서버 기동 후 http://localhost:8080 으로 접속합니다.

### Docker 빌드

```bash
docker build -t web01:latest .
docker run -p 8080:8080 \
  -e APP_UPLOAD_PATH=/app/uploads \
  -e APP_UPLOAD_URL_PREFIX=/uploads \
  -v /volume1/docker/spring/uploads:/app/uploads \
  -v /volume1/docker/spring/secret/application-secret.properties:/app/config/application-secret.properties \
  web01:latest
```

### 자주 쓰는 커맨드

```bash
# NAS SSH 접속
ssh royalstyles_Admin@100.100.109.24

# NAS Docker Compose 재배포
cd /volume1/docker/spring
docker compose pull && docker compose up -d

# Spring Boot 로그 확인
docker logs -f web01

# Oracle DB 접속
docker exec -it oracle-19c bash
sqlplus 'springuser/<비밀번호>@ORCLPDB1'

# 컨테이너 리소스 확인
docker stats oracle-19c
```

---

## ⚙️ 환경 설정 & 시크릿 관리

### application.properties (공통 — Git 포함)

```properties
# Oracle DB
spring.datasource.url=jdbc:oracle:thin:@//<HOST>:<PORT>/ORCLPDB1
spring.datasource.username=springuser

# Flyway
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=false
spring.flyway.baseline-version=9
spring.flyway.validate-migration-naming=true

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.open-in-view=false

# 파일 업로드 크기 제한
spring.servlet.multipart.max-file-size=500MB
spring.servlet.multipart.max-request-size=500MB

# 업로드 경로 (Docker 배포 기본값)
app.upload.path=/app/uploads
app.upload.url-prefix=/uploads

# Gmail SMTP
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=<Gmail 주소>
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Nginx 리버스 프록시
server.forward-headers-strategy=framework
server.port=8080

# 시크릿 파일 import
spring.config.import=optional:classpath:application-secret.properties
```

### application-secret.properties (🔒 Git 제외)

```properties
spring.datasource.password=<DB_PASSWORD>
spring.mail.password=<GMAIL_APP_PASSWORD>
```

### application-local.properties (로컬 개발용)

```properties
# SQL 디버깅
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.orm.jdbc.bind=TRACE
spring.jpa.properties.hibernate.format_sql=true

# 로컬 업로드 경로 (환경변수로 주입)
app.upload.path=${APP_UPLOAD_PATH}
app.upload.url-prefix=${APP_UPLOAD_URL_PREFIX}
```

> ⚠️ Gmail 앱 비밀번호는 Google 계정 → 보안 → 2단계 인증 활성화 후 발급하세요.

---

## 🔄 CI/CD 파이프라인

GitHub Actions (`.github/workflows/deploy.yml`) — `master` 브랜치 push 시 자동 배포.

```
master push
  └─▶ Tailscale VPN 연결
  └─▶ Docker 이미지 빌드 → Docker Hub push
        (royalstyles/web01:latest)
  └─▶ SSH → NAS 접속
        ├─▶ GitHub Secrets → application-secret.properties 생성
        │     (/volume1/docker/spring/secret/)
        └─▶ docker compose pull && docker compose up -d
```

### 필요한 GitHub Secrets

| Secret | 설명 |
|--------|------|
| `TAILSCALE_AUTHKEY` | Tailscale 인증 키 |
| `DOCKER_USERNAME` | Docker Hub 사용자명 |
| `DOCKER_PASSWORD` | Docker Hub 비밀번호 |
| `NAS_HOST` | NAS Tailscale IP |
| `NAS_USER` | NAS SSH 사용자 |
| `NAS_SSH_KEY` | NAS SSH 개인키 |
| `DB_PASSWORD` | Oracle DB 비밀번호 |
| `MAIL_PASSWORD` | Gmail 앱 비밀번호 |

---

## 🔐 보안 정책

- 게시판 목록·상세는 비로그인도 읽기 가능, 작성/수정/삭제/댓글/좋아요는 인증 필수
- `/admin/**`은 `ROLE_ADMIN` 전용
- CSRF 기본 활성화 (로그아웃 포함 모든 POST에 토큰 사용)
- 비밀번호 정책: 영문 + 숫자 + 특수문자, 8자 이상
- 로그인 5회 실패 시 10분 잠금
- 이메일 미인증 계정 로그인 차단 (`DisabledException` → `?unverified=true`)
- **프로필 접근 시 비밀번호 재인증** (10분 세션 유효, 만료 시 재인증 요구)
- DB 비밀번호 · 메일 비밀번호는 `application-secret.properties`로 분리 (Git 제외)
- 파일 업로드: MIME 타입 화이트리스트 검증, 크기 제한 (이미지 20MB / 동영상 500MB)
- UUID 기반 저장 파일명으로 경로 추측 공격 방지
- 아이디 변경 시 기존 세션 강제 무효화 (재로그인 유도)
- 관리자 자기 자신 권한 변경 및 삭제 방지
- 동영상은 `<video>` 태그로 렌더링 (iframe 미사용 — `X-Frame-Options: DENY` 정책 준수)

---

## 📝 라이선스

본 프로젝트는 개인/학습 목적으로 작성되었습니다.
