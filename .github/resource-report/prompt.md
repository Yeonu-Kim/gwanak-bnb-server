# Resource Report Generation Task

You are analyzing a repository to generate an AWS resource report and Kubernetes manifest for EKS migration.

## Context
- Repository: $REPO_NAME
- Issue Number: $ISSUE_NUMBER
- AWS Region: $AWS_REGION
- Issue Description: $ISSUE_BODY

## Working Directory
The repository root is your current directory. Output files go to `/tmp/report/`.

```bash
mkdir -p /tmp/report
```

---

## Step 1: Analyze the Repository

Scan the repository to understand the application stack:

1. **Runtime Detection**: Check for `package.json`, `pom.xml`, `build.gradle`, `requirements.txt`, `pyproject.toml`, `Cargo.toml`, `go.mod`
2. **Framework Detection**:
    - Next.js: `next.config.*`, `"next"` in package.json
    - Spring Boot: `spring-boot-starter-*` in pom.xml/build.gradle
    - FastAPI: `fastapi` in requirements.txt/pyproject.toml
    - NestJS: `@nestjs/core` in package.json
3. **Dockerfile**: Check for `Dockerfile`, `docker-compose.yml`, `.dockerignore`
4. **Environment Variables**: Scan `.env.example`, `docker-compose.yml` env sections, `application.yml`, `application.properties` for env var keys (NOT values)
5. **Port**: Extract exposed port from Dockerfile `EXPOSE`, or framework defaults
6. **Dependencies that hint at AWS services**:
    - Redis client (`redis`, `ioredis`, `spring-data-redis`, `redis-py`) → ElastiCache
    - S3 SDK (`@aws-sdk/client-s3`, `boto3`, `spring-cloud-aws`) → S3
    - SES SDK → SES
    - SNS/SQS SDK → SNS/SQS
    - MySQL/PostgreSQL driver → RDS
    - MongoDB driver → DocumentDB or Atlas
    - Elasticsearch client → OpenSearch

Use commands like:
```bash
find . -name "package.json" -not -path "*/node_modules/*" | head -5
find . -name "*.gradle" -o -name "pom.xml" | head -5
find . -name "Dockerfile*" | head -5
find . -name ".env.example" -o -name "application.yml" -o -name "application.properties" | head -5
grep -r "redis\|elasticache" --include="*.json" --include="*.txt" --include="*.toml" --include="*.gradle" --include="*.xml" -l 2>/dev/null | head -10
grep -r "s3\|boto3\|aws-sdk" --include="*.json" --include="*.txt" --include="*.toml" -l 2>/dev/null | head -10
```

---

## Step 2: Estimate AWS Resources

Based on Step 1 analysis, determine required AWS resources:

### Universal (almost always required)
- **EKS**: Application deployment
- **ECR**: Container image registry
- **IAM Role + ServiceAccount**: IRSA for pod-level permissions
- **CloudWatch Logs**: Log aggregation

### Backend applications (Spring, FastAPI, NestJS, etc.)
- **RDS** (if SQL dependency detected): PostgreSQL or MySQL
- **ElastiCache Redis** (if Redis dependency detected)
- **ALB** via AWS Load Balancer Controller (if external traffic needed)
- **SES** (if email SDK detected)
- **SNS/SQS** (if messaging SDK detected)
- **Secrets Manager** (if secret management patterns detected)

### Frontend applications (Next.js, static)
- **S3**: Static asset storage or SSR deployment
- **CloudFront**: CDN

### Common additions
- **Route53**: DNS (if domain config detected)
- **ACM**: TLS certificates

---

## Step 3: Estimate Resource Requirements (CPU/Memory)

### If Dockerfile exists, perform static analysis:
```bash
cat Dockerfile
```

Estimate based on framework:
| Framework | Memory Request | Memory Limit | CPU Request | CPU Limit |
|-----------|---------------|--------------|-------------|-----------|
| Spring Boot | 512Mi | 1Gi | 250m | 500m |
| Spring Boot (heavy) | 1Gi | 2Gi | 500m | 1000m |
| FastAPI | 128Mi | 256Mi | 100m | 300m |
| Next.js SSR | 256Mi | 512Mi | 200m | 500m |
| NestJS | 128Mi | 256Mi | 100m | 300m |
| Next.js static | 64Mi | 128Mi | 50m | 100m |

**Do NOT attempt to docker build in CI** — it may cause OOM or timeout. Instead use static analysis.
If the Dockerfile uses multi-stage builds, note the final stage base image.
If JVM flags like `-Xmx` or `-Xms` are set, use those as memory hints.

---

## Step 4: Generate IAM Policy

Use the `mcp__iam-policy-autopilot__generate_application_policies` tool with:
- The detected AWS services list
- Application name from REPO_NAME
- Region from AWS_REGION

Save the result to `/tmp/report/iam-policy.json`

**Important**: The IAM policy must use placeholder values only:
- Account ID: `<ACCOUNT_ID>`
- Resource ARNs must use placeholders, not real values

---

## Step 5: Generate Kubernetes Manifest

Create `/tmp/report/k8s-manifest.md` with the following content.

**Placeholder rules** — never hardcode real values:
- `<APP_NAME>`: Use the repo name (from $REPO_NAME), lowercase, hyphens only
- `<NAMESPACE>`: `<NAMESPACE>` (team fills this in)
- `<ACCOUNT_ID>`: literal string `<ACCOUNT_ID>`
- `<REGION>`: Use $AWS_REGION
- `<ECR_REPO>`: `<ACCOUNT_ID>.dkr.ecr.$AWS_REGION.amazonaws.com/<APP_NAME>`
- `<IMAGE_TAG>`: `latest` as default
- `<SERVICE_ACCOUNT_NAME>`: `<APP_NAME>-sa`
- `<IAM_ROLE_NAME>`: `<APP_NAME>-irsa-role`
- `<PORT>`: Detected port, or framework default
- `<ENV_KEY>` / `<ENV_VALUE>`: Only include env var KEYS detected from .env.example or config files, set values to `"<REPLACE_ME>"`
- `<STARTUP_PROBE_PATH>`, `<LIVENESS_PROBE_PATH>`, `<READINESS_PROBE_PATH>`: Use `/health`, `/actuator/health`, `/api/health` based on framework
- `<ISTIO_GATEWAY>`: `<NAMESPACE>/istio-gateway`
- `<API_HOSTNAME>`: `<YOUR_DOMAIN>`

The manifest file should be wrapped in a markdown code block with `yaml` syntax highlighting.

---

## Step 6: Generate Summary Report

Create `/tmp/report/summary.md`:

```markdown
## 📊 분석 결과 요약

### 🔍 감지된 스택
- **런타임**: [e.g., Node.js 20, Python 3.11, JVM 17]
- **프레임워크**: [e.g., Next.js 14, FastAPI, Spring Boot 3.x]
- **컨테이너**: [Dockerfile 존재 여부, 베이스 이미지]
- **노출 포트**: [포트 번호]

### ☁️ 필요 AWS 리소스

| 리소스 | 용도 | 필수 여부 |
|--------|------|-----------|
| EKS | 컨테이너 오케스트레이션 | ✅ 필수 |
| ECR | 컨테이너 이미지 레지스트리 | ✅ 필수 |
| ... | ... | ... |

### 💾 리소스 추정

| 항목 | Request | Limit | 근거 |
|------|---------|-------|------|
| CPU | Xm | Xm | [근거] |
| Memory | XMi | XMi | [근거] |

### 🔐 IAM 권한

필요한 IAM Action 목록 (상세 내용은 Artifact의 `iam-policy.json` 참조):
- [주요 permission 요약]

### 📋 다음 단계

팀에서 채워야 할 항목:
- [ ] `<NAMESPACE>` — EKS 네임스페이스 지정
- [ ] `<ACCOUNT_ID>` — AWS 계정 ID
- [ ] `<IAM_ROLE_NAME>` — IRSA 역할 생성 후 ARN 기입
- [ ] `<ENV_KEY>` 값들 — 실제 환경변수 값 설정
- [ ] `<YOUR_DOMAIN>` — 서비스 도메인
- [ ] Probe 경로 확인 (`/health` 등 실제 헬스체크 엔드포인트)

> ⚠️ `iam-policy.json` 및 `k8s-manifest.md`는 Action Artifacts에서 다운로드하세요.
> 민감한 정보(계정 ID, 실제 도메인, 시크릿 등)는 직접 기입하지 말고 Secrets Manager 또는 팀 내부 문서를 활용하세요.
```

---

## Important Security Rules

1. **Never output** actual AWS account IDs, access keys, secret keys, real domain names, or database passwords anywhere
2. **Environment variable values** from `.env` files — use KEYS only, set all values to `"<REPLACE_ME>"`
3. **Do not** read or output `.env` files directly — only `.env.example` or config templates
4. **IAM policy** must use `<ACCOUNT_ID>` placeholder, not real account numbers
5. All secrets should reference AWS Secrets Manager paths as placeholders
