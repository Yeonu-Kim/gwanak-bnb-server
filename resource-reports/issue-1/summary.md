## 📊 분석 결과 요약 — gwanak-bnb-server

### 🔍 감지된 스택
- **런타임**: JVM 17
- **프레임워크**: Spring Boot 4.0.5
- **앱 타입**: Backend
- **컨테이너**: Dockerfile 존재, eclipse-temurin:17-jre 베이스 이미지
- **노출 포트**: 8080

### ☁️ 필요 AWS 리소스

| 리소스 | Prod | Dev | 관리 주체 |
|--------|------|-----|-----------|
| ECR | gwanak-bnb-server-prod/server | gwanak-bnb-server-dev/server | 인프라팀 |
| Secrets Manager | gwanak-bnb-server/prod/* | gwanak-bnb-server/dev/* | 인프라팀 |
| IAM Role (IRSA) | gwanak-bnb-server-OnPodProd | gwanak-bnb-server-OnPodDev | 인프라팀 |
| IAM Policy (IRSA) | gwanak-bnb-server-SecretManagerAccessProd | gwanak-bnb-server-SecretManagerAccessDev | 인프라팀 |
| IAM User (CICD) | gwanak-bnb-server-CICD | 공용 | 인프라팀 |
| Route 53 (Backend) | gwanak-bnb-server-api.wafflestudio.com | gwanak-bnb-server-api-dev.wafflestudio.com | 인프라팀 |
| RDS | ✅ | ✅ | 인프라팀 |

### 💾 리소스 추정

| 항목 | Request | Limit | 근거 |
|------|---------|-------|------|
| CPU | 250m | 500m | Spring Boot 기본값 |
| Memory | 512Mi | 1Gi | Spring Boot 기본값 |

### 🔐 IAM 권한 요약

**IRSA (`gwanak-bnb-server-SecretManagerAccessProd`)**
> 앱이 런타임에 AWS SDK로 직접 호출하는 서비스만 포함

- secretsmanager:GetSecretValue

**CICD (`gwanak-bnb-server-CICDPushPolicy`)**
- ECR push/pull: gwanak-bnb-server-prod/server, gwanak-bnb-server-dev/server

### 📋 다음 단계

- [ ] `<NAMESPACE>` — EKS 네임스페이스 지정
- [ ] `<ACCOUNT_ID>` — AWS 계정 ID
- [ ] IRSA Role 생성: `gwanak-bnb-server-OnPodProd` / `gwanak-bnb-server-OnPodDev`
- [ ] IAM User 생성: `gwanak-bnb-server-CICD` → AccessKey 발급 → GitHub Secrets 등록
- [ ] Secrets Manager에 시크릿 생성 후 ExternalSecret `key` 경로 확인
- [ ] `<YOUR_DOMAIN>` 실제 도메인으로 교체
- [ ] ExternalSecret `secretStoreRef.name` — 클러스터의 실제 ClusterSecretStore 이름 확인
- [ ] Probe 경로 — 실제 헬스체크 엔드포인트 확인

> ⚠️ 생성된 IAM 정책은 baseline입니다. 배포 전 보안 검토 후 최소 권한으로 조정하세요.
> 민감한 정보는 절대 파일에 직접 기입하지 마세요.