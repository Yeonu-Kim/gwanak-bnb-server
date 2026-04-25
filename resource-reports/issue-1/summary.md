## 📊 분석 결과 요약

### 🔍 감지된 스택
- **런타임**: Java 17 (eclipse-temurin:17-jre)
- **프레임워크**: Spring Boot 4.0.5 (Web MVC, JPA, Security)
- **빌드 도구**: Gradle 8.14
- **컨테이너**: Multi-stage Docker build (gradle:8.14-jdk17 → eclipse-temurin:17-jre)
- **노출 포트**: 8080
- **데이터베이스**: MySQL 8.0 (mysql-connector-j 사용)

### ☁️ 필요 AWS 리소스

| 리소스 | 용도 | 필수 여부 |
|--------|------|-----------|
| EKS | 컨테이너 오케스트레이션 | ✅ 필수 |
| ECR | 컨테이너 이미지 레지스트리 | ✅ 필수 |
| RDS MySQL | 데이터베이스 (MySQL 8.0) | ✅ 필수 |
| IAM Role + ServiceAccount | IRSA 기반 Pod 권한 관리 | ✅ 필수 |
| CloudWatch Logs | 로그 수집 및 모니터링 | ✅ 필수 |
| ALB | 외부 트래픽 로드밸런싱 | ✅ 필수 |
| Secrets Manager | 데이터베이스 자격 증명 관리 | ✅ 권장 |
| ACM | SSL/TLS 인증서 관리 | ✅ 권장 |
| Route53 | DNS 관리 | ⚪ 선택 |

### 💾 리소스 추정

| 항목 | Request | Limit | 근거 |
|------|---------|-------|------|
| CPU | 250m | 500m | Spring Boot 표준 웹 애플리케이션 |
| Memory | 512Mi | 1Gi | JVM 기반, JPA + Security 포함 |

### 🔐 IAM 권한

필요한 IAM Action 목록 (상세 내용은 Artifact의 `iam-policy.json` 참조):
- **CloudWatch Logs**: 로그 생성/전송 권한
  - `logs:CreateLogGroup`, `logs:CreateLogStream`, `logs:PutLogEvents`
- **Secrets Manager**: 데이터베이스 자격 증명 조회
  - `secretsmanager:GetSecretValue`

### 📋 다음 단계

팀에서 채워야 할 항목:
- [ ] `<NAMESPACE>` — EKS 네임스페이스 지정 (예: gwanak-bnb-prod)
- [ ] `<ACCOUNT_ID>` — AWS 계정 ID (12자리 숫자)
- [ ] `<IAM_ROLE_NAME>` — IRSA 역할 생성 후 ARN 기입
- [ ] `<YOUR_DOMAIN>` — 서비스 도메인 (예: api.gwanak-bnb.com)
- [ ] `<CERTIFICATE_ID>` — ACM 인증서 ID
- [ ] 환경변수 값 설정:
  - `SPRING_DATASOURCE_URL` — RDS MySQL 엔드포인트
  - `DB_NAME` — 데이터베이스 이름
- [ ] **RDS MySQL 설정**: VPC 보안그룹에서 EKS 노드그룹 접근 허용
- [ ] **Spring Boot Actuator 활성화**: Health check 엔드포인트를 위해 `spring-boot-starter-actuator` 의존성 추가 권장

### ⚠️ 주의사항

1. **Health Check**: 현재 manifest는 `/actuator/health` 엔드포인트를 가정합니다. Spring Boot Actuator가 설정되지 않은 경우 다른 헬스체크 경로로 변경하거나 Actuator 의존성을 추가해야 합니다.

2. **Database Connection**: RDS MySQL과 EKS 간 네트워크 연결을 위해 VPC 보안그룹 설정이 필요합니다.

3. **Container Image**: ECR에 이미지 푸시 전 AWS CLI 인증 및 Docker 로그인 필요합니다.

> ⚠️ `iam-policy.json` 및 `k8s-manifest.md`는 Action Artifacts에서 다운로드하세요.
> 민감한 정보(계정 ID, 실제 도메인, 시크릿 등)는 직접 기입하지 말고 Secrets Manager 또는 팀 내부 문서를 활용하세요.