# Security lab — Authentication, Authorization, JWT, CORS, CSRF và OAuth2/OIDC

Security lab này dùng các endpoint `/security/**` riêng. Các flow cũ `/orders`, `/outbox`, `/idempotency`, `/resilience`, `/fake-payment` và `/rabbit` được `permitAll` để không đổi behavior hiện tại.

## Authentication và Authorization

- **Authentication** trả lời: “Bạn là ai?”. Lab dùng username/password ở `/security/login`, sau đó tạo JWT.
- **Authorization** trả lời: “Bạn được làm gì?”. Lab đọc role `USER` hoặc `ADMIN` từ Authentication và chặn `/security/admin-area` bằng `hasRole("ADMIN")`.

## LAB 1 — REGISTER và password hashing

```powershell
$body='{"username":"quang","password":"123456"}'
Invoke-RestMethod -Method Post -Uri http://localhost:8080/security/register `
  -ContentType 'application/json' -Body $body | ConvertTo-Json
```

Hibernate tạo bảng `app_users` gồm `id`, `username`, `password_hash`, `role`, `created_at`. Password được hash bằng BCrypt; plaintext không được lưu và không xuất hiện trong log.

Query DB:

```bash
docker compose exec db psql -U orderuser -d orderdb -c \
  "select id, username, password_hash, role, created_at from app_users;"
```

Không thể decrypt BCrypt để lấy password gốc; login dùng `passwordEncoder.matches(input, password_hash)`.

## LAB 2 — LOGIN, access token và refresh token

```powershell
$body='{"username":"quang","password":"123456"}'
$tokens=Invoke-RestMethod -Method Post -Uri http://localhost:8080/security/login `
  -ContentType 'application/json' -Body $body
$tokens | ConvertTo-Json
$accessToken=$tokens.accessToken
$refreshToken=$tokens.refreshToken
```

Access token là JWT HS256, mặc định sống 15 phút. Claims gồm `sub`, `role`, `iat`, `exp`, `typ=access`.

Refresh token sống 7 ngày. Server **không lưu raw refresh token**, chỉ lưu SHA-256 hash trong bảng `security_refresh_tokens`. Raw token chỉ xuất hiện trong response login/refresh và phía client phải giữ an toàn.

## LAB 3 — JWT và `/security/me`

Không có token:

```powershell
try { Invoke-RestMethod http://localhost:8080/security/me } catch { $_.ErrorDetails.Message }
```

Kết quả HTTP 401:

```json
{"status":401,"error":"UNAUTHORIZED","message":"Authentication required"}
```

Có token:

```powershell
Invoke-RestMethod http://localhost:8080/security/me `
  -Headers @{ Authorization="Bearer $accessToken" } | ConvertTo-Json
```

Flow là:

```text
Request -> JwtAuthenticationFilter -> parse/verify signature/expiration
        -> UsernamePasswordAuthenticationToken
        -> SecurityContext -> controller
```

Filter không log full JWT; chỉ log token received, username, role và trạng thái valid/expired.

## LAB 4 — Role-based authorization

USER hoặc ADMIN:

```powershell
Invoke-RestMethod http://localhost:8080/security/user-area `
  -Headers @{ Authorization="Bearer $accessToken" }
```

USER gọi admin:

```powershell
try { Invoke-RestMethod http://localhost:8080/security/admin-area `
  -Headers @{ Authorization="Bearer $accessToken" } } catch { $_.ErrorDetails.Message }
```

Kết quả HTTP 403:

```json
{"status":403,"error":"FORBIDDEN","message":"Admin role required"}
```

## Tạo ADMIN

Cách khuyến nghị cho lab là seed bằng environment variables khi start app:

```powershell
$env:SECURITY_SEED_ADMIN_USERNAME='admin'
$env:SECURITY_SEED_ADMIN_PASSWORD='admin123'
```

Startup sẽ tạo hoặc nâng user đó thành `ADMIN`. Không log password.

Nếu user đã tồn tại, có thể dùng SQL lab:

```bash
docker compose exec db psql -U orderuser -d orderdb -c \
  "update app_users set role='ADMIN' where username='quang';"
```

Sau đó login lại để nhận token có role mới.

## LAB 5 — Access token expired

Mặc định token sống 15 phút. Để test nhanh khi chạy jar/Compose, đặt:

```powershell
$env:SECURITY_ACCESS_TOKEN_TTL='10s'
```

Restart app, login lại, chờ hơn 10 giây rồi gọi `/security/me`; kết quả là HTTP 401 với message `Access token expired`.

Dùng refresh token để xin token mới:

```powershell
$body="{\"refreshToken\":\"$refreshToken\"}"
$newTokens=Invoke-RestMethod -Method Post -Uri http://localhost:8080/security/refresh `
  -ContentType 'application/json' -Body $body
```

Refresh token không được dùng trực tiếp gọi `/security/me`; chỉ access token mới được JWT filter chấp nhận.

## LAB 6 — Refresh và logout/revoke

Refresh token được rotate: token cũ bị revoke khi refresh thành công và token mới được tạo.

```powershell
$logoutBody="{\"refreshToken\":\"$newTokens.refreshToken\"}"
Invoke-RestMethod -Method Post -Uri http://localhost:8080/security/logout `
  -ContentType 'application/json' -Body $logoutBody
```

Gọi `/security/refresh` lại với token đã revoke sẽ trả HTTP 401.

## LAB 7 — CORS

Allowed origin mặc định là `http://localhost:3000`. CORS chỉ là browser security policy; nó không xác thực user và không thay thế JWT.

Preflight:

```bash
curl -i -X OPTIONS http://localhost:8080/security/me \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Authorization"
```

Origin khác sẽ không được trả `Access-Control-Allow-Origin`:

```bash
curl -i -X OPTIONS http://localhost:8080/security/me \
  -H "Origin: http://evil.example" \
  -H "Access-Control-Request-Method: GET"
```

Methods: `GET, POST, PUT, PATCH, DELETE, OPTIONS`. Headers: `Authorization, Content-Type`. Không dùng `*` cùng credentials; lab này dùng Bearer header và `allowCredentials=false`.

## LAB 8 — CSRF

CSRF được disable cho API lab vì access token nằm trong `Authorization: Bearer ...`, không tự động được browser gửi như cookie; API cũng không dùng session cookie cho JWT flow.

Điều này không có nghĩa CSRF luôn không quan trọng. Nếu access/refresh token đặt trong cookie hoặc dùng session authentication, browser tự gửi cookie và cần CSRF token/kiểm tra Origin phù hợp. Khi đổi storage/token transport, threat model phải đánh giá lại.

## LAB 9 — OAuth2/OIDC

OAuth2 client chỉ activate khi:

```powershell
$env:GOOGLE_OAUTH_ENABLED='true'
$env:GOOGLE_CLIENT_ID='...'
$env:GOOGLE_CLIENT_SECRET='...'
```

Không có các biến này, app vẫn start bình thường và không tạo registration Google.

Khi đã cấu hình redirect URI trong Google Console:

```text
GET /oauth2/authorization/google
```

Flow:

```text
app -> Google authorization endpoint
Google -> /login/oauth2/code/google
app đổi authorization code lấy token/user info
Spring Security tạo OidcUser và Authentication
```

- OAuth2 là framework authorization/delegation.
- OIDC là identity layer trên OAuth2.
- ID Token nói về danh tính user/client, dành cho client.
- Access Token dùng gọi resource server/API; không nên nhầm hai loại token.

Client secret không hardcode và không commit.

## LAB 10 — Secret management

Các secret/config nhạy cảm:

```text
JWT_SECRET
DB_PASSWORD
RABBITMQ_PASSWORD
GOOGLE_CLIENT_SECRET
SECURITY_SEED_ADMIN_PASSWORD
```

JWT secret đọc từ `JWT_SECRET`, tối thiểu 32 bytes cho HS256. Nếu không set, app dùng giá trị `DEV_ONLY...` để lab local có thể start; không dùng default này ở production.

Environment/.env chỉ là bước đầu. Production nên dùng Docker Secrets, Kubernetes Secrets, Vault hoặc cloud secret manager. Không log password, JWT secret, client secret hay full token.

## Security config hiện tại

Public:

```text
/security/register
/security/login
/security/refresh
/security/logout
/oauth2/**
/login/**
/fake-payment/**
/resilience/**
/orders/**
/outbox/**
/idempotency/**
/rabbit/**
/actuator/health
/actuator/info
```

Protected:

```text
/security/me          authenticated
/security/user-area   authenticated
/security/admin-area  ROLE_ADMIN
```

Các request khác vẫn `permitAll` để bảo toàn các lab cũ.
