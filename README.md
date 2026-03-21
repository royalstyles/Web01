# Web01 — Spring Boot 웹 애플리케이션

Spring Boot 3 기반의 커뮤니티 웹 애플리케이션입니다.  
Oracle DB + Flyway 마이그레이션, Spring Security 인증 보호, 이메일 인증, 관리자 대시보드, 프로필 관리, **Quill.js 리치 텍스트 게시판** (이미지·동영상 업로드, 댓글, 좋아요), GitHub Actions CI/CD, Docker 컨테이너 배포까지 포함한 풀스택 구성입니다.

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
│   └── WebMvcConfig.java                  # 정적 파일 핸들러 + 스케줄러 활성화
├── controller/
│   ├── AuthController.java                # 로그인 / 회원가입 / 이메일 인증
│   ├── HomeController.java                # 홈 (게시판 목록 통합)
│   ├── BoardController.java               # 게시판 CRUD + 댓글 + 좋아요
│   ├── FileUploadController.java          # 이미지 · 동영상 업로드 API
│   ├── AdminController.java               # 관리자 대시보드
│   └── ProfileController.java             # 프로필 관리 + 이미지 업로드
├── entity/
│   ├── User.java                          # 회원 엔티티 (UserDetails 구현)
│   ├── Post.java                          # 게시글 엔티티
│   ├── Comment.java                       # 댓글 엔티티
│   ├── Category.java                      # 게시판 카테고리 엔티티
│   ├── PostLike.java                      # 좋아요 엔티티 (복합 UNIQUE)
│   ├── PostFile.java                      # 파일 엔티티 (이미지/동영상)
│   ├── EmailVerificationToken.java        # 회원가입 이메일 인증 토큰
│   └── EmailChangeToken.java              # 이메일 변경 인증 토큰
├── repository/
│   ├── UserRepository.java
│   ├── PostRepository.java                # fetch join 쿼리 포함
│   ├── CommentRepository.java
│   ├── CategoryRepository.java
│   ├── PostLikeRepository.java
│   ├── PostFileRepository.java            # 고아 파일 조회 쿼리 포함
│   ├── EmailVerificationTokenRepository.java
│   └── EmailChangeTokenRepository.java
├── service/
│   ├── CustomUserDetailsService.java      # 인증 + 회원가입
│   ├── BoardService.java                  # 게시글/댓글/좋아요/파일 연결 로직
│   ├── FileService.java                   # 파일 업로드 · 삭제 · 고아 정리 스케줄러
│   ├── EmailService.java                  # 이메일 발송 (인증 / 변경)
│   ├── AdminService.java                  # 관리자 회원 관리 서비스
│   ├── ProfileService.java                # 프로필 변경 + 이미지 서비스
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
│   └── V9__categories_sort_order_update.sql
├── static/
│   └── favicon.svg
└── templates/
    ├── home.html                          # 홈 (게시판 통합, 로그인/비로그인 분기)
    ├── admin.html                         # 관리자 대시보드
    ├── profile.html                       # 프로필 수정
    ├── profile-verify-result.html
    ├── auth/
    │   ├── login.html
    │   ├── signup.html
    │   └── verify-result.html
    └── board/
        ├── board-list.html                # 게시판 목록
        ├── board-view.html                # 게시글 상세 (댓글, 좋아요 AJAX)
        └── board-write.html               # 게시글 작성/수정 (Quill.js 에디터)
```

---

## ✅ 주요 기능

### 회원 인증
- 커스텀 로그인 페이지 (`/auth/login`)
- BCrypt 비밀번호 암호화
- 로그아웃 시 세션 및 쿠키 완전 삭제 (CSRF 토큰 방식 POST)

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
| `ROLE_USER` | 비로그인 + 게시글 작성/수정/삭제, 댓글, 좋아요, `/profile/**` |
| `ROLE_ADMIN` | 전체 + `/admin/**` |

### 게시판 — 신규
- **목록/검색**: 카테고리 필터 + 제목 키워드 검색, 10건 페이징
- **Quill.js 리치 텍스트 에디터**: 폰트, 색상, 표, 코드블록, 이미지, 동영상 지원
- **이미지 업로드**: 툴바 클릭 → 서버 저장 → URL 삽입 (최대 20MB)
- **동영상 업로드**: 별도 버튼 → 서버 저장 → 에디터 삽입 (최대 500MB)
- **댓글**: AJAX 비동기 등록/수정/삭제 (본인 + 관리자 수정·삭제 가능)
- **좋아요**: AJAX 토글, Oracle 트리거로 `POSTS.LIKE_COUNT` 자동 동기화
- **조회수**: 상세 페이지 접근 시 자동 증가
- **수정/삭제**: 작성자 또는 관리자만 가능
- **비로그인 공개**: 목록 및 상세 읽기 가능, 작성/댓글/좋아요는 로그인 필요

### 파일 업로드 — 신규
- 저장 경로: `APP_UPLOAD_PATH` 환경변수 지정 (OS별 분리)
  - Docker 배포: `/app/uploads/`
  - 로컬 Windows: `C:/NAS Uploads/`
  - 로컬 Mac: `/Users/junghokim/NAS Uploads/`
- 하위 디렉토리: `images/`, `videos/`, `profiles/`
- UUID 기반 저장 파일명으로 중복 방지
- **고아 파일 자동 정리 스케줄러**: 매 1시간마다 게시글과 연결되지 않은 임시 파일 삭제 (프로필 이미지 제외)
- 파일↔게시글 연결: 임시 업로드(post=null) → 게시글 저장 시 `attachFilesToPost()`로 연결

### 관리자 대시보드 (`/admin`)
- 통계 카드: 전체 회원 / 관리자 / 이메일 미인증 / 잠금 계정
- 권한 변경 (`ROLE_USER` ↔ `ROLE_ADMIN`), 계정 잠금 해제, 이메일 인증 강제 완료, 회원 삭제
- 자기 자신 권한 변경 및 삭제 방지

### 프로필 관리 (`/profile`)
- **프로필 이미지**: 카메라 아이콘 클릭 → 즉시 업로드, 삭제 가능
- **아이디 변경**: 중복 검사 후 변경 → 자동 로그아웃 (세션 무효화)
- **비밀번호 변경**: 현재 비밀번호 확인 + 실시간 강도 표시 바
- **이메일 변경**: 새 이메일로 인증 링크 발송 → 클릭 후 변경 완료 (Race Condition 방지)

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
[서버] FileService.upload() → /app/uploads/{images|videos}/UUID.ext 저장
       PostFile(post=null) DB 저장 (임시 상태)
    │
    ▼
[응답] { url: "/uploads/images/UUID.jpg" } → Quill 에디터에 URL 삽입
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
#    Windows:
#      APP_UPLOAD_PATH=C:/NAS Uploads
#      APP_UPLOAD_URL_PREFIX=/uploads
#    Mac:
#      APP_UPLOAD_PATH=/Users/junghokim/NAS Uploads
#      APP_UPLOAD_URL_PREFIX=/uploads

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
- DB 비밀번호 · 메일 비밀번호는 `application-secret.properties`로 분리 (Git 제외)
- 파일 업로드: MIME 타입 화이트리스트 검증, 크기 제한 (이미지 20MB / 동영상 500MB)
- UUID 기반 저장 파일명으로 경로 추측 공격 방지
- 아이디 변경 시 기존 세션 강제 무효화 (재로그인 유도)
- 관리자 자기 자신 권한 변경 및 삭제 방지

---

## 📝 라이선스

본 프로젝트는 개인/학습 목적으로 작성되었습니다.