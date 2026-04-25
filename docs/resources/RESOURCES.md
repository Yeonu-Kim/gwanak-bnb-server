# EKS 배포 리소스 리포트

## 1. 프로젝트 개요

### 사용 중인 AWS 서비스 목록
- **EKS**: Kubernetes 클러스터 (배포 대상)
- **ECR**: 컨테이너 이미지 레지스트리 (이미지 Pull 권한 필요)
- **RDS**: MySQL 데이터베이스 (password 기반 인증, IAM 권한 불필요)

애플리케이션에서 직접 사용하는 AWS SDK 서비스는 없음을 확인하였습니다.

### EKS 배포 대상 애플리케이션 개요
- **이름**: gwanak-bnb-server
- **유형**: Spring Boot 3.x + Java 17 웹 애플리케이션
- **주요 기능**: REST API, JWT 인증, JPA ORM, Spring Security
- **데이터베이스**: MySQL (외부 RDS 연결)
- **포트**: 8080

## 2. IAM 정책 설명 (EKS Pod IRSA용)

### ECRPull Statement
- **목적**: ECR에서 Docker 이미지를 Pull하기 위한 토큰 획득
- **권한**: `ecr:GetAuthorizationToken`
- **리소스**: `*` (AWS 계정 전체)
- **근거**: EKS Pod가 ECR 레지스트리에 인증하기 위해 필요

### ECRRepository Statement
- **목적**: 특정 ECR 레포지토리에서 이미지 레이어 다운로드
- **권한**: `ecr:BatchGetImage`, `ecr:GetDownloadUrlForLayer`, `ecr:BatchCheckLayerAvailability`
- **리소스**: 특정 ECR 레포지토리 ARN
- **근거**: 컨테이너 이미지 레이어를 실제로 다운로드하기 위해 필요

## 3. 리소스 추정 근거

### 메모리 추정 (2순위 방법 사용)
Dockerfile에 JVM 힙 설정(-Xmx)이 없어 의존성 기반 산정을 사용했습니다.

**의존성별 메모리 기여값:**
- `spring-boot-starter-webmvc`: 300Mi (기본 웹 프레임워크)
- `spring-boot-starter-data-jpa`: +100Mi (JPA 및 Hibernate)
- `spring-boot-starter-security`: +50Mi (보안 프레임워크)
- HikariCP 커넥션 풀 (기본 10개): +50Mi (DB 커넥션)

**총 추정값:**
- **requests.memory**: 500Mi
- **limits.memory**: 750Mi (500Mi × 1.5)

**근거 파일:** `build.gradle:22-25`

### CPU 추정
- **requests.cpu**: 100m (idle 상태 기준)
- **limits.cpu**: 500m (요청 처리 피크 기준)

## 4. EKS 적용 전 필수 확인 체크리스트

- [ ] `<ACCOUNT_ID>` 실제 AWS 계정 ID로 교체
- [ ] `<ECR_REPO_NAME>` 실제 ECR 레포지토리 이름으로 교체
- [ ] `<NAMESPACE>` EKS 네임스페이스 결정 후 교체
- [ ] `<ARCH>` 노드그룹 아키텍처(arm64/amd64) 확인 후 교체
- [ ] `<ISTIO_GATEWAY>` Istio 게이트웨이 이름 확인 후 교체
- [ ] `<API_HOSTNAME>` 도메인 확정 후 교체
- [ ] `<IAM_ROLE_NAME>` IRSA용 IAM Role 생성 후 ARN 교체
- [ ] `<RDS_ENDPOINT>` RDS 엔드포인트 확정 후 교체
- [ ] `<DB_NAME>`, `<DB_USERNAME>`, `<DB_PASSWORD>` DB 정보 확정 후 교체
- [ ] `<JWT_SECRET>` JWT 시크릿 키 확정 후 교체
- [ ] DB 비밀번호 등 민감 정보를 Kubernetes Secret으로 분리
- [ ] 실측 후 resources 값 재검토 (docker stats 또는 배포 후 CloudWatch)

## 5. 생성 정보

- **생성 일시**: 2026-04-25
- **트리거 이슈**: #1
- **분석 방법**: 정적 코드 분석 + 기본 인프라 권한 적용
- **AWS 리전**: ap-northeast-2
- **분석 대상 파일**: build.gradle, application.yaml, Dockerfile, docker-compose.prod.yml