// k6 load test for the authorization API.
// Install k6 (https://k6.io) then run against a running app:
//   k6 run -e BASE_URL=http://localhost:8080 load/authorize.js
//
// It fires many concurrent authorizations and asserts the p95 latency budget.
import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  vus: 20,            // virtual users
  duration: '30s',
  thresholds: {
    http_req_duration: ['p(95)<100'],   // 95% of requests under 100ms
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  // Unique stan/rrn per iteration so each is a fresh authorization (not an idempotent replay).
  const n = ${__VU}${__ITER}.padStart(6, '0').slice(-6);
  const rrn = ${Date.now()}.slice(-12).padStart(12, '0');

  const payload = JSON.stringify({
    pan: '4111111111111111',
    expiry: '3012',
    amount: 100,            // $1.00 — small so balance/limit don't run out
    currency: '840',
    stan: n,
    rrn: rrn,
  });

  const res = http.post(${BASE_URL}/authorize, payload, {
    headers: { 'Content-Type': 'application/json' },
  });

  check(res, {
    'status is 200': (r) => r.status === 200,
    'has responseCode': (r) => r.json('responseCode') !== undefined,
  });
}