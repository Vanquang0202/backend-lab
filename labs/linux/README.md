# Linux/debugging lab

Các lệnh dùng được trong Linux shell; nếu host là Windows, chạy trong container bằng `docker exec -it <backend> sh` hoặc dùng WSL. Đừng xem Solution trước.

## Bài 1 — Java process
**Scenario** → Backend đang chạy nhưng cần biết PID/command.

**Mục tiêu** → Tìm process Java và xem command line.

**Tôi cần tự tìm command** → Tìm theo `java`, `pid`, `command`.

**Expected observation** → Một JVM chạy jar `app.jar`.

## Bài 2 — RAM và disk
**Scenario** → Container phản hồi chậm hoặc log tăng.

**Mục tiêu** → Phân biệt RAM process/container và disk filesystem.

**Tôi cần tự tìm command** → Tìm command tương đương `free`, `df`, `du`.

**Expected observation** → Biết filesystem nào gần đầy và thư mục log lớn.

## Bài 3 — Port 8080
**Scenario** → Backend không bind được port.

**Mục tiêu** → Tìm process đang listen trên 8080.

**Tôi cần tự tìm command** → Tìm `ss` hoặc `lsof`.

**Expected observation** → Có PID/process và địa chỉ listen.

## Bài 4 — Log realtime và grep ERROR
**Scenario** → Có lỗi intermittent.

**Mục tiêu** → Follow log rồi lọc ERROR/Exception.

**Tôi cần tự tìm command** → Nghĩ đến `tail -f` và `grep`.

## Bài 5 — Curl và dependency connectivity
**Scenario** → API không tạo order hoặc consumer không xử lý.

**Mục tiêu** → Gọi health/API, thử DNS/port của Redis, Kafka, DB từ backend container.

**Tôi cần tự tìm command** → `curl`, `nc`/ `getent`, client CLI.

**Expected observation** → Tách được HTTP lỗi với lỗi network dependency.

## Solution

```bash
ps aux | grep '[j]ava'
pgrep -af java
free -h
df -h
du -sh /var/log/* 2>/dev/null
ss -ltnp | grep ':8080'
lsof -i :8080
tail -f app.log
grep -RniE 'ERROR|Exception' /var/log 2>/dev/null
curl -v http://localhost:8080/actuator/health
getent hosts redis kafka db
nc -vz redis 6379
nc -vz kafka 29092
nc -vz db 5432
```

