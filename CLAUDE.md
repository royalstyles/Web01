# 전역 개발 설정 (Global Claude Instructions)

## 기본 환경
- 주력 언어: Java (Spring Boot, Android)
- 응답 언어: 항상 한국어
- 코드 내 주석: 한국어
- 주석은 최대한 상세하게

---

## Java / Spring Boot 코딩 스타일

- Java 21 기준
- Lombok 적극 활용 (`@RequiredArgsConstructor`, `@Slf4j`, `@Builder` 등)
- `@Service` 레이어에 `@Transactional` 명시
- 예외: 명시적 커스텀 예외 또는 `IllegalArgumentException` / `IllegalStateException` 사용
- Stream API / Optional 적극 활용
- 의미 있는 변수명 우선
- 응답 형식: 변경이 필요한 부분만 발췌 제시 (전체 파일 재출력 지양)

### Spring Boot 선호 패턴
- Spring Boot 3.x / Spring Security 6.x 기준
- JPA + Repository 패턴
- DTO / Entity 분리 원칙
- 설정 파일: `application.properties` 기반 (yml 비선호)
- DDL: Flyway 마이그레이션 관리 (`ddl-auto=validate`)
- 시크릿 분리: `application-secret.properties` (Git 제외)

---

## Android 개발 스타일

- Java 기반 (Kotlin 비선호)
- Firebase (Firestore, Auth, Storage) + Anthropic Claude API
- Material Design 3 UI 컴포넌트 활용
- RecyclerView + Adapter 패턴

---

## 홈랩 인프라 컨텍스트

| 항목 | 내용 |
|------|------|
| NAS | Ugreen DXP4800 PLUS (UGOS Pro) |
| 컨테이너 | Docker Compose |
| CI/CD | GitHub Actions → Docker Hub → SSH 배포 |
| VPN | Tailscale |
| 외부 접근 | Cloudflare Tunnel |
| Oracle DB | 19c, PDB: ORCLPDB1, user: springuser |
| PostgreSQL | 17 |

---

## 코드 응답 원칙

1. **발췌 우선**: 변경 부분만 제시, 전체 파일 재출력 지양
2. **빌드/배포 영향 경고**: 의존성 추가, DB 스키마 변경, 보안 설정 변경 시 반드시 명시
3. **검증 코드 포함**: 중요한 로직 변경 시 테스트 힌트 또는 확인 방법 안내
4. **단계별 설명**: 복잡한 구현은 순서대로 설명

---

## 자주 쓰는 커맨드 참고

### NAS Docker Compose 재배포
ssh royalstyles_Admin@100.100.109.24

### 메모리 현황
free -h

### Oracle 메모리 제한 확인
docker inspect oracle-19c | grep -i memory

```bash
# NAS Docker Compose 접근
cd /volume1/docker/spring
cd /volume1/docker/cloudflared

# Spring Boot 로컬 실행
./gradlew bootRun --args='--spring.profiles.active=local'

# Docker 빌드
docker build -t web01:latest .

# NAS Docker Compose 정지
docker compose down

# NAS Docker Compose 재배포
docker compose pull && docker compose up -d

# NAS Docker ORACLE19 DB 접속
docker exec -it oracle-19c bash
sqlplus 'springuser/비밀번호@ORCLPDB1'

# 컨테이너 재시작
docker restart oracle-19c

# 컨테이너 리소스 사용량 확인 (메모리/CPU)
docker stats oracle-19c
```