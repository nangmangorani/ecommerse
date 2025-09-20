import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';

// 테스트 데이터 준비
const testData = new SharedArray('test-data', function () {
  const combinations = [];
  // 사용자 1-10, 쿠폰 1-10 조합 생성
  for (let userId = 1; userId <= 10; userId++) {
    for (let couponId = 1; couponId <= 10; couponId++) {
      combinations.push({
        userId: userId,
        couponId: couponId,
        productId: couponId
      });
    }
  }
  return combinations;
});

export const options = {
  scenarios: {
    // 점진적 부하 증가
    gradual_ramp: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '30s', target: 50 },   // 30초간 50명 증가
        { duration: '1m', target: 100 },   // 1분간 100명 유지
        { duration: '30s', target: 0 },    // 30초간 0명으로 감소
      ],
      env: { SCENARIO: 'gradual' },
    },

    // 선착순 상황
    flash_sale: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '5s', target: 200 },
        { duration: '15s', target: 300 },
        { duration: '5s', target: 0 },
      ],
      startTime: '2m',
      env: { SCENARIO: 'flash' },
    },

    // 시나리오 3: 중복 요청 테스트
    duplicate_test: {
      executor: 'per-vu-iterations',
      vus: 20,
      iterations: 3,
      startTime: '4m',
      env: { SCENARIO: 'duplicate' },
    }
  },

  thresholds: {
    'http_req_duration': ['p(95)<1000'],
    'http_req_failed': ['rate<0.1'],
    'successful_coupons': ['count>50'],
  },
};

const BASE_URL = 'http://localhost:8080';

import { Counter } from 'k6/metrics';
const successfulCoupons = new Counter('successful_coupons');

export default function () {
  const scenario = __ENV.SCENARIO;

  let testUser;

  if (scenario === 'duplicate') {
    const fixedIndex = __VU % 10;
    testUser = {
      userId: fixedIndex + 1,
      couponId: fixedIndex + 1,
      productId: fixedIndex + 1
    };
  } else {
    const randomCombination = testData[Math.floor(Math.random() * testData.length)];
    testUser = {
      userId: randomCombination.userId,
      couponId: randomCombination.couponId,
      productId: randomCombination.productId
    };
  }

  const payload = JSON.stringify(testUser);
  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
    timeout: '5s',
  };

  const response = http.post(`${BASE_URL}/coupons/issue`, payload, params);

  const checks = check(response, {
    'request successful': (r) => r && r.status !== undefined,
    'coupon issued (200)': (r) => r && r.status === 200,
    'already issued (404)': (r) => r && r.status === 404 && r.body && r.body.includes('이미 발급받았음'),
    'coupon exhausted': (r) => r && r.status === 404 && r.body && r.body.includes('모두 발급되었습니다'),
    'response time OK': (r) => r && r.timings && r.timings.duration < 1000,
  });

  // 성공 카운트
  if (response && response.status === 200) {
    successfulCoupons.add(1);
    console.log(`User${testUser.userId}-Coupon${testUser.couponId}: 발급 성공`);
  } else if (response && response.status === 404) {
    if (response.body && response.body.includes('이미 발급받았음')) {
      console.log(`User${testUser.userId}-Coupon${testUser.couponId}: 중복 차단`);
    } else if (response.body && response.body.includes('모두 발급되었습니다')) {
      console.log(`User${testUser.userId}-Coupon${testUser.couponId}: 쿠폰 소진`);
    }
  } else {
    console.log(`User${testUser.userId}-Coupon${testUser.couponId}: 오류 (${response ? response.status : 'no response'})`);
  }

  if (scenario === 'flash') {
    sleep(0.1); // 선착순 상황: 짧은 대기
  } else if (scenario === 'duplicate') {
    sleep(0.5); // 중복 테스트: 중간 대기
  } else {
    sleep(Math.random() * 2); // 일반: 랜덤 대기
  }
}

export function setup() {
  console.log('쿠폰 발급 부하테스트 시작');
  console.log(`테스트 대상: ${BASE_URL}/coupons/issue`);

  // 서버 상태 확인
  const healthCheck = http.get(`${BASE_URL}/actuator/health`);
  console.log(`서버 상태: ${healthCheck ? healthCheck.status : 'N/A'}`);

  // Redis 초기화 권장 메시지
  console.log('깨끗한 테스트를 위해 Redis 초기화 권장: redis-cli flushdb');

  return { startTime: new Date().toISOString() };
}

export function teardown(data) {
  console.log('테스트 완료');
  console.log(`시작: ${data.startTime}, 종료: ${new Date().toISOString()}`);
}