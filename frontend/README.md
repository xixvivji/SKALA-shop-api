# SKALA Shop Frontend

프레임워크 없이 HTML, CSS와 ES Module JavaScript로 구현한 쇼핑몰 프론트입니다.
Spring Boot API의 고객·관리자 기능을 모두 연결하고 Vercel에 정적 사이트로
배포합니다.

- 운영 주소: <https://skala-shop-bice.vercel.app>
- 진입 파일: [`index.html`](index.html)
- 상태와 화면 로직: [`js/app.js`](js/app.js)
- API client: [`js/api.js`](js/api.js)
- 디자인: [`styles.css`](styles.css)

## 제공 화면

### 고객

- 상품 검색, 카테고리·가격 필터와 재고 표시
- 회원가입·로그인·비밀번호 재설정
- 장바구니, 저장 배송지와 다중 상품 주문
- 주문·배송 상태와 부분 취소
- 프로필, 보유 상품, 포인트 잔액·거래 원장
- 회원 탈퇴와 세션 만료 재로그인

### 관리자

- 상품명·가격·카테고리·설명·이미지 URL 관리
- 초기 재고와 재고 증감 조정
- 고객과 전체 주문 조회
- 배송 상태 변경과 이력 조회

데스크톱과 모바일에서 같은 HTML을 사용하며 860px 이하에서는 하단 navigation과
모바일 카드 레이아웃으로 전환합니다. 키보드 tab 이동, dialog focus 복원,
`aria-live`, reduced motion과 명확한 입력 오류를 지원합니다.

## 파일 구조

```text
frontend/
├── index.html                    # 모든 view, form과 dialog
├── styles.css                   # 반응형 storefront 디자인
├── config.js                    # 환경에 맞는 API 주소 선택
├── runtime-config.js            # 로컬 기본 runtime 설정
├── js/
│   ├── api.js                   # fetch, CSRF, 쿠키, 오류와 API 함수
│   └── app.js                   # 상태, render와 사용자 interaction
├── scripts/
│   ├── build.mjs                # Vercel 배포용 dist 생성
│   └── validate-static.mjs      # 정적 계약·접근성 회귀 검사
├── tests/
│   ├── shop-flow.spec.js        # mock 기반 고객·관리자 E2E
│   └── live-production.spec.js  # opt-in 실제 운영 E2E
├── playwright.config.js
└── vercel.json                  # 빌드와 API rewrite
```

## 로컬 실행

먼저 저장소 루트에서 PostgreSQL과 백엔드를 실행합니다.

```bash
docker compose -f compose.local.yml up -d
./gradlew bootRun
```

다른 터미널에서 정적 서버를 실행합니다.

```bash
python3 -m http.server 3000 --directory frontend
```

<http://localhost:3000>으로 접속합니다. `file://`로 직접 열면 ES Module과 CORS가
정상 동작하지 않습니다.

## API 주소 결정

`config.js`는 실행 위치에 따라 다음 순서로 API 주소를 선택합니다.

1. 빌드가 생성한 `window.SKALA_CONFIG.API_BASE_URL`
2. 로컬 개발에서만 허용하는 localStorage override
3. localhost에서는 같은 host의 `:8080`
4. 운영에서는 현재 Vercel Origin

운영 `vercel.json`은 다음 요청을 EC2 API로 rewrite합니다.

```text
/api/*      → https://api-3-39-64-119.sslip.io/api/*
/actuator/* → https://api-3-39-64-119.sslip.io/actuator/*
```

따라서 기본 운영 구성에는 Vercel의 `SKALA_API_BASE_URL` 환경변수가 필요하지
않습니다. 외부 API Origin을 직접 호출하도록 변경할 때만 HTTPS 주소를 설정합니다.

API 주소는 정적 파일에 노출되는 공개 설정입니다. DB 비밀번호, JWT secret,
Docker Hub token이나 AWS 자격 증명을 프론트 환경변수에 넣으면 안 됩니다.

## API client 동작

`js/api.js`가 다음 공통 처리를 담당합니다.

- 모든 요청에 `credentials: "include"`
- 상태 변경 전에 CSRF token 발급과 `X-XSRF-TOKEN` 전송
- 403 CSRF 실패 시 토큰을 한 번 새로 받아 재시도
- JSON과 204 응답 구분
- 공통 `ApiError` 변환과 field error 보존
- 주문·취소·재고용 UUID 명령 키 생성

`js/app.js`는 API 결과를 상태에 반영하고 DOM을 다시 그립니다. 초기 세션 복구와
새 로그인 요청이 경합하지 않도록 generation과 session snapshot을 비교합니다.
화면이 1분 이상 숨겨졌다가 다시 보이면 현재 인증을 재확인합니다.

## 상품 데이터

상품 응답은 다음 정보를 화면에 사용합니다.

```text
id, name, price, status, categoryId, description, imageUrl, stock
```

`imageUrl`은 HTTP/HTTPS만 화면에 사용하며 값이 없거나 올바르지 않으면 상품명의
첫 글자와 CSS tone을 표시합니다. 품절·판매 중지 상품의 담기와 주문 버튼은
비활성화하고 재고가 바뀌면 목록을 다시 읽습니다.

## 인증 UX

- 서버 Validation의 `fieldErrors`를 해당 input 아래에 표시합니다.
- 로그인·가입·재설정의 성공과 실패 메시지를 dialog 안에 보여줍니다.
- 401이 오면 고객 상태를 지우고 한 번만 재로그인 dialog를 엽니다.
- 고객 ID 저장은 localStorage를 사용하지만 비밀번호와 JWT는 저장하지 않습니다.
- JWT는 HttpOnly 쿠키이므로 JavaScript가 직접 읽지 않습니다.

현재 비밀번호 재설정은 고객 ID와 등록 이름을 확인하는 교육용 흐름입니다. 상용
서비스에서는 이메일·휴대전화 일회용 인증으로 교체해야 합니다.

## 빌드와 정적 검사

```bash
npm --prefix frontend ci
npm --prefix frontend run check
npm --prefix frontend run build
```

빌드는 `frontend/dist`를 새로 만들고 HTML, CSS, JavaScript와 runtime 설정을
복사합니다. Vercel build에서 `SKALA_API_BASE_URL`을 지정했다면 HTTPS가 아닌 주소를
거부합니다.

## 브라우저 테스트

로컬 mock E2E:

```bash
npx --prefix frontend playwright install chromium
npm --prefix frontend run test:e2e
```

Desktop Chrome과 Pixel 5 viewport에서 고객·관리자 흐름을 검사합니다. API 응답을
테스트 안에서 모킹하므로 로컬 백엔드가 없어도 실행할 수 있습니다.

실제 운영 E2E:

```bash
LIVE_E2E_ENABLED=true \
E2E_BASE_URL=https://skala-shop-bice.vercel.app \
npm --prefix frontend run test:e2e:live
```

이 검사는 실제 고객·주문 기록을 생성하므로 명시적으로 활성화할 때만 실행합니다.
주문은 전량 취소하고 임시 고객은 비활성화합니다. 관리자 읽기 검사에는
`SKALA_ADMIN_ID`, `SKALA_ADMIN_PASSWORD` 환경변수가 추가로 필요합니다.

전체 테스트 범위는 [테스트 전략](../docs/testing.md)을 참고합니다.

## Vercel 설정

- GitHub repository: `xixvivji/SKALA-shop-api`
- Root Directory: `frontend`
- Production Branch: `main`
- Build Command: `node scripts/build.mjs`
- Output Directory: `dist`

PR에는 Vercel Preview가 생성되고 `main` 병합 시 운영 주소가 갱신됩니다. 백엔드
운영 배포와 프론트 배포는 독립적이므로, API 계약을 변경할 때는 이전 프론트와의
호환 순서를 고려해야 합니다.
