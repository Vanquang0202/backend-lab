# Duplicate event và idempotency lab

Lab này dùng topic riêng `idempotency-lab` và consumer group riêng `idempotency-lab-group`. Flow `order-created`, Outbox, RabbitMQ và Redis giữ nguyên. Consumer lab dùng chung bảng `processed_events` hiện tại.

## Trạng thái trước khi thêm lab

Consumer order hiện tại đã có idempotency cơ bản:

- Producer tạo `eventId` bằng `UUID.randomUUID()` trong `OrderService`; Outbox tạo UUID khi lưu payload và giữ nguyên UUID khi publish.
- Kafka consumer lấy `event.eventId().toString()` làm key.
- Key được lưu vào `processed_events.event_id`.
- Bảng có unique constraint trên `event_id`.
- Nếu event đến lần hai tuần tự, `existsByEventId()` trả true, consumer log `IDEMPOTENT SKIP` và không gửi notification lần hai.

Flow hiện tại vẫn có race window giữa `existsByEventId()` và `saveAndFlush()`. Unique constraint ngăn insert trùng; catch `DataIntegrityViolationException` cũng ngăn commit bản ghi trùng. Tuy nhiên nếu hai consumer xử lý song song cùng event trước khi insert, business logic có thể chạy hai lần trước khi một insert bị unique constraint từ chối. Đây là giới hạn có chủ ý cho lab; production thường dùng claim/insert idempotency key trước business side effect hoặc cơ chế transaction/lock phù hợp.

## API

Gửi event với eventId tự chọn:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/idempotency/publish `
  -ContentType 'application/json' `
  -Body '{"eventId":"fixed-event-001","orderId":"order-001","payload":"test"}'
```

Response là `202 Accepted`; producer gửi bất đồng bộ vào topic `idempotency-lab`.

Xem các event đã được mark:

```powershell
Invoke-RestMethod http://localhost:8080/idempotency/processed-events | ConvertTo-Json
```

## LAB 1 — Event đầu tiên

Gửi eventId cố định một lần. Log consumer sẽ có:

```text
[IDEMPOTENCY] Received eventId=fixed-event-001
[IDEMPOTENCY] Not processed yet
[IDEMPOTENCY] Processing...
[IDEMPOTENCY] Saved processed event
[IDEMPOTENCY] Completed
```

Kiểm tra `GET /idempotency/processed-events`; `fixed-event-001` phải có một record.

## LAB 2 — Gửi lại cùng eventId

Gửi đúng request trên lần nữa. Consumer sẽ log:

```text
[IDEMPOTENCY] Received eventId=fixed-event-001
[IDEMPOTENCY] Duplicate detected
[IDEMPOTENCY] Skipping event
```

Không có business processing lần hai và bảng vẫn chỉ có một row cho eventId đó.

## LAB 3 — Xóa marker rồi gửi lại

Chỉ dùng trong database lab:

```bash
docker compose exec db psql -U orderuser -d orderdb -c \
  "delete from processed_events where event_id = 'fixed-event-001';"
```

Gửi lại request. Vì marker đã bị xóa, consumer xử lý và insert lại event.

## LAB 4 — Outbox và idempotency giải quyết hai vấn đề khác nhau

- Transactional Outbox bảo vệ phía producer: order và “việc cần publish” commit cùng transaction, giúp tránh mất event khi Kafka tạm thời lỗi.
- Idempotency bảo vệ phía consumer: nếu Kafka giao cùng event nhiều lần, consumer dùng eventId đã xử lý để bỏ qua business logic lặp lại.

Hai pattern bổ sung cho nhau; Outbox không thay thế idempotent consumer.

## LAB 5 — Vì sao Kafka có thể giao lại?

Kafka consumer thường commit offset sau khi listener xử lý thành công. Một chuỗi có thể xảy ra:

1. Consumer xử lý business/database thành công.
2. Process crash trước khi commit offset.
3. Consumer restart hoặc rebalance.
4. Kafka giao lại record từ offset chưa commit.

Vì vậy delivery thực tế thường là at-least-once; consumer cần idempotency.

## LAB 6 — Mô phỏng failure trước khi mark

Gửi payload đặc biệt:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/idempotency/publish `
  -ContentType 'application/json' `
  -Body '{"eventId":"redelivery-001","orderId":"order-001","payload":"FAIL_BEFORE_MARK"}'
```

Consumer cố tình throw trước khi insert `processed_events`, nên transaction không có marker. Spring Kafka error handler retry record; log sẽ cho thấy event được nhận lại và xử lý lại. Implementation chỉ fail một lần cho mỗi eventId trong một JVM để lần retry kế tiếp có thể hoàn tất và insert marker.

Sau cùng, `redelivery-001` phải xuất hiện một lần trong `processed_events`.

## Query trực tiếp PostgreSQL

```bash
docker compose exec db psql -U orderuser -d orderdb -c \
  "select id, event_id, processed_at from processed_events order by processed_at desc;"
```

## Xem Kafka record

```bash
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka:29092 \
  --topic idempotency-lab \
  --from-beginning \
  --group idempotency-inspect-001 \
  --property print.key=true
```

Producer sử dụng Kafka key bằng `eventId`, nên các lần gửi cùng eventId đi cùng partition; đây là hỗ trợ ordering, không tự tạo idempotency. Marker trong `processed_events` mới là cơ chế skip.
