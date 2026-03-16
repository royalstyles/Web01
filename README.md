# Web01 — Spring Boot 웹 애플리케이션

Spring Boot 3 기반의 회원 인증·관리 웹 애플리케이션입니다.  
Oracle DB 연동, Spring Security 로그인 보호, 이메일 인증, GitHub Actions CI/CD, Docker 컨테이너 배포까지 포함한 풀스택 구성입니다.

---

## 📋 목차

- [기술 스택](#기술-스택)
- [프로젝트 구조](#프로젝트-구조)
- [주요 기능](#주요-기능)
- [회원가입 & 이메일 인증 흐름](#회원가입--이메일-인증-흐름)
- [시작하기](#시작하기)
- [환경 설정](#환경-설정)
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
├── Web01Application.java              # 애플리케이션 진입점
├── IndexController.java               # 루트 엔드포인트
├── config/
│   ├── SecurityConfig.java            # Spring Security 설정
│   └── PasswordEncoderConfig.java     # BCrypt 인코더 빈 등록
├── controller/
│   ├── AuthController.java            # 로그인 / 회원가입 / 이메일 인증 처리
│   └── HomeController.java            # 홈 / 관리자 페이지
├── entity/
│   ├── User.java                      # 회원 엔티티 (UserDetails 구현)
│   └── EmailVerificationToken.java    # 이메일 인증 토큰 엔티티
├── repository/
│   ├── UserRepository.java            # JPA 리포지토리
│   └── EmailVerificationTokenRepository.java  # 토큰 리포지토리
├── service/
│   ├── CustomUserDetailsService.java  # 인증 서비스 + 회원가입
│   ├── EmailService.java              # 인증 메일 발송 서비스
│   └── LoginAttemptService.java       # 로그인 실패 횟수 관리
└── util/
    └── PasswordValidator.java         # 비밀번호 정책 검증

src/main/resources/
├── application.properties             # DB, JPA, 메일, 서버 설정
└── templates/
    ├── home.html                      # 메인 홈 화면
    ├── admin.html                     # 관리자 회원 목록
    └── auth/
        ├── login.html                 # 로그인 페이지
        ├── signup.html                # 회원가입 페이지
        └── verify-result.html         # 이메일 인증 결과 페이지
```

---

## ✅ 주요 기능

### 회원 인증
- 커스텀 로그인 페이지 (`/auth/login`)
- BCrypt 비밀번호 암호화
- 로그인 성공 시 `/home` 리다이렉트, 실패 시 에러 메시지 표시
- 로그아웃 시 세션 및 쿠키 완전 삭제

### 이메일 인증 (신규)
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
| `ROLE_USER` | `/home`, `/auth/**` |
| `ROLE_ADMIN` | `/home`, `/admin/**`, `/auth/**` |

### 관리자 페이지
- 전체 회원 목록 조회 (`/admin`)
- 회원 ID, 아이디, 이메일, 권한, 가입일시 표시

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
[서버] emailVerified = true 업데이트
       토큰 삭제
    │
    ▼
[사용자] 로그인 가능
```

---

## 🗄 DB 테이블 구조

### USERS
| 컬럼 | 타입 | 설명 |
|------|------|------|
| ID | NUMBER | PK (USERS_SEQ) |
| USERNAME | VARCHAR2(50) | 아이디 (UNIQUE) |
| PASSWORD | VARCHAR2 | BCrypt 암호화 비밀번호 |
| EMAIL | VARCHAR2(100) | 이메일 (UNIQUE) |
| ROLE | VARCHAR2(20) | ROLE_USER / ROLE_ADMIN |
| EMAIL_VERIFIED | NUMBER(1) | 이메일 인증 여부 (0/1) |
| CREATED_AT | TIMESTAMP | 가입일시 |

### EMAIL_VERIFICATION_TOKENS
| 컬럼 | 타입 | 설명 |
|------|------|------|
| ID | NUMBER | PK (EVT_SEQ) |
| TOKEN | VARCHAR2 | UUID 토큰 (UNIQUE) |
| USER_ID | NUMBER | FK → USERS.ID |
| EXPIRES_AT | TIMESTAMP | 만료일시 (가입 후 24시간) |

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

# 2. application.properties DB 및 메일 설정 수정
# (아래 환경 설정 참고)

# 3. 빌드 및 실행
./gradlew bootRun
```

서버 기동 후 http://localhost:8080/auth/login 으로 접속합니다.

### Docker 빌드

```bash
docker build -t web01:latest .
docker run -p 8080:8080 web01:latest
```

---

## ⚙️ 환경 설정

`src/main/resources/application.properties` 를 수정합니다.

```properties
# Oracle DB 연결 정보
spring.datasource.url=jdbc:oracle:thin:@//<HOST>:<PORT>/<SERVICE_NAME>
spring.datasource.username=<DB_USER>
spring.datasource.password=<DB_PASSWORD>

# JPA (테이블이 이미 존재하면 validate, 최초 생성 시 update 사용)
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.database-platform=org.hibernate.dialect.OracleDialect

# Gmail SMTP 설정
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=<Gmail 주소>
spring.mail.password=<Gmail 앱 비밀번호>  # 구글 계정 2단계 인증 후 앱 비밀번호 발급
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# 서버 포트
server.port=8080

# Nginx 리버스 프록시 사용 시
server.forward-headers-strategy=framework
```

> ⚠️ `application.properties`는 `.gitignore`에 추가하거나 환경변수로 분리하는 것을 권장합니다.  
> ⚠️ Gmail 앱 비밀번호는 Google 계정 → 보안 → 2단계 인증 활성화 후 발급할 수 있습니다.

---

## 🔄 CI/CD 파이프라인

GitHub Actions (`.github/workflows/deploy.yml`) 를 통해 `master` 브랜치 push 시 자동 배포됩니다.

```
master push
  └─▶ Tailscale VPN 연결
  └─▶ Docker 이미지 빌드 → Docker Hub push
        (royalstyles/web01:latest)
  └─▶ SSH → NAS 접속
        └─▶ docker compose pull
        └─▶ docker compose up -d
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

---

## 🔐 보안 정책

- 모든 페이지는 로그인 필요 (`anyRequest().authenticated()`)
- `/auth/**`, `/css/**`, `/js/**` 만 비인증 접근 허용
- CSRF 기본 활성화 (Spring Security 기본값)
- 비밀번호 정책: 영문 + 숫자 + 특수문자, 8자 이상
- 로그인 5회 실패 시 10분 잠금
- 이메일 미인증 계정 로그인 차단 (`DisabledException` → `?unverified=true`)
- 세션 쿠키(`JSESSIONID`) 로그아웃 시 삭제

---

## 📝 라이선스

본 프로젝트는 개인/학습 목적으로 작성되었습니다.