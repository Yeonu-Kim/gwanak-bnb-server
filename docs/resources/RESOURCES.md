# EKS 배포 리소스 리포트

## 1. 프로젝트 개요

### 사용 중인 AWS 서비스 목록
- **확인된 AWS 서비스**: 없음
- **분석 결과**: 소스 코드에서 AWS SDK 직접 호출이 확인되지 않음
- **사용 기술스택**: Spring Boot 4.0.5, Spring Data JPA, Spring Security, MySQL

### EKS 배포 대상 애플리케이션 개요
- **애플리케이션명**: gwanak-bnb-server
- **타입**: REST API 서버 (Airbnb 스타일의 숙박 예약 플랫폼)
- **주요 기능**: 사용자 인증, 숙박 시설 관리, 예약 시스템, 리뷰 시스템
- **데이터베이스**: MySQL (외부 RDS 연동)

## 2. IAM 정책 설명 (EKS Pod IRSA용)

### ECRPull Statement
- **목적**: ECR에서 컨테이너 이미지 인증 토큰 획득
- **필요 이유**: EKS Pod가 ECR에서 이미지를 pull하기 위한 기본 권한

### ECRRepository Statement  
- **목적**: 특정 ECR 리포지토리에서 이미지 다운로드
- **필요 이유**: 실제 컨테이너 이미지 레이어 다운로드 권한

**참고**: 소스 코드 분석 결과 AWS SDK 사용이 확인되지 않아, ECR 접근 권한만 포함

## 3. 리소스 추정 근거

### 사용한 방법: **2순위 - 의존성 기반 산정**
- Dockerfile에서 JAVA_OPTS(-Xmx)가 확인되지 않아 2순위 방법 사용

### 의존성 목록과 메모리 기여값
- `spring-boot-starter-webmvc`: 300Mi (기본 웹 스택)
- `spring-boot-starter-data-jpa`: +100Mi (JPA/Hibernate 메모리)
- `spring-security`: +50Mi (보안 필터 체인)
- `HikariCP 커넥션 풀`: +50Mi (기본 10개 연결)
- **합산**: 500Mi
- **Request**: 500Mi, **Limit**: 750Mi (500Mi × 1.5)

### CPU 추정
- **Request**: 100m (idle 상태 기준)
- **Limit**: 500m (요청 처리 피크 기준)

## 4. EKS 적용 전 필수 확인 체크리스트

- [ ] `<ACCOUNT_ID>` 실제 AWS 계정 ID로 교체
- [ ] `<ECR_REPO_NAME>` 실제 ECR 리포지토리 이름으로 교체 (예: gwanak-bnb-server)
- [ ] `<NAMESPACE>` EKS 네임스페이스 결정 후 교체 (예: production)
- [ ] `<ARCH>` 노드그룹 아키텍처(arm64/amd64) 확인 후 교체
- [ ] `<ISTIO_GATEWAY>` Istio 게이트웨이 이름 확인 후 교체
- [ ] `<API_HOSTNAME>` 도메인 확정 후 교체 (예: api.gwanak-bnb.com)
- [ ] IRSA용 IAM Role 생성 후 `<IAM_ROLE_NAME>` ARN 교체
- [ ] DB 비밀번호 등 민감 정보를 Kubernetes Secret으로 분리
- [ ] 실측 후 resources 값 재검토 (docker stats 또는 배포 후 CloudWatch)
- [ ] 헬스체크 경로 `/check` 확인 (DummyController.java:9 참조)

## 5. 생성 정보

- **생성 일시**: 2026-04-25
- **트리거 이슈**: #1 (리소스 리포트 생성)
- **분석 방법**: 정적 코드 분석
- **분석 파일**:
  - `build.gradle`: 의존성 확인
  - `src/main/resources/application.yaml`: 환경변수 및 설정 확인
  - `Dockerfile`: 컨테이너 설정 확인
  - `src/main/java/**/*.java`: AWS SDK 사용 여부 확인