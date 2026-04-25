## 📊 분석 결과 요약

### 🔍 감지된 스택
- **런타임**: Java 17 (JRE)
- **프레임워크**: Spring Boot 4.0.5 with Spring Security, JPA, WebMVC
- **컨테이너**: Dockerfile 존재 (Multi-stage build: gradle:8.14-jdk17 → eclipse-temurin:17-jre)
- **노출 포트**: 8080

### ☁️ 필요 AWS 리소스

| 리소스 | 용도 | 필수 여부 |
|--------|------|-----------|
| EKS | 컨테이너 오케스트레이션 | ✅ 필수 |
| ECR | 컨테이너 이미지 레지스트리 | ✅ 필수 |
| RDS (MySQL) | 데이터베이스 (MySQL 8.0) | ✅ 필수 |
| ALB | 외부 트래픽 로드밸런싱 | ✅ 필수 |
| CloudWatch Logs | 로그 수집 및 모니터링 | ✅ 필수 |
| IAM Role + ServiceAccount | IRSA를 통한 Pod 권한 관리 | ✅ 필수 |
| Secrets Manager | 데이터베이스 패스워드, JWT 시크릿 관리 | 🔶 권장 |
| ACM | TLS 인증서 관리 | 🔶 권장 |
| Route53 | DNS 관리 | 🔶 선택사항 |

### 💾 리소스 추정

| 항목 | Request | Limit | 근거 |
|------|---------|-------|------|
| CPU | 250m | 500m | Spring Boot 표준 애플리케이션 |
| Memory | 512Mi | 1Gi | Spring Boot + JPA + Security 스택 |

### 🔐 IAM 권한

필요한 IAM Action 목록 (상세 내용은 Artifact의 `iam-policy.json` 참조):
- **ECR**: 컨테이너 이미지 pull 권한
- **CloudWatch Logs**: 로그 스트림 생성 및 로그 전송
- **Secrets Manager**: 애플리케이션 시크릿 조회
- **RDS**: 데이터베이스 연결 권한 (IAM 인증 사용 시)

### 📋 다음 단계

팀에서 채워야 할 항목:
- [ ] `<NAMESPACE>` — EKS 네임스페이스 지정
- [ ] `<ACCOUNT_ID>` — AWS 계정 ID
- [ ] `<IAM_ROLE_NAME>` — IRSA 역할 생성 후 ARN 기입
- [ ] `SPRING_DATASOURCE_URL` — RDS 엔드포인트 포함 JDBC URL
- [ ] `SPRING_DATASOURCE_USERNAME` — RDS 사용자명
- [ ] 시크릿 값들 — `db-password`, `jwt-secret` Base64 인코딩 후 설정
- [ ] `<YOUR_DOMAIN>` — 서비스 도메인
- [ ] Health check 경로 확인 (`/actuator/health` - Spring Boot Actuator 사용 여부 확인 필요)

### ⚠️ 주의사항

- **Health Check**: 현재 Spring Boot Actuator dependency가 build.gradle에 없으므로 `/actuator/health` 경로를 사용할 수 없습니다. 애플리케이션에 맞는 health check 엔드포인트를 구현하거나 Actuator 의존성 추가가 필요합니다.
- **MySQL**: 현재 `mysql-connector-j`를 사용중이므로 RDS MySQL 8.0 호환성 확인이 필요합니다.
- **JWT Secret**: 현재 기본값이 하드코딩되어 있으므로 프로덕션에서는 반드시 Secrets Manager 또는 환경변수로 관리해야 합니다.

> ⚠️ `iam-policy.json` 및 `k8s-manifest.md`는 Action Artifacts에서 다운로드하세요.
> 민감한 정보(계정 ID, 실제 도메인, 시크릿 등)는 직접 기입하지 말고 Secrets Manager 또는 팀 내부 문서를 활용하세요.