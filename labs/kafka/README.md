# Kafka lab

Kafka CLI nằm trong container `kafka`. Nếu tên khác, lấy bằng `docker compose ps`. Các lệnh dưới đây chạy từ host bằng `docker compose exec kafka ...`; broker trong network là `kafka:29092`.

## Bài 1 — Topic, partition, key
**Scenario** → Muốn kiểm tra ordering và cách key chọn partition.

**Task** → Tạo topic 3 partition; produce nhiều message cùng key rồi key khác nhau; consume từ đầu và xem metadata.

**Expected observation** → Cùng key đi cùng partition và giữ thứ tự trong partition; key khác có thể vào partition khác.

**Hints** → Dùng `kafka-topics.sh`, `kafka-console-producer.sh --property parse.key=true`, `--property print.partition=true`.

## Bài 2 — Consumer group và rebalance
**Scenario** → Một consumer không đủ throughput.

**Task** → Chạy group `lab-group` với 1 consumer, sau đó mở terminal thứ hai với cùng group; quan sát assignment. Tạo group `lab-group-2` và thấy nó cũng nhận toàn bộ message.

**Expected observation** → Một partition chỉ được một consumer trong cùng group xử lý tại một thời điểm; group khác có offset riêng và nhận lại message.

## Bài 3 — Lag
**Scenario** → Consumer xử lý chậm.

**Task** → Produce nhiều message; chạy consumer chậm hoặc tạm dừng consumer; xem CURRENT-OFFSET, LOG-END-OFFSET, LAG.

**Expected observation** → Lag tăng khi producer nhanh hơn consumer.

## Bài 4 — Retry và DLQ
**Scenario** → Notification lỗi tạm thời hoặc lỗi poison message.

**Task** → Đặt biến môi trường rồi restart backend; produce một order/event; xem 2 lần retry rồi topic `order-created.DLT`. Đặt lại false và restart để xử lý event mới.

**Expected observation** → Error handler retry rồi publish record sang DLT.

## Bài 5 — Duplicate và idempotency
**Scenario** → Consumer xử lý DB thành công nhưng chết trước commit offset.

**Task** → Dùng group mới để đọc lại, hoặc restart consumer sau khi event đã được xử lý; kiểm tra `processed_events`. Gửi lại cùng event id và quan sát `IDEMPOTENT SKIP`.

**Expected observation** → Có thể thấy delivery lại, nhưng event id chỉ được ghi một lần.

## Solution

```bash
# topic
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:29092 --create --if-not-exists --topic lab-orders --partitions 3 --replication-factor 1
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:29092 --describe --topic lab-orders

# producer: key:value
docker compose exec -it kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server kafka:29092 --topic lab-orders --property parse.key=true --property key.separator=:
# nhập: A:one, A:two, B:three

# consumer có partition/offset/key
docker compose exec -it kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:29092 --topic lab-orders --from-beginning --group lab-group --property print.partition=true --property print.offset=true --property print.key=true

# groups và lag
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kafka:29092 --list
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kafka:29092 --describe --group lab-group

# xem topic DLT
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:29092 --list
docker compose exec -it kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:29092 --topic order-created.DLT --from-beginning --property print.headers=true

# PowerShell: bật/tắt lỗi giả lập cho backend
$env:LAB_NOTIFICATION_FAIL='true'; docker compose up -d backend
$env:LAB_NOTIFICATION_FAIL='false'; docker compose up -d backend
```

Key quyết định partition; ordering chỉ được đảm bảo trong một partition. Offset là vị trí của record và thuộc consumer group. Consumer group chia partition; rebalance xảy ra khi member join/leave. Lag là khoảng cách log-end với committed offset. Retry/DLQ xử lý lỗi; idempotency cần event id/business key và unique constraint. Lưu ý producer API của app dùng JSON, còn console producer ở trên dùng string cho topic lab riêng.
