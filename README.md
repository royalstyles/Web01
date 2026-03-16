# Web01 — Spring Boot 웹 애플리케이션

Spring Boot 3 기반의 회원 인증·관리 웹 애플리케이션입니다.  
Oracle DB 연동, Spring Security 로그인 보호, GitHub Actions CI/CD, Docker 컨테이너 배포까지 포함한 풀스택 구성입니다.

---

## 📋 목차

- [기술 스택](#기술-스택)
- [프로젝트 구조](#프로젝트-구조)
- [주요 기능](#주요-기능)
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

---

## 📁 프로젝트 구조

```
src/main/java/com/jhpj/Web01/
├── Web01Application.java          # 애플리케이션 진입점
├── IndexController.java           # 루트 엔드포인트
├── config/
│   ├── SecurityConfig.java        # Spring Security 설정
│   └── PasswordEncoderConfig.java # BCrypt 인코더 빈 등록
├── controller/
│   ├── AuthController.java        # 로그인 / 회원가입 처리
│   └── HomeController.java        # 홈 / 관리자 페이지
├── entity/
│   └── User.java                  # 회원 엔티티 (UserDetails 구현)
├── repository/
│   └── UserRepository.java        # JPA 리포지토리
├── service/
│   ├── CustomUserDetailsService.java  # 인증 서비스 + 회원가입
│   └── LoginAttemptService.java       # 로그인 실패 횟수 관리
└── util/
    └── PasswordValidator.java         # 비밀번호 정책 검증

src/main/resources/
├── application.properties         # DB, JPA, 서버 설정
└── templates/
    ├── home.html                  # 메인 홈 화면
    ├── admin.html                 # 관리자 회원 목록
    └── auth/
        ├── login.html             # 로그인 페이지
        └── signup.html            # 회원가입 페이지
```

---

## ✅ 주요 기능

### 회원 인증
- 커스텀 로그인 페이지 (`/auth/login`)
- BCrypt 비밀번호 암호화
- 로그인 성공 시 `/home` 리다이렉트, 실패 시 에러 메시지 표시
- 로그아웃 시 세션 및 쿠키 완전 삭제

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

## 🚀 시작하기

### 사전 요구사항
- Java 21
- Oracle DB (또는 로컬 XE)
- Docker (선택)

### 로컬 실행

```bash
# 1. 저장소 클론
git clone https://github.com/<your-org>/Web01.git
cd Web01

# 2. application.properties DB 설정 수정
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

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.database-platform=org.hibernate.dialect.OracleDialect

# 서버 포트
server.port=8080

# Nginx 리버스 프록시 사용 시
server.forward-headers-strategy=framework
```

> ⚠️ `application.properties`는 `.gitignore`에 추가하거나 환경변수로 분리하는 것을 권장합니다.

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
- 세션 쿠키(`JSESSIONID`) 로그아웃 시 삭제

---

## 📝 라이선스

본 프로젝트는 개인/학습 목적으로 작성되었습니다.
