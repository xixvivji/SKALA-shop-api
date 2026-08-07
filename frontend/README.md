# SKALA Shop Frontend

Spring Boot 쇼핑몰 API의 현재 기능을 시연하는 순수 HTML/CSS/JavaScript 프론트엔드입니다. 별도 빌드 과정이나 프레임워크가 필요하지 않습니다.

## 현재 연결된 기능

- 회원가입, 로그인, 아이디 저장, 비밀번호 표시, 로그아웃, 회원 탈퇴
- 고객 ID와 가입 이름을 확인하는 데모용 비밀번호 재설정
- HttpOnly JWT 쿠키 인증과 CSRF 토큰 처리
- 내 정보, 포인트 잔액·거래 원장, 이름 변경과 보유 상품 조회
- 카테고리·상품명·가격 범위 검색, 실시간 주문 가능 재고·품절 표시
- 저장 배송지 등록·수정·삭제와 기본 배송지
- 장바구니 추가·수량 변경·삭제·비우기와 저장 배송지를 이용한 다중 상품 주문
- UUID 멱등성 키를 사용한 주문·부분 취소와 주문·배송 상태 조회
- 관리자 로그인, 고객·전체 주문 조회, 배송 상태 변경·이력
- 관리자 상품 등록·수정·삭제와 재고 초기화·증감 조정

상품 등록·수정 화면은 백엔드 계약과 동일하게 판매 가격을
`0.01`~`30,000,000.00`로 제한합니다. 최대 주문 수량 1,000,000개를 곱한
30조 포인트까지 브라우저 `Number`의 소수 둘째 자리 왕복을 검증합니다.

## 로컬 실행

먼저 백엔드를 `http://localhost:8080`에서 실행합니다. 그다음 이 디렉터리에서 정적 서버를 실행합니다.

```bash
cd frontend
python3 -m http.server 3000
```

브라우저에서 `http://localhost:3000`으로 접속합니다. 파일을 직접 여는 `file://` 방식은 ES 모듈과 CORS 문제 때문에 사용하지 않습니다.

현재 백엔드의 로컬 CORS 기본값에 맞추려면 다음 조합 중 하나를 사용해야 합니다.

- 프론트 `http://localhost:3000` + 백엔드 `http://localhost:8080`
- 프론트 `http://127.0.0.1:5500` + 백엔드 `http://127.0.0.1:8080`

## 브라우저 E2E 테스트

Playwright는 실제 백엔드 데이터에 의존하지 않는 상태 기반 API 모킹으로 고객과
관리자의 핵심 화면 흐름을 검증합니다.

```bash
cd frontend
npm ci
npx playwright install chromium
npm run test:e2e
```

현재 회원가입 → 장바구니 → 배송지 저장 → 다중 상품 주문 → 포인트 조회와 관리자
주문 조회 → 배송 상태 변경 → 변경 이력 조회를 Chromium에서 확인합니다. GitHub
Actions의 프론트 검사와 운영 배포 검사에서도 같은 테스트를 실행합니다.

실제 Vercel과 운영 API를 연결한 검사는 명시적으로 활성화할 때만 실행합니다.
임시 고객과 주문 기록이 생성되므로 일반 E2E와 분리되어 있습니다. 주문은 테스트
안에서 전량 취소하고 임시 고객은 비활성화합니다.

```bash
LIVE_E2E_ENABLED=true \
E2E_BASE_URL=https://skala-shop-bice.vercel.app \
npm run test:e2e:live
```

관리자 목록까지 읽기 검증하려면 `SKALA_ADMIN_ID`, `SKALA_ADMIN_PASSWORD`를
로컬 환경변수로만 추가합니다. 비밀번호는 명령 기록이나 저장소 파일에 넣지 않습니다.

## API 주소 설정

`config.js`는 실행 중인 주소를 기준으로 안전한 기본값을 선택합니다.

- `localhost` 또는 `127.0.0.1`에서 실행하면 같은 호스트의 `8080` 포트를 API로 사용합니다.
- Vercel 배포에서는 프론트의 HTTPS Origin을 기본값으로 사용하고, `vercel.json`이 `/api`와 `/actuator` 요청을 EC2 API로 프록시합니다. 브라우저에는 Vercel 주소만 보이고 인증 쿠키도 같은 Origin에서 동작합니다.
- 프론트와 API를 서로 다른 Origin으로 직접 연결해야 할 때만 Vercel 환경변수 `SKALA_API_BASE_URL`에 실제 **HTTPS API 주소**를 설정합니다. 빌드는 이 값을 `runtime-config.js`에 기록하며 HTTP 주소는 거부합니다. API 주소는 비밀값이 아니며 정적 파일에서 확인할 수 있습니다.

Vercel 프로젝트의 Root Directory는 `frontend`, Production Branch는 `main`으로
설정합니다. 기본 프록시 구성에서는 `SKALA_API_BASE_URL` 환경변수를 등록하지
않습니다. 외부 API 직접 연결로 변경해 환경변수를 추가했다면 새로 배포해야
적용됩니다.

정적 프론트에 주입되는 API 주소는 비밀값이 아닙니다. DB 비밀번호, JWT secret,
Docker Hub 토큰이나 AWS 자격 증명은 Vercel 환경변수에 넣지 않습니다.

로컬에서만 다른 API를 시험하려면 브라우저 콘솔에서 아래 값을 저장하고 새로고침합니다. 배포 환경에서는 이 로컬 스토리지 재정의를 무시합니다.

```js
localStorage.setItem("skala-api-base-url", "http://localhost:8080");
```

## 비밀번호 재설정 주의사항

현재 재설정 기능은 학습·시연 요구사항에 맞춰 고객 ID와 현재 등록된 이름을 확인합니다. 두 값은 강한 본인 인증 수단이 아니므로 실제 서비스에서는 이메일 또는 휴대전화 일회용 인증으로 교체해야 합니다. 새 비밀번호는 백엔드에서 평문으로 저장하지 않고 BCrypt로 다시 해시합니다.

## 관리자 시연

백엔드의 bootstrap 관리자 환경변수를 설정하고 서버를 처음 실행한 뒤 같은 로그인 화면에서 관리자 계정으로 로그인합니다. 관리자 화면에서는 고객 목록과 상품 관리를 확인할 수 있습니다. 운영에서는 bootstrap 관리자 생성을 완료한 후 해당 기능을 비활성화하는 편이 안전합니다.

## Vercel 배포 전 확인

GitHub의 `SKALA-shop-api` 저장소를 Vercel로 가져온 뒤 Root Directory를 `frontend`로 지정하면 이 폴더를 정적 사이트로 배포할 수 있습니다. 운영에서는 쿠키 인증이 안정적으로 동작하도록 프론트와 API를 같은 상위 도메인 아래에 두는 구성을 권장합니다. 예: `shop.example.com`과 `api.example.com`. 또한 백엔드의 CORS 허용 Origin, HTTPS, 인증 쿠키의 `Secure`/`SameSite` 설정을 실제 도메인에 맞춰야 합니다.
