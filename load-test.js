import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * 1. 테스트 설정 (Stages & Thresholds)
 * - 로컬 환경임을 고려하여 부하를 단계적으로 설계했습니다.
 */
export const options = {
    stages: [
        { duration: '1m', target: 1000 }, // 1단계: 1,000 VU (현재 성공한 지점 재확인)
        { duration: '2m', target: 2000 }, // 2단계: 2,000 VU (3,000 RPS 목표 구간)
        { duration: '2m', target: 3000 }, // 3단계: 3,000 VU (5,000 RPS 한계 도전)
        { duration: '1m', target: 0 },    // 종료 및 정리
      ],
      thresholds: {
        // 5,000 RPS 상황에서는 지연시간이 늘어날 수밖에 없으므로 기준을 살짝 넓힙니다.
        http_req_failed: ['rate<0.01'],    // 에러율은 여전히 1% 미만 (엄격)
        http_req_duration: ['p(95)<500'],  // 95% 응답시간 500ms 이내 (로컬 캐시 도입 명분용)
      },
};

const BASE = 'http://localhost:8080';

export default function () {
  // --- [STEP 1] 메인 페이지(1페이지) 조회 ---
  // 트래픽의 100%가 일단 1페이지에 진입합니다. (Redis 캐시 스트레스 테스트)




  // 2. 헤더 설정
    //가짜 Authorization (JWT 간섭을 피하기 위해 X-Test-User-id 사용)
      // 1. 0~499 사이의 랜덤 유저 ID 생성
  const randomUserId = Math.floor(Math.random() * 500);
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Test-User-ID': randomUserId.toString(), // DummyAuthFilter에서 읽을 키값
    },
  };

  // 3. 요청 시 두 번째 인자로 params 전달
  const url = 'http://localhost:8080/posts?size=10';
  const res1 = http.get(url, params);
  check(res1, { '1st page status 200': (r) => r.status === 200 });

  // 1페이지 호출 성공 시 응답 가져오기
  if (res1.status === 200) {
    const resp = res1.json();
    const response = resp.data;
    const posts = response.data;
    const next = response.nextCursor;

    /**
     * 8:2 법칙 적용:
     * - 유저의 30%만 스크롤을 내려 다음 페이지(DB 조회)를 요청한다고 가정합니다.
     * - 이렇게 해야 Redis와 DB의 부하 비율이 현실적으로 배분됩니다.
     */
    const isScrollingUser = Math.random() < 0.3;

    if (isScrollingUser && next && next.id != null && next.at) {
      const id = next.id;
      const at = next.at;

      // --- [STEP 2] 다음 페이지 조회 (Cursor 기반 DB 조회) ---
      // LocalDateTime 문자열 인코딩 필수
      const secondUrl = `${BASE}/posts?size=10&id=${id}&at=${encodeURIComponent(at)}`;
      const res2 = http.get(secondUrl, params);

      check(res2, {
        'cursor page status 200': (r) => r.status === 200,
        'has items': (r) => {
          const posts = r.json()?.data?.data;
          return Array.isArray(posts) && posts.length > 0;
        },
      });
    }
  }

  // 가상 유저(VU) 간의 생각할 시간 (0.5~1.5초 랜덤)
  sleep(Math.random() * 0.2 + 0.1);
}