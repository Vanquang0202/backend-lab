# API Resilience lab — Timeout, Retry, Circuit Breaker

Lab này thêm một fake Payment Service ngay trong cùng Spring Boot app. Backend vẫn gọi fake service qua HTTP tại `/fake-payment/payments`, vì vậy timeout, retry và circuit breaker là các lớp thật của outbound HTTP call. Không thêm container hoặc đổi port Docker hiện tại.

## API

Backend API:

```http
POST /resilience/payment
GET  /resilience/circuit-breaker
```

Fake downstream để quan sát trực tiếp:

```http
POST /fake-payment/payments
POST /fake-payment/reset
```

Request mẫu:

```json
{
  "orderId": "order-001",
  "amount": 100000,
  "mode": "NORMAL"
}
```

Các mode của fake payment:

- `NORMAL`: trả 200 ngay.
- `SLOW`: sleep 9 giây rồi trả 200.
- `ERROR`: trả HTTP 500.
- `FLAKY`: cùng `orderId` fail 2 lần đầu, lần 3 success.

Reset counter của FLAKY trước mỗi bài:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/fake-payment/reset
```

## Cấu hình

- Connect timeout: `2000ms`.
- Read/response timeout: `3000ms`.
- Retry: tối đa `3 attempts`, backoff `1s`.
- Retry chỉ cho `ResourceAccessException` (timeout/network) và `HttpServerErrorException` (HTTP 5xx).
- Không retry HTTP 4xx.
- Circuit Breaker: count window 4 calls, minimum 4 calls, failure threshold 50%.
- OPEN trong 10 giây, sau đó tự chuyển HALF_OPEN; tối đa 2 probe calls.

Có thể override bằng environment variables trong lab:

```text
PAYMENT_CONNECT_TIMEOUT_MS
PAYMENT_READ_TIMEOUT_MS
PAYMENT_RETRY_MAX_ATTEMPTS
PAYMENT_RETRY_WAIT
PAYMENT_CB_WINDOW_SIZE
PAYMENT_CB_MIN_CALLS
PAYMENT_CB_FAILURE_THRESHOLD
PAYMENT_CB_OPEN_WAIT
PAYMENT_CB_HALF_OPEN_CALLS
PAYMENT_SERVICE_URL
```

Trong Docker, default URL là `http://localhost:8080/fake-payment` vì fake service nằm cùng backend container. Khi chạy jar trên host ở port khác, đặt `PAYMENT_SERVICE_URL=http://localhost:18081/fake-payment`.

## LAB 1 — NORMAL

```powershell
$body='{"orderId":"normal-001","amount":100000,"mode":"NORMAL"}'
Invoke-RestMethod -Method Post -Uri http://localhost:8080/resilience/payment `
  -ContentType 'application/json' -Body $body | ConvertTo-Json
```

Kết quả là `APPROVED`, downstream status 200 và chỉ có một attempt.

## LAB 2 — SLOW và timeout

```powershell
$body='{"orderId":"slow-001","amount":100000,"mode":"SLOW"}'
Measure-Command {
  Invoke-RestMethod -Method Post -Uri http://localhost:8080/resilience/payment `
    -ContentType 'application/json' -Body $body
}
```

Fake payment đợi 9 giây nhưng backend timeout sau khoảng 3 giây cho mỗi attempt. Retry hữu hạn nên tổng thời gian có thể gồm nhiều lần timeout và backoff; backend không đợi một call đơn lẻ 9 giây. Cuối cùng response là HTTP 503 với:

```json
{
  "status": "FALLBACK",
  "message": "Payment service temporarily unavailable"
}
```

Log có `attempt=1`, `timeout`, rồi các attempt tiếp theo.

## LAB 3 — ERROR và fallback

```powershell
$body='{"orderId":"error-001","amount":100000,"mode":"ERROR"}'
Invoke-RestMethod -Method Post -Uri http://localhost:8080/resilience/payment `
  -ContentType 'application/json' -Body $body
```

Fake trả 500. Backend retry tối đa 3 attempts, sau đó trả fallback. HTTP 4xx từ fake không nằm trong retry list.

## LAB 4 — FLAKY

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/fake-payment/reset
$body='{"orderId":"flaky-001","amount":100000,"mode":"FLAKY"}'
Invoke-RestMethod -Method Post -Uri http://localhost:8080/resilience/payment `
  -ContentType 'application/json' -Body $body | ConvertTo-Json
```

Fake fail lần 1 và 2, success lần 3. Request cuối cùng trả `APPROVED`.

## LAB 5 — Circuit Breaker

Circuit Breaker cần tối thiểu 4 calls và failure rate từ 50% để OPEN. Gửi `ERROR` cho cùng hoặc các orderId khác ít nhất 4 lần:

```powershell
1..4 | ForEach-Object {
  $body = "{\"orderId\":\"cb-error-$($_)\",\"amount\":100000,\"mode\":\"ERROR\"}"
  try {
    Invoke-RestMethod -Method Post -Uri http://localhost:8080/resilience/payment `
      -ContentType 'application/json' -Body $body
  } catch { $_.ErrorDetails.Message }
}
Invoke-RestMethod http://localhost:8080/resilience/circuit-breaker | ConvertTo-Json
```

Quan sát:

```text
CLOSED -> OPEN
```

Trong trạng thái OPEN, request mới fail-fast, không gọi fake payment và không chạy retry. Chờ 10 giây, rồi xem endpoint state; breaker chuyển `HALF_OPEN`. Đổi downstream về NORMAL bằng request mới:

```powershell
$body='{"orderId":"cb-recover-001","amount":100000,"mode":"NORMAL"}'
Invoke-RestMethod -Method Post -Uri http://localhost:8080/resilience/payment `
  -ContentType 'application/json' -Body $body | ConvertTo-Json
```

Với cấu hình `permitted-number-of-calls-in-half-open-state=2`, cần tối đa 2 probe thành công để đưa breaker về `CLOSED`. Nếu một probe tiếp tục lỗi, breaker mở lại.

## LAB 6 — Ý nghĩa các pattern

- Timeout: không đợi downstream vô thời hạn.
- Retry: thử lại lỗi tạm thời với số lần và backoff hữu hạn.
- Circuit Breaker: tạm ngừng gọi downstream đang lỗi để bảo vệ caller và downstream.

## Retry và Circuit Breaker: tránh retry storm

Implementation dùng thứ tự:

```text
CircuitBreaker(
    Retry(
        HTTP call
    )
)
```

Nghĩa là một request người dùng có tối đa 3 HTTP attempts nhưng Circuit Breaker ghi nhận kết quả cuối cùng của cả request. Khi breaker OPEN, nó chặn ngay từ ngoài nên không tạo thêm retry. Không có vòng retry vô hạn: retry tối đa 3 lần, circuit mở sau ngưỡng failure và giữ OPEN 10 giây.

Trong production cần cân nhắc kỹ việc retry payment vì payment có thể không idempotent. Lab này chỉ mô phỏng resilience; payment thật nên có idempotency key.

## Logging

Các log chính:

```text
[RESILIENCE] orderId=...
[RESILIENCE] mode=...
[RESILIENCE] attempt=...
[RESILIENCE] Calling payment...
[RESILIENCE] downstream status=...
[RESILIENCE] timeout
[RESILIENCE] circuit state=CLOSED -> OPEN
[RESILIENCE] fallback
```

## Kiểm tra state

```powershell
Invoke-RestMethod http://localhost:8080/resilience/circuit-breaker | ConvertTo-Json
```

Response gồm state, failure rate, buffered calls và failed calls.
