# EKS 배포 Resource Report

## 1. 프로젝트 개요

### 사용 중인 AWS 서비스 목록
- **ECR (Elastic Container Registry)**: 컨테이너 이미지 저장 및 배포
- **EKS (Elastic Kubernetes Service)**: 애플리케이션 실행 환경

### EKS 배포 대상 애플리케이션 개요
- **애플리케이션명**: gwanak-bnb-server
- **프레임워크**: Spring Boot 4.0.5 + Java 17
- **데이터베이스**: MySQL (외부 연동)
- **주요 기능**: REST API 서버, 숙박 예약 시스템

## 2. IAM 정책 설명 (EKS Pod IRSA용)

### ECRPull Statement
- **목적**: ECR에서 Docker 이미지 pull을 위한 인증 토큰 획득
- **근거**: EKS Pod가 컨테이너 이미지를 다운로드하기 위해 필수

### ECRRepository Statement  
- **목적**: 특정 ECR 레포지토리에서 이미지 레이어 다운로드
- **근거**: 컨테이너 실행을 위한 이미지 구성 요소 접근 권한

## 3. 리소스 추정 근거

### 메모리 추정 방법: 2순위 (의존성 기반 산정)
- **사유**: Dockerfile에서 JAVA_OPTS(-Xmx) 설정이 확인되지 않음
- **의존성별 메모리 기여값**:
  - spring-boot-starter-webmvc: 300Mi (기본값)
  - spring-boot-starter-data-jpa: +100Mi
  - spring-boot-starter-security: +50Mi
  - HikariCP 커넥션 풀 (기본 10개): +50Mi
- **합산**: 500Mi
- **최종 설정**: 
  - requests.memory: 500Mi
  - limits.memory: 750Mi (500Mi × 1.5)

### CPU 추정
- **requests.cpu**: 100m (idle 상태 기준)
- **limits.cpu**: 500m (요청 처리 피크 기준)

## 4. EKS 적용 전 필수 확인 체크리스트

- [ ] `<ACCOUNT_ID>` 실제 AWS 계정 ID로 교체
- [ ] `<ECR_REPO>` 실제 ECR 레포지토리 이름으로 교체 (예: gwanak-bnb-server)
- [ ] `<NAMESPACE>` EKS 네임스페이스 결정 후 교체 (예: gwanak-bnb)
- [ ] `<ARCH>` 노드그룹 아키텍처(arm64/amd64) 확인 후 교체
- [ ] `<ISTIO_GATEWAY>` Istio 게이트웨이 이름 확인 후 교체
- [ ] `<API_HOSTNAME>` 도메인 확정 후 교체
- [ ] `<IAM_ROLE_NAME>` IRSA용 IAM Role 생성 후 이름 교체
- [ ] DB 비밀번호 등 민감 정보를 Kubernetes Secret으로 분리
- [ ] 실측 후 resources 값 재검토 (docker stats 또는 배포 후 CloudWatch)
- [ ] server.port 미설정으로 기본 포트 8080 사용 확인
- [ ] actuator 엔드포인트 미설정으로 "/" 경로를 헬스체크로 사용 (DummyController 확인 필요)

## 5. 생성 정보

- **생성 일시**: 2026-04-25
- **트리거 이슈**: #1
- **분석 방법**: 정적 코드 분석 + 의존성 기반 리소스 추정
- **분석 대상 파일**:
  - build.gradle: 의존성 확인
  - src/main/resources/application.yaml: 환경변수 및 설정 확인  
  - Dockerfile: JVM 설정 및 포트 확인
  - 소스코드: AWS SDK 사용 여부 확인 (미사용 확인됨)