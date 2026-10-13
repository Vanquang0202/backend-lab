# Transactional Outbox lab

Lab này là flow riêng tại `/outbox`; flow cũ `POST /orders`, Kafka notification, RabbitMQ và Redis không bị thay đổi.

## Ý tưởng

`POST /outbox/orders` chạy trong một transaction database duy nhất:

1. Insert vào `orders`.
2. Insert `outbox_events` với `event_type=ORDER_CREATED`, `status=NEW` và payload JSON.
3. Commit cả hai cùng nhau.

Transaction này **không** gọi Kafka. Một publisher nền lấy event `NEW` hoặc `FAILED`, gửi vào topic `${ORDER_TOPIC}` (mặc định `order-created`) rồi cập nhật trạng thái. Publisher dùng trạng thái nội bộ `PROCESSING` khi đang claim và dừng retry sau `OUTBOX_MAX_RETRY` lần (mặc định 5).

## API

Tạo order qua Outbox:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/outbox/orders `
  -ContentType 'application/json' `
  -Body '{"customerName":"outbox-lab","totalAmount":125.50}'
```

Xem event và trạng thái:

```powershell
Invoke-RestMethod http://localhost:8080/outbox/events | ConvertTo-Json -Depth 5
```

Bật/tắt failure mode runtime:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/outbox/failure-mode `
  -ContentType 'application/json' -Body '{"enabled":true}'
Invoke-RestMethod -Method Post -Uri http://localhost:8080/outbox/failure-mode `
  -ContentType 'application/json' -Body '{"enabled":false}'
```

Có thể bật ngay từ lúc start bằng `OUTBOX_SIMULATE_KAFKA_FAILURE=true`. Failure mode chỉ làm publisher fail có chủ đích; order và outbox row vẫn commit.

## LAB 1 — Commit rồi publish thành công

1. Đảm bảo failure mode là `false`.
2. Gọi `POST /outbox/orders`.
3. Quan sát log `[OUTBOX] order saved`, `[OUTBOX] event stored`, `publishing event`, `publish success`.
4. Sau tối đa khoảng 2 giây, event có `status=PUBLISHED` và có `publishedAt`.

## LAB 2 — DB commit nhưng Kafka publish fail

1. `POST /outbox/failure-mode` với `{"enabled":true}`.
2. Tạo một order mới.
3. Order vẫn tồn tại trong `orders`; outbox event tồn tại độc lập với trạng thái `FAILED` và `retryCount` tăng.
4. Log có `[OUTBOX] publish failed`; publisher không retry vô hạn.

## LAB 3 — Tắt lỗi và retry

1. Gửi `{"enabled":false}`.
2. Chờ publisher chạy hoặc xem log backend.
3. Event `FAILED` được claim lại, publish vào Kafka và chuyển sang `PUBLISHED`.

## LAB 4 — Vì sao tốt hơn dual-write trực tiếp?

Với cách `save DB` rồi `publish Kafka` trực tiếp, DB có thể commit nhưng process/broker lỗi trước khi publish; không còn bản ghi nào mô tả việc cần publish. Outbox ghi “việc cần làm” trong cùng transaction DB với order. Publisher có thể thử lại sau khi dependency hồi phục, nên event không bị mất chỉ vì một lỗi tạm thời.

Outbox không phải distributed transaction: vẫn có khoảng thời gian order đã commit nhưng event chưa được publish, nên consumer nhận event bất đồng bộ.

## LAB 5 — Giới hạn: duplicate và idempotency

Publisher có thể publish thành công nhưng process chết trước khi cập nhật `PUBLISHED`. Khi chạy lại, event có thể được gửi lần nữa. Vì vậy outbox không tự loại bỏ duplicate delivery. Consumer phải idempotent bằng `eventId`/business key và unique constraint. Consumer Kafka hiện tại đã có bảng `processed_events` để minh họa điều này.

## Query PostgreSQL

```bash
docker compose exec db psql -U orderuser -d orderdb -c \
  "select id, aggregate_id, event_type, status, retry_count, created_at, published_at, last_error from outbox_events order by created_at;"

docker compose exec db psql -U orderuser -d orderdb -c \
  "select id, customer_name, total_amount, status, created_at from orders order by created_at desc;"
```

Bảng `outbox_events` được Hibernate tạo/cập nhật từ entity; không sửa hoặc xóa bảng `orders` hiện tại.

## Xem Kafka event

Topic mặc định là `order-created`. Consumer ứng dụng cũng đọc topic này nên log notification có thể xuất hiện khi publish thành công.

```bash
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka:29092 --topic order-created --from-beginning \
  --property print.key=true --property print.headers=true
```

Nếu dùng consumer group mới để xem lại, Kafka sẽ đọc các record từ đầu theo group đó. Không dùng `LAB_NOTIFICATION_FAIL` cho failure mode Outbox; biến đó là lab consumer cũ.
