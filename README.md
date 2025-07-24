## 프로젝트

### 레이어드 아키텍처

- **Controller**: HTTP 요청을 받는 계층

- **Service**: 비즈니스 로직 처리 계층

- **Domain**: Entity, Domain Logic 담당 (Entity 중심)

- **DTO**: 요청/응답 전용 객체 (Request/Response DTO)

- **Repository**: DB 접근 계층 (JPA)

모든 의존성은 Controller → Service → Domain → Repository 방향으로만 흐르도록 설계하였습니다.
---

## Getting Started

### Prerequisites

#### Running Docker Containers

`local` profile 로 실행하기 위하여 인프라가 설정되어 있는 Docker 컨테이너를 실행해주셔야 합니다.

```bash
docker-compose up -d 
```