# Resource Report Generation Task

You are analyzing a repository to generate an AWS resource report and Kubernetes manifest for EKS migration.
This report follows the WaffleStudio infrastructure conventions.

## Context
- Repository: $REPO_NAME
- Issue Number: $ISSUE_NUMBER
- AWS Region: $AWS_REGION
- Issue Description: $ISSUE_BODY
- Project Name: $REPO_NAME (used as `{project_name}` throughout)

## Working Directory
The repository root is your current directory. Output files go to `/tmp/report/`.

```bash
mkdir -p /tmp/report
```

---

## Step 1: Analyze the Repository

Scan the repository to understand the application stack:

1. **Runtime Detection**: Check for `package.json`, `pom.xml`, `build.gradle`, `requirements.txt`, `pyproject.toml`, `Cargo.toml`, `go.mod`
2. **App Type Detection**:
    - **Backend**: Spring Boot (`spring-boot-starter-*`), FastAPI (`fastapi`), NestJS (`@nestjs/core`)
    - **Frontend (CSR)**: Next.js with `output: export` or CRA — S3 + CloudFront 배포
    - **Frontend (SSR)**: Next.js without static export — EKS 배포
3. **Dockerfile**: Check for `Dockerfile`, `docker-compose.yml`
4. **Environment Variables**: Scan `.env.example`, `application.yml`, `application.properties` for env var keys (NOT values)
5. **Port**: Extract from Dockerfile `EXPOSE` or framework defaults (Spring: 8080, FastAPI: 8000, Next.js: 3000)
6. **AWS Service Dependencies**:
    - Redis client (`spring-data-redis`, `ioredis`, `redis-py`) → ElastiCache
    - S3 SDK (`spring-cloud-aws`, `@aws-sdk/client-s3`, `boto3`) → S3
    - SES SDK → SES
    - SNS/SQS SDK → SNS/SQS
    - MySQL/PostgreSQL driver → RDS
    - MongoDB driver → DocumentDB or Atlas
    - Secrets Manager SDK (`spring-cloud-aws-secrets-manager`, `@aws-sdk/client-secrets-manager`) → Secrets Manager
    - CloudWatch SDK 직접 사용 (`CloudWatchLogsClient`) → CloudWatch (SDK 직접 호출 시에만)

```bash
find . -name "package.json" -not -path "*/node_modules/*" | head -5
find . -name "*.gradle" -o -name "pom.xml" | head -5
find . -name "Dockerfile*" | head -5
find . -name ".env.example" -o -name "application.yml" -o -name "application.properties" | head -5
grep -r "redis\|elasticache" --include="*.json" --include="*.txt" --include="*.toml" --include="*.gradle" --include="*.xml" -l 2>/dev/null | head -10
grep -r "secretsmanager\|secrets-manager\|SecretsManager" --include="*.java" --include="*.kt" --include="*.py" --include="*.ts" -l 2>/dev/null | head -10
grep -r "CloudWatchLogs\|cloudwatch" --include="*.java" --include="*.kt" --include="*.py" --include="*.ts" -l 2>/dev/null | head -10
```

---

## Step 2: Determine Required AWS Resources

아래 WaffleStudio 컨벤션에 따라 필요한 리소스를 결정하세요.
모든 리소스는 Prod / Dev 환경을 구분하여 목록화하세요.

### 공통 (항상 필요)

| 리소스 | Prod | Dev |
|--------|------|-----|
| ECR Repository | `{project_name}-prod/server` | `{project_name}-dev/server` |
| Secrets Manager | `{project_name}/prod/*` | `{project_name}/dev/*` |
| IAM Role (IRSA) | `{project_name}-OnPodProd` | `{project_name}-OnPodDev` |
| IAM Policy (IRSA) | `{project_name}-SecretManagerAccessProd` | `{project_name}-SecretManagerAccessDev` |
| IAM User (CICD) | `{project_name}-CICD` (Prod/Dev 공용) | |
| IAM Policy (CICD) | `{project_name}-CICDPushPolicy` (Prod/Dev 공용) | |
| Route 53 (Backend) | `{project_name}-api.wafflestudio.com` | `{project_name}-api-dev.wafflestudio.com` |

### Frontend (CSR — S3 배포) 감지 시 추가

| 리소스 | Prod | Dev |
|--------|------|-----|
| S3 Bucket | `{project_name}-web-prod` | `{project_name}-web-dev` |
| CloudFront | `{project_name}-web-prod` | `{project_name}-web-dev` |
| Route 53 (Frontend) | `{project_name}.wafflestudio.com` | `{project_name}-dev.wafflestudio.com` |

### 앱 코드 분석 기반 추가 리소스 (감지 시에만)

- RDS (SQL 드라이버 감지 시)
- ElastiCache Redis (Redis 클라이언트 감지 시)
- SES, SNS, SQS (해당 SDK 감지 시)

---

## Step 3: Estimate Resource Requirements (CPU/Memory)

Dockerfile을 정적 분석하세요. **Docker 빌드는 절대 실행하지 마세요.**

JVM 플래그(`-Xmx`, `-Xms`)가 있으면 그 값을 memory limit 기준으로 사용하세요.

| Framework | Memory Request | Memory Limit | CPU Request | CPU Limit |
|-----------|---------------|--------------|-------------|-----------|
| Spring Boot | 512Mi | 1Gi | 250m | 500m |
| Spring Boot (heavy) | 1Gi | 2Gi | 500m | 1000m |
| FastAPI | 128Mi | 256Mi | 100m | 300m |
| Next.js SSR | 256Mi | 512Mi | 200m | 500m |
| NestJS | 128Mi | 256Mi | 100m | 300m |
| Next.js static | 64Mi | 128Mi | 50m | 100m |

---

## Step 4: Generate IAM Policies

WaffleStudio 컨벤션에 따라 두 종류의 IAM 정책을 생성하세요.

### 4-1. IRSA 정책 (Pod에서 Secrets Manager 접근용)

`{project_name}-SecretManagerAccessProd` / `{project_name}-SecretManagerAccessDev`

**원칙**: 앱 코드가 런타임에 AWS SDK로 직접 호출하는 서비스만 포함합니다.
EKS, EC2, RDS, CloudWatch(Fluent Bit 담당)는 절대 포함하지 마세요.

기본 포함:
```json
{
  "Effect": "Allow",
  "Action": ["secretsmanager:GetSecretValue"],
  "Resource": [
    "arn:aws:secretsmanager:::secret:{project_name}/prod/*",
    "arn:aws:secretsmanager:::secret:{project_name}/dev/*"
  ]
}
```

앱 분석에서 추가 AWS SDK 사용이 감지된 경우에만 해당 Statement 추가:
- S3 SDK → `s3:GetObject`, `s3:PutObject` 등
- SES SDK → `ses:SendEmail`, `ses:SendRawEmail`
- SNS SDK → `sns:Publish`
- SQS SDK → `sqs:SendMessage`, `sqs:ReceiveMessage`, `sqs:DeleteMessage`
- CloudWatch SDK 직접 사용 시에만 → 로그 그룹 경로: `/app/{project_name}:*`

Resource ARN 규칙:
- Secrets Manager: `secret:{project_name}/*` (특정 시크릿 이름으로 고정하지 마세요)
- CloudWatch Logs: `/app/{project_name}:*` (`/aws/eks/` 경로 사용 금지)

### 4-2. CICD 정책 (`{project_name}-CICDPushPolicy`)

GitHub Actions에서 ECR push 및 S3 배포에 필요한 권한입니다.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:GetRepositoryPolicy",
        "ecr:DescribeRepositories",
        "ecr:ListImages",
        "ecr:DescribeImages",
        "ecr:BatchGetImage",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload",
        "ecr:PutImage"
      ],
      "Resource": [
        "arn:aws:ecr:::repository/{project_name}-prod/server",
        "arn:aws:ecr:::repository/{project_name}-dev/server"
      ]
    }
  ]
}
```

Frontend(CSR) 감지 시 아래 Statement를 추가하세요:
```json
{
  "Effect": "Allow",
  "Action": [
    "s3:PutObject",
    "s3:GetObject",
    "s3:DeleteObject",
    "s3:ListBucket"
  ],
  "Resource": [
    "arn:aws:s3:::{project_name}-web-prod",
    "arn:aws:s3:::{project_name}-web-prod/*",
    "arn:aws:s3:::{project_name}-web-dev",
    "arn:aws:s3:::{project_name}-web-dev/*"
  ]
}
```

### 4-3. CLI로 IRSA 정책 생성

**Python / Go / TypeScript인 경우**:
```bash
uvx iam-policy-autopilot generate-policies  \
  --service-hints  \
  --pretty \
  > /tmp/report/iam-policy-irsa.json
```

**Java (Spring Boot)인 경우** — 정적 분석 미지원, 힌트만 전달:
```bash
echo "" > /tmp/dummy.py
uvx iam-policy-autopilot generate-policies /tmp/dummy.py \
  --service-hints secretsmanager  \
  --pretty \
  > /tmp/report/iam-policy-irsa.json
```

생성된 파일 최상단에 추가:
// ⚠️ Java 앱 — 정적 코드 분석 불가, 서비스 힌트 기반 생성. 배포 전 보안 검토 필요.

CICD 정책은 위 4-2 내용을 그대로 `/tmp/report/iam-policy-cicd.json`으로 저장하세요.

---

## Step 5: Generate Kubernetes Manifest

`/tmp/report/k8s-manifest.md`를 생성하세요.

### Placeholder 규칙
- `<APP_NAME>`: $REPO_NAME 기반, 소문자 + 하이픈만
- `<NAMESPACE>`: `<NAMESPACE>` (팀이 직접 기입)
- `<ACCOUNT_ID>`: 리터럴 문자열 `<ACCOUNT_ID>`
- `<REGION>`: $AWS_REGION
- ECR 이미지: `<ACCOUNT_ID>.dkr.ecr.<REGION>.amazonaws.com/{project_name}-prod/server:<IMAGE_TAG>`
- `<IMAGE_TAG>`: `latest`
- ServiceAccount 이름: `<APP_NAME>-sa`
- IRSA Role: `{project_name}-OnPodProd`
- env 값: 키만 포함, 값은 모두 `"<REPLACE_ME>"`

### 구조 규칙 (반드시 준수)

1. **Namespace 리소스 포함 금지** — 클러스터 레벨에서 팀이 별도 관리
2. **K8s Secret 리소스 생성 금지** — ExternalSecret을 사용하세요
3. **Ingress 사용 금지** — Istio VirtualService로 통일
4. **Service 이름은 `<APP_NAME>`으로 통일** — VirtualService destination과 반드시 일치
5. **nodeSelector와 tolerations 반드시 포함**
6. **startupProbe에 `initialDelaySeconds` 금지** — `failureThreshold`로만 최대 대기 시간 조절
7. **Probe 경로**: Spring Boot → liveness: `/actuator/health/liveness`, readiness: `/actuator/health/readiness` / 기타 → `/health`

### 포함 순서
ServiceAccount → Deployment → Service → VirtualService → ExternalSecret

### 템플릿

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: <APP_NAME>-sa
  namespace: <NAMESPACE>
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::<ACCOUNT_ID>:role/{project_name}-OnPodProd
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: <APP_NAME>
  namespace: <NAMESPACE>
  labels:
    app: <APP_NAME>
spec:
  replicas: 1
  revisionHistoryLimit: 4
  selector:
    matchLabels:
      app: <APP_NAME>
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 25%
      maxUnavailable: 25%
  template:
    metadata:
      labels:
        app: <APP_NAME>
    spec:
      serviceAccountName: <APP_NAME>-sa
      nodeSelector:
        kubernetes.io/arch: arm64
        phase: prod
      tolerations:
        - effect: NoSchedule
          key: phase
          operator: Equal
          value: prod
      containers:
        - name: <APP_NAME>
          image: <ACCOUNT_ID>.dkr.ecr.<REGION>.amazonaws.com/{project_name}-prod/server:<IMAGE_TAG>
          ports:
            - containerPort: <PORT>
          env:
            - name: <ENV_KEY>
              value: "<REPLACE_ME>"
          resources:
            requests:
              cpu: <CPU_REQUEST>
              memory: <MEMORY_REQUEST>
            limits:
              cpu: <CPU_LIMIT>
              memory: <MEMORY_LIMIT>
          startupProbe:
            httpGet:
              path: <STARTUP_PROBE_PATH>
              port: <PORT>
            periodSeconds: 10
            failureThreshold: 18
            timeoutSeconds: 5
          livenessProbe:
            httpGet:
              path: <LIVENESS_PROBE_PATH>
              port: <PORT>
            periodSeconds: 30
            timeoutSeconds: 5
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: <READINESS_PROBE_PATH>
              port: <PORT>
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
---
apiVersion: v1
kind: Service
metadata:
  name: <APP_NAME>
  namespace: <NAMESPACE>
spec:
  type: ClusterIP
  selector:
    app: <APP_NAME>
  ports:
    - port: 80
      targetPort: <PORT>
---
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: <APP_NAME>
  namespace: <NAMESPACE>
spec:
  gateways:
    - <NAMESPACE>/istio-gateway
    - mesh
  hosts:
    - {project_name}-api.wafflestudio.com
  http:
    - route:
        - destination:
            host: <APP_NAME>
---
# Secrets Manager 사용 시에만 포함
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: <APP_NAME>-secrets
  namespace: <NAMESPACE>
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secretsmanager
    kind: ClusterSecretStore
  target:
    name: <APP_NAME>-secrets
  dataFrom:
    - extract:
        key: {project_name}/prod/<SECRET_NAME>
```

---

## Step 6: Generate Summary Report

`/tmp/report/summary.md`를 생성하세요:

```markdown
## 📊 분석 결과 요약 — {project_name}

### 🔍 감지된 스택
- **런타임**: [e.g., JVM 17, Python 3.11, Node.js 20]
- **프레임워크**: [e.g., Spring Boot 3.x, FastAPI, Next.js 14]
- **앱 타입**: [Backend / Frontend CSR / Frontend SSR]
- **컨테이너**: [Dockerfile 존재 여부, 베이스 이미지]
- **노출 포트**: [포트 번호]

### ☁️ 필요 AWS 리소스

| 리소스 | Prod | Dev | 관리 주체 |
|--------|------|-----|-----------|
| ECR | {project_name}-prod/server | {project_name}-dev/server | 인프라팀 |
| Secrets Manager | {project_name}/prod/* | {project_name}/dev/* | 인프라팀 |
| IAM Role (IRSA) | {project_name}-OnPodProd | {project_name}-OnPodDev | 인프라팀 |
| IAM Policy (IRSA) | {project_name}-SecretManagerAccessProd | {project_name}-SecretManagerAccessDev | 인프라팀 |
| IAM User (CICD) | {project_name}-CICD | 공용 | 인프라팀 |
| Route 53 (Backend) | {project_name}-api.wafflestudio.com | {project_name}-api-dev.wafflestudio.com | 인프라팀 |
| [Frontend 감지 시] S3 | {project_name}-web-prod | {project_name}-web-dev | 인프라팀 |
| [Frontend 감지 시] CloudFront | {project_name}-web-prod | {project_name}-web-dev | 인프라팀 |
| [감지 시] RDS | ✅ | ✅ | 인프라팀 |

### 💾 리소스 추정

| 항목 | Request | Limit | 근거 |
|------|---------|-------|------|
| CPU | Xm | Xm | [프레임워크 기본값 / JVM 플래그] |
| Memory | XMi | XMi | [프레임워크 기본값 / JVM 플래그] |

### 🔐 IAM 권한 요약

**IRSA (`{project_name}-SecretManagerAccessProd`)**
> 앱이 런타임에 AWS SDK로 직접 호출하는 서비스만 포함

- [감지된 permission 목록]

**CICD (`{project_name}-CICDPushPolicy`)**
- ECR push/pull: {project_name}-prod/server, {project_name}-dev/server
- [Frontend 감지 시] S3 업로드: {project_name}-web-prod, {project_name}-web-dev

### 📋 다음 단계

- [ ] `<NAMESPACE>` — EKS 네임스페이스 지정
- [ ] `<ACCOUNT_ID>` — AWS 계정 ID
- [ ] IRSA Role 생성: `{project_name}-OnPodProd` / `{project_name}-OnPodDev`
- [ ] IAM User 생성: `{project_name}-CICD` → AccessKey 발급 → GitHub Secrets 등록
- [ ] Secrets Manager에 시크릿 생성 후 ExternalSecret `key` 경로 확인
- [ ] `<YOUR_DOMAIN>` 실제 도메인으로 교체
- [ ] ExternalSecret `secretStoreRef.name` — 클러스터의 실제 ClusterSecretStore 이름 확인
- [ ] Probe 경로 — 실제 헬스체크 엔드포인트 확인

> ⚠️ 생성된 IAM 정책은 baseline입니다. 배포 전 보안 검토 후 최소 권한으로 조정하세요.
> 민감한 정보는 절대 파일에 직접 기입하지 마세요.
```

---

## Important Security Rules

1. 실제 AWS 계정 ID, 액세스 키, 시크릿 키, 도메인, DB 비밀번호를 어디에도 출력하지 마세요
2. `.env` 파일 직접 읽기 금지 — `.env.example`이나 config 템플릿만 참조
3. K8s `Secret` 리소스 생성 금지 — ExternalSecret만 사용
4. IAM 정책의 모든 ARN은 `<ACCOUNT_ID>` placeholder 사용
5. Secrets Manager Resource ARN은 반드시 `{project_name}/*` 패턴으로 작성 (특정 시크릿 이름 고정 금지)
