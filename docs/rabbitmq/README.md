# RabbitMQ lab

Lab này độc lập với Kafka. Luồng là:

`POST /rabbit/jobs` → `lab.jobs.exchange` → routing key `lab.jobs` → `lab.jobs.queue` → consumer.

## Thông tin local

- Management UI: http://localhost:15673
- Username: `labuser`
- Password: `labpass`
- AMQP từ host: `localhost:5673`
- AMQP trong Docker network: `rabbitmq:5672`
- Exchange: `lab.jobs.exchange`
- Queue chính: `lab.jobs.queue`
- DLX: `lab.jobs.dlx`
- DLQ: `lab.jobs.dlq`

Credentials trên chỉ dành cho lab local; không dùng cho môi trường thật.

## LAB 1 — Start stack và kiểm tra RabbitMQ healthy

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f rabbitmq
```

Đợi `rabbitmq` có trạng thái `healthy`, rồi mở Management UI. Backend được cấu hình chờ RabbitMQ healthy trước khi start.

## LAB 2 — Publish 1 job và xem consumer xử lý

PowerShell:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/rabbit/jobs `
  -ContentType 'application/json' `
  -Body '{"jobType":"GENERATE_REPORT","payload":"report-001"}'
```

Xem `docker compose logs -f backend`. Consumer sleep mặc định 2 giây để dễ quan sát.

## LAB 3 — Dừng consumer, gửi 5–10 job, xem Ready tăng

Gửi nhanh 5–10 request khi backend đang chạy, sau đó dừng backend ngay:

```bash
docker compose stop backend
```

Nếu muốn publish trực tiếp khi backend đã stop, dùng tab **Exchanges** trong UI, chọn `lab.jobs.exchange`, nhập routing key `lab.jobs` và payload JSON. Mở UI → Queues and Streams → `lab.jobs.queue`: `Ready` tăng vì không có consumer.

## LAB 4 — Start backend và quan sát queue giảm

```bash
docker compose start backend
docker compose logs -f backend
```

Backend sẽ consume dần, `Ready` giảm. Khi message đã giao cho consumer nhưng chưa xử lý xong, `Unacked` tăng.

## LAB 5 — Gửi nhiều job và quan sát consumer

Gửi liên tiếp 5–10 request hợp lệ. Quan sát log `Received`, `Processing`, `Completed`; mỗi message mất khoảng 2 giây.

## LAB 6 — Retry rồi sang DLQ

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/rabbit/jobs `
  -ContentType 'application/json' `
  -Body '{"jobType":"GENERATE_REPORT","payload":"FAIL"}'
```

Log sẽ cho thấy 3 attempt (lần đầu + 2 retry). Sau lần cuối, listener reject không requeue; broker dead-letter message qua `lab.jobs.dlx` vào `lab.jobs.dlq`. Kiểm tra queue DLQ trong UI.

Retry là stateless retry của Spring AMQP với tối đa 3 lần và backoff 1 giây. `ACK` chỉ xảy ra sau khi listener return thành công. Khi exception, message chưa được ACK; recoverer dùng reject/don't-requeue để tránh retry vô hạn và DLX nhận message.

## LAB 7 — Ready vs Unacked

- `Ready`: message đang nằm trong queue, chưa giao cho consumer.
- `Unacked`: đã giao cho consumer nhưng consumer chưa ACK; sleep 2 giây là lúc dễ thấy trạng thái này.
- Khi xử lý thành công, message biến mất khỏi queue sau ACK.
- Khi lỗi, retry diễn ra trong consumer; hết retry thì reject không requeue và message sang DLQ.

## LAB 8 — Các command Docker

```bash
docker compose ps
docker compose logs -f rabbitmq
docker compose logs -f backend
docker compose stop backend
docker compose start backend
docker compose restart backend
```

Nếu host port bị chiếm, override mà không đổi port nội bộ:

```powershell
$env:RABBITMQ_HOST_PORT='5674'
$env:RABBITMQ_MANAGEMENT_HOST_PORT='15674'
docker compose up -d --build
```

Để reset dữ liệu RabbitMQ local, dùng `docker compose down -v` chỉ khi chấp nhận xóa volume của stack; hiện RabbitMQ không dùng volume riêng nên cấu hình sẽ được tạo lại khi container tạo lại.
