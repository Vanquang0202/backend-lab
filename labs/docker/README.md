# Docker lab

## Bài 1 — Nhìn hệ thống đang chạy
**Scenario** → Compose vừa được start.

**Task** → Tìm container đang chạy, container đã dừng, port mapping và health status.

**Expected observation** → Có backend, db, redis, kafka; backend map `0.0.0.0:8080`.

**Hints** → Thử `docker ps`, `docker ps -a`, `docker compose ps`.

## Bài 2 — Logs và shell trong container
**Scenario** → API trả lỗi hoặc muốn kiểm tra Redis.

**Task** → Theo dõi log backend; mở Redis CLI và kiểm tra `PING`; vào DB container.

**Expected observation** → Backend log có Spring/Kafka; Redis trả `PONG`; `psql` có thể list database.

**Hints** → Dùng `docker compose logs -f backend`, `docker exec -it <name> redis-cli`, `docker exec -it <name> psql -U orderuser -d orderdb`.

## Bài 3 — Restart, stats, down
**Scenario** → Backend tăng CPU hoặc bị treo.

**Task** → Quan sát resource, restart riêng backend, rồi stop toàn bộ stack và start lại.

**Expected observation** → DB volume vẫn giữ order sau `down` rồi `up -d`.

**Hints** → `docker stats`, `docker restart <name>`, `docker compose down`, `docker compose up -d`.

## Solution

```bash
docker ps
docker ps -a
docker compose ps
docker compose logs -f backend
docker exec -it <redis-container> redis-cli PING
docker stats
docker restart <backend-container>
docker compose down
docker compose up -d
```

