# Redis/cache lab

## Bài 1 — Cache miss/hit
**Scenario** → Gọi GET order lần đầu và lần hai.

**Task** → Xóa key nếu có; gọi GET hai lần; xem log và Redis key.

**Expected observation** → Lần đầu đọc DB rồi set cache; lần sau trả từ cache. Key dạng `orders::<uuid>`.

**Hints** → `redis-cli --scan --pattern 'orders::*'`, bật SQL log nếu cần.

## Bài 2 — TTL expire
**Scenario** → Cache chỉ sống ngắn.

**Task** → Dùng `TTL` và `EXPIRE`; quan sát TTL giảm rồi key biến mất.

**Expected observation** → TTL khoảng 120 giây cho cache ứng dụng.

## Bài 3 — Stale cache
**Scenario** → Một developer update DB trực tiếp nhưng quên invalidate Redis.

**Task** → Gọi GET để warm cache; sửa DB bằng psql; gọi GET lại trước khi expire.

**Expected observation** → API trả dữ liệu cũ. Sau khi xóa key hoặc update qua PUT, dữ liệu mới xuất hiện.

## Bài 4 — Invalidation đúng
**Scenario** → Update/delete order qua API.

**Task** → Warm cache, PUT/DELETE, kiểm tra key trước và sau.

**Expected observation** → `@CacheEvict` xóa key; GET sau update đọc dữ liệu mới.

## Bài 5 — Redis CLI và KEYS
**Scenario** → Cần tìm cache keys trong production.

**Task** → Thực hành `SET/GET/DEL/TTL/EXPIRE`; so sánh `KEYS *` với `SCAN`.

**Expected observation** → `KEYS *` có thể block Redis khi database lớn; SCAN trả incremental cursor.

## Solution

```bash
docker compose exec redis redis-cli SET lab:sample hello
docker compose exec redis redis-cli GET lab:sample
docker compose exec redis redis-cli EXPIRE lab:sample 10
docker compose exec redis redis-cli TTL lab:sample
docker compose exec redis redis-cli DEL lab:sample
docker compose exec redis redis-cli --scan --pattern 'orders::*'
docker compose exec redis redis-cli SCAN 0 MATCH 'orders::*' COUNT 100
# chỉ lab nhỏ:
docker compose exec redis redis-cli KEYS 'orders::*'
```

Để tạo cache app: POST order rồi GET; Redis key thường hiện dưới dạng `orders::<uuid>` tùy serializer/cache prefix. TTL cache được đặt bằng cấu hình Redis cache manager mặc định của Spring; nếu muốn ép đúng 120 giây, thay bằng CacheManager bean trong code rồi kiểm tra lại bằng `TTL`.

