# Web01 — Spring Boot 웹 애플리케이션

Spring Boot 3 기반의 회원 인증·관리 웹 애플리케이션입니다.  
Oracle DB + Flyway 마이그레이션, Spring Security 로그인 보호, 이메일 인증, 관리자 대시보드, 프로필 관리, GitHub Actions CI/CD, Docker 컨테이너 배포까지 포함한 풀스택 구성입니다.

---

## 📋 목차

- [기술 스택](#기술-스택)
- [프로젝트 구조](#프로젝트-구조)
- [주요 기능](#주요-기능)
- [회원가입 & 이메일 인증 흐름](#회원가입--이메일-인증-흐름)
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
├── config/
│   ├── SecurityConfig.java                # Spring Security 설정
│   └── PasswordEncoderConfig.java         # BCrypt 인코더 빈 등록
├── controller/
│   ├── AuthController.java                # 로그인 / 회원가입 / 이메일 인증
│   ├── HomeController.java                # 홈 페이지
│   ├── AdminController.java               # 관리자 대시보드
│   └── ProfileController.java             # 프로필 관리
├── entity/
│   ├── User.java                          # 회원 엔티티 (UserDetails 구현)
│   ├── EmailVerificationToken.java        # 회원가입 이메일 인증 토큰
│   └── EmailChangeToken.java             # 이메일 변경 인증 토큰
├── repository/
│   ├── UserRepository.java
│   ├── EmailVerificationTokenRepository.java
│   └── EmailChangeTokenRepository.java
├── service/
│   ├── CustomUserDetailsService.java      # 인증 서비스 + 회원가입
│   ├── EmailService.java                  # 이메일 발송 (인증 / 변경)
│   ├── AdminService.java                  # 관리자 회원 관리 서비스
│   ├── ProfileService.java                # 프로필 변경 서비스
│   └── LoginAttemptService.java           # 로그인 실패 횟수 관리
└── util/
    └── PasswordValidator.java             # 비밀번호 정책 검증

src/main/resources/
├── application.properties                 # 공통 설정 (DB URL, 메일, Flyway 등)
├── application-local.properties           # 로컬 개발용 프로파일 (SQL 로그 등)
├── application-secret.properties          # 🔒 시크릿 (Git 제외, 배포 시 주입)
├── db/migration/
│   ├── V1__init_users.sql                 # USERS 테이블 + 시퀀스
│   ├── V2__email_verification_tokens.sql  # 이메일 인증 토큰 테이블
│   └── V3__email_change_tokens.sql        # 이메일 변경 토큰 테이블
├── static/
│   └── favicon.svg                        # SVG 파비콘
└── templates/
    ├── home.html                          # 홈 화면
    ├── admin.html                         # 관리자 대시보드
    ├── profile.html                       # 프로필 수정
    ├── profile-verify-result.html         # 이메일 변경 인증 결과
    └── auth/
        ├── login.html
        ├── signup.html
        └── verify-result.html             # 회원가입 이메일 인증 결과
```

---

## ✅ 주요 기능

### 회원 인증
- 커스텀 로그인 페이지 (`/auth/login`)
- BCrypt 비밀번호 암호화
- 로그인 성공 시 `/home` 리다이렉트, 실패 시 에러 메시지 표시
- 로그아웃 시 세션 및 쿠키 완전 삭제 (CSRF 토큰 방식 POST 처리)

### 이메일 인증
- 회원가입 시 즉시 인증 메일 발송 (Gmail SMTP)
- 인증 전 계정은 `isEnabled() = false` → 로그인 차단 (`?unverified=true` 안내)
- 인증 링크 유효시간: **24시간** (만료 시 재가입 필요)
- `EmailVerificationToken` 엔티티로 UUID 토큰 DB 관리
- 인증 완료 시 `emailVerified = true` 업데이트 후 토큰 삭제

### 회원가입
- 아이디 / 이메일 중복 검사
- 비밀번호 정책: **8자 이상, 영문 + 숫자 + 특수문자** 조합 필수
- 비밀번호 확인 일치 검증

### 로그인 보호 (Brute-Force 방지)
- 로그인 5회 실패 시 **10분 계정 잠금**
- 잠금 해제까지 남은 시간(초) 안내
- 인메모리 `ConcurrentHashMap` 기반 (재시작 시 초기화)

### 권한 관리
| 권한 | 접근 가능 경로 |
|------|----------------|
| `ROLE_USER` | `/home`, `/profile`, `/auth/**` |
| `ROLE_ADMIN` | `/home`, `/profile`, `/admin/**`, `/auth/**` |

### 관리자 대시보드 (`/admin`) — 신규
- **통계 카드**: 전체 회원 수 / 관리자 수 / 이메일 미인증 수 / 잠금 계정 수
- **회원 테이블**: 아이디, 이메일, 권한, 인증 상태, 가입일, 잠금 여부 표시
- **관리 기능**
    - 권한 변경: `ROLE_USER` ↔ `ROLE_ADMIN` 토글
    - 계정 잠금 해제: 브루트포스로 잠긴 계정 즉시 해제
    - 이메일 인증 강제 완료: 미인증 회원 수동 승인
    - 회원 삭제: 삭제 확인 후 처리 (자기 자신 보호)
- 자기 자신의 권한 변경 및 삭제 방지

### 프로필 관리 (`/profile`) — 신규
- **아이디 변경**: 중복 검사 후 변경, 완료 시 자동 로그아웃 (세션 재발급)
- **비밀번호 변경**: 현재 비밀번호 확인 → 새 비밀번호 정책 검증 → 변경
    - 실시간 비밀번호 강도 표시 바 (매우 약함 / 약함 / 보통 / 강함)
- **이메일 변경**: 새 이메일로 인증 링크 발송 → 클릭 시 변경 완료
    - `EmailChangeToken` 엔티티로 24시간 유효 토큰 관리
    - 인증 완료 직전 이메일 중복 재확인 (Race Condition 방지)

---

## 📧 회원가입 & 이메일 인증 흐름

```
[사용자] 회원가입 폼 제출
    │
    ▼
[서버] 유효성 검사 (아이디·이메일 중복, 비밀번호 정책)
    │
    ▼
[서버] USERS 테이블에 저장 (emailVerified = false)
    │
    ▼
[서버] UUID 토큰 생성 → EMAIL_VERIFICATION_TOKENS 저장 (24시간 유효)
    │
    ▼
[서버] Gmail SMTP로 인증 메일 발송
    │                          │
    ▼                          ▼
[사용자] 인증 링크 클릭       [미클릭 시 로그인 시도]
    │                          │
    ▼                          ▼
[서버] 토큰 검증               [서버] isEnabled() = false
    │                               → ?unverified=true 로그인 차단
    ▼
[서버] emailVerified = true 업데이트, 토큰 삭제
    │
    ▼
[사용자] 로그인 가능
```

---

## 🗄 DB 마이그레이션 (Flyway)

JPA `ddl-auto` 대신 **Flyway**로 DDL을 관리합니다. 애플리케이션 시작 시 `db/migration/` 아래 파일을 순서대로 실행합니다.

| 버전 | 파일 | 내용 |
|------|------|------|
| V1 | `V1__init_users.sql` | USERS 테이블 + USERS_SEQ 시퀀스 |
| V2 | `V2__email_verification_tokens.sql` | EMAIL_VERIFICATION_TOKENS 테이블 + EVT_SEQ |
| V3 | `V3__email_change_tokens.sql` | EMAIL_CHANGE_TOKENS 테이블 + ECT_SEQ + 인덱스 |

```properties
# 기존 DB에 Flyway를 처음 적용할 때 baseline 설정
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=3
```

> ⚠️ 이미 테이블이 존재하는 DB에 처음 Flyway를 적용할 때는 `baseline-on-migrate=true`와 `baseline-version`을 현재 최신 버전으로 설정하세요.

### DB 테이블 구조

**USERS**
| 컬럼 | 타입 | 설명 |
|------|------|------|
| ID | NUMBER | PK (USERS_SEQ) |
| USERNAME | VARCHAR2(50) | 아이디 (UNIQUE) |
| PASSWORD | VARCHAR2(255) | BCrypt 암호화 비밀번호 |
| EMAIL | VARCHAR2(100) | 이메일 (UNIQUE) |
| ROLE | VARCHAR2(20) | ROLE_USER / ROLE_ADMIN |
| EMAIL_VERIFIED | NUMBER(1) | 이메일 인증 여부 (0/1) |
| CREATED_AT | TIMESTAMP | 가입일시 |

**EMAIL_VERIFICATION_TOKENS**
| 컬럼 | 타입 | 설명 |
|------|------|------|
| ID | NUMBER | PK (EVT_SEQ) |
| TOKEN | VARCHAR2(255) | UUID 토큰 (UNIQUE) |
| USER_ID | NUMBER | FK → USERS.ID |
| EXPIRES_AT | TIMESTAMP | 만료일시 (가입 후 24시간) |

**EMAIL_CHANGE_TOKENS**
| 컬럼 | 타입 | 설명 |
|------|------|------|
| ID | NUMBER | PK (ECT_SEQ) |
| TOKEN | VARCHAR2(255) | UUID 토큰 (UNIQUE) |
| USER_ID | NUMBER | FK → USERS.ID |
| NEW_EMAIL | VARCHAR2(100) | 변경하려는 새 이메일 |
| EXPIRES_AT | TIMESTAMP | 만료일시 (요청 후 24시간) |

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

# 3. 빌드 및 실행 (로컬 프로파일 활성화 시 SQL 로그 출력)
./gradlew bootRun --args='--spring.profiles.active=local'
```

서버 기동 후 http://localhost:8080/auth/login 으로 접속합니다.

### Docker 빌드

```bash
docker build -t web01:latest .
docker run -p 8080:8080 \
  -v /path/to/secret/application-secret.properties:/app/config/application-secret.properties \
  web01:latest
```

---

## ⚙️ 환경 설정 & 시크릿 관리

### application.properties (공통 — Git 포함)

```properties
# Oracle DB (비밀번호는 secret으로 분리)
spring.datasource.url=jdbc:oracle:thin:@//<HOST>:<PORT>/<SERVICE_NAME>
spring.datasource.username=<DB_USER>

# Flyway
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=3

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.open-in-view=false

# Gmail SMTP (비밀번호는 secret으로 분리)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=<Gmail 주소>
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# 서버 포트
server.port=8080

# Nginx 리버스 프록시 사용 시
server.forward-headers-strategy=framework

# 시크릿 파일 import
spring.config.import=optional:classpath:application-secret.properties
```

### application-secret.properties (🔒 Git 제외 — `.gitignore` 등록됨)

```properties
spring.datasource.password=<DB_PASSWORD>
spring.mail.password=<GMAIL_APP_PASSWORD>
```

> ⚠️ Gmail 앱 비밀번호는 Google 계정 → 보안 → 2단계 인증 활성화 후 발급하세요.  
> ⚠️ 이 파일은 절대 Git에 커밋하지 마세요. CI/CD에서 GitHub Secrets를 통해 NAS에 직접 생성됩니다.

### application-local.properties (로컬 개발용 프로파일)

```properties
# SQL 디버깅
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.orm.jdbc.bind=TRACE
spring.jpa.properties.hibernate.format_sql=true
```

---

## 🔄 CI/CD 파이프라인

GitHub Actions (`.github/workflows/deploy.yml`) 를 통해 `master` 브랜치 push 시 자동 배포됩니다.

```
master push
  └─▶ Tailscale VPN 연결
  └─▶ Docker 이미지 빌드 → Docker Hub push
        (royalstyles/web01:latest)
  └─▶ SSH → NAS 접속
        ├─▶ GitHub Secrets로 application-secret.properties 생성
        │     (/volume1/docker/spring/secret/)
        └─▶ docker compose pull
            docker compose up -d
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

- 모든 페이지는 로그인 필요 (`anyRequest().authenticated()`)
- `/auth/**`, `/css/**`, `/js/**` 만 비인증 접근 허용
- `/admin/**` 은 `ROLE_ADMIN` 전용
- CSRF 기본 활성화 (로그아웃 포함 모든 POST에 토큰 사용)
- 비밀번호 정책: 영문 + 숫자 + 특수문자, 8자 이상
- 로그인 5회 실패 시 10분 잠금
- 이메일 미인증 계정 로그인 차단 (`DisabledException` → `?unverified=true`)
- 세션 쿠키(`JSESSIONID`) 로그아웃 시 삭제
- DB 비밀번호 및 메일 비밀번호는 `application-secret.properties`로 분리 관리 (Git 제외)
- 아이디 변경 시 기존 세션 강제 무효화 (재로그인 유도)
- 운영체제마다 파일 업로드 폴더 구분되서 적용되도록 환경변수 적용
``` 
  # IntelliJ Run Configuration → Environment variables에 입력:
      Windows: APP_UPLOAD_PATH=C:/NAS Uploads
      Windows: APP_UPLOAD_URL_PREFIX=/NAS Uploads
      
      Mac: APP_UPLOAD_PATH=/Users/junghokim/NAS Uploads
      Mac: APP_UPLOAD_URL_PREFIX=/NAS Uploads
```

## 📝 라이선스

본 프로젝트는 개인/학습 목적으로 작성되었습니다.