# Order Lab — Backend thực hành local

Backend nhỏ để luyện workflow đi làm: Java 21, Spring Boot, Maven, PostgreSQL, Redis, Kafka và Docker Compose. Business logic cố ý đơn giản; trọng tâm là command, quan sát hệ thống và debug.

## Chạy từ đầu

Yêu cầu: Docker Desktop/Engine, Docker Compose v2, Git. Nếu chạy backend ngoài Docker thì cần JDK 21 và Maven.

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f backend
```

Chờ backend khởi động xong rồi thử:

```bash
curl -X POST http://localhost:8080/orders -H "Content-Type: application/json" -d '{"customerName":"Alice","totalAmount":125.50}'
curl http://localhost:8080/orders/<id>
curl -X PUT http://localhost:8080/orders/<id> -H "Content-Type: application/json" -d '{"customerName":"Alice","totalAmount":150,"status":"PAID"}'
curl -i -X DELETE http://localhost:8080/orders/<id>
curl http://localhost:8080/actuator/health
```

GET dùng Redis cache key `orders::<uuid>` với TTL 120 giây. Update/delete xóa key. Tạo order lưu DB trước rồi publish `order-created`, key Kafka là order id. Consumer ghi `processed_events` bằng event id để bỏ qua message trùng. Retry 2 lần rồi đưa vào `<topic>.DLT`. Bật lỗi consumer bằng `LAB_NOTIFICATION_FAIL=true` (xem Kafka lab).

Tên bảng là `orders`, không phải `ORDER`, vì `ORDER` là SQL reserved word trong PostgreSQL. Nếu muốn chạy app ngoài Compose, dùng `mvn spring-boot:run`; mặc định app trỏ localhost và các biến `DB_*`, `REDIS_*`, `KAFKA_*` có thể override.

## Các lệnh Docker cần nhớ

`docker ps`, `docker ps -a`, `docker logs -f <container>`, `docker exec -it <container> redis-cli`, `docker stats`, `docker restart <container>`, `docker compose up -d`, `docker compose down`, `docker compose logs -f`.

Tên container có thể khác nếu Compose project name khác; dùng `docker compose ps` để lấy tên chính xác. `docker compose down` giữ volume DB; thêm `-v` chỉ khi bạn muốn xóa dữ liệu lab.

Nếu host port đã bị chiếm, override khi chạy (backend vẫn dùng port nội bộ): `$env:BACKEND_HOST_PORT=18080; $env:DB_HOST_PORT=15432; $env:REDIS_HOST_PORT=16379; $env:KAFKA_HOST_PORT=19092; docker compose up -d --build`. Khi đó gọi API ở `http://localhost:18080`; app ngoài Docker cần `DB_PORT=15432`, `REDIS_PORT=16379`, `KAFKA_BOOTSTRAP_SERVERS=localhost:19092`; backend trong Compose không cần đổi.

## Lộ trình

1. [Docker lab](labs/docker/README.md)
2. [Linux/debugging lab](labs/linux/README.md)
3. [Git lab](labs/git/README.md)
4. [Kafka lab](labs/kafka/README.md)
5. [Redis/cache lab](labs/redis/README.md)
6. [Fault scenarios](labs/debugging/README.md)

Mỗi bài để `Solution` ở cuối file. Hãy dừng trước phần đó và tự thử trước.

test git 
