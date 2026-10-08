# Fault scenarios

Chỉ đọc symptom và tự lập hypothesis trước. Mỗi scenario nên bắt đầu bằng `docker compose ps`, logs, health check và kiểm tra dependency. Solution ở cuối.

## Bài 1 — Redis
**Scenario** → GET order trả 500; log backend có lỗi timeout tới Redis.

**Task** → Xác định dependency nào lỗi và khôi phục service.

**Expected observation** → API trở lại sau khi dependency usable.

## Bài 2 — Kafka unavailable
**Scenario** → POST order lưu được hoặc chậm, consumer không notification; log producer/consumer có connection errors.

**Task** → Phân biệt DB write với Kafka publish; kiểm tra broker và topic.

## Bài 3 — DB sai cấu hình
**Scenario** → Backend restart loop; health fail; log nói authentication/connection refused.

**Task** → Kiểm tra env thực tế trong container và credential DB.

## Bài 4 — Port 8080 bị chiếm
**Scenario** → Compose backend không start, host báo address already in use.

**Task** → Tìm listener, đổi port mapping hoặc dừng process phù hợp.

## Bài 5 — Container crash
**Scenario** → `docker ps` không thấy backend nhưng `docker ps -a` thấy exited.

**Task** → Xem exit code/logs, rồi xác định crash startup hay runtime.

## Bài 6 — Disk/log tăng
**Scenario** → Docker Desktop cảnh báo disk; Kafka/Postgres volume lớn.

**Task** → Quan sát `docker system df`, volume, container logs; tránh xóa volume dữ liệu khi chưa hiểu.

## Bài 7 — Kafka lag
**Scenario** → Notification chậm dù producer vẫn chạy.

**Task** → Đo lag group; kiểm tra consumer logs, retry/DLT và số partition/member.

## Bài 8 — Redis stale
**Scenario** → PUT trả dữ liệu mới nhưng GET ngay sau đó vẫn trả dữ liệu cũ.

**Task** → Kiểm tra key/TTL và đường update; chứng minh cache invalidate có/không.

## Solution

```bash
docker compose ps
docker compose logs --tail=200 backend redis kafka db
docker compose exec backend printenv | grep -E 'DB_|REDIS_|KAFKA_'
docker compose exec redis redis-cli PING
docker compose exec kafka /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server kafka:29092
docker compose exec db pg_isready -U orderuser -d orderdb
docker ps -a
docker inspect <container>
docker port <container>
docker system df
docker stats
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kafka:29092 --describe --group order-notification-group
docker compose exec redis redis-cli --scan --pattern 'orders::*'
```

Khôi phục có kiểm soát: sửa env/config rồi `docker compose up -d --build backend`; broker/DB/Redis chỉ restart service liên quan. Chỉ dùng `docker compose down -v` khi chấp nhận mất DB volume.

