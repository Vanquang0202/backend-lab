# Linux server lab (Ubuntu/WSL)

Phần này chạy riêng Spring Boot bằng `java -jar` trên Ubuntu/WSL. PostgreSQL,
Redis và Kafka vẫn chạy trong Docker Desktop/Compose trên Windows. Không cần
chạy service `backend` của Compose khi đang luyện systemd.

## 0. Dependency và network

Từ PowerShell tại repo, khởi động đúng ba dependency:

```powershell
docker compose up -d db redis kafka
docker compose ps
```

Với `.env` hiện tại, host port là PostgreSQL `15432`, Redis `16379`, Kafka
`19092`. Profile `linux-lab` dùng đúng các port này và mặc định trỏ đến
`localhost`. WSL2 thường truy cập được port Docker Desktop publish trên
`localhost`; nếu không, đặt `DB_HOST`, `REDIS_HOST` và Kafka host tương ứng
trong `/etc/backend-lab/backend-lab.env`.

## 1. Bật systemd trong WSL (một lần)

Trong Ubuntu/WSL:

```bash
sudo sh -c 'printf "[boot]\nsystemd=true\n" > /etc/wsl.conf'
```

Từ PowerShell, restart WSL rồi mở Ubuntu lại:

```powershell
wsl --shutdown
wsl
```

Kiểm tra:

```bash
ps -p 1 -o comm=
```

Kết quả mong đợi là `systemd`.

## 2. Build jar

Trong thư mục repo:

```bash
java -version
mvn -version
mvn clean package -DskipTests
ls -lh target/order-lab-0.0.1-SNAPSHOT.jar
```

`pom.xml` dùng Spring Boot Maven Plugin nên `package` tạo executable jar đúng
tên trên. Có thể build ở Windows rồi copy jar sang WSL, nhưng build trên
Ubuntu giúp luyện đúng môi trường server.

## 3. Chạy thử trực tiếp

```bash
java -jar target/order-lab-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=linux-lab
```

Terminal khác:

```bash
curl http://localhost:8080/actuator/health
ss -ltnp | grep ':8080'
```

Dừng bằng `Ctrl-C`. Nếu app không start, kiểm tra Compose dependency và port.

## 4. Cài jar và unit systemd

Các lệnh dưới đây cố ý để bạn thực hành từng bước:

```bash
sudo useradd --system --home /opt/backend-lab --shell /usr/sbin/nologin backend-lab
sudo install -d -o backend-lab -g backend-lab -m 0755 /opt/backend-lab
sudo install -d -m 0755 /etc/backend-lab
sudo install -o backend-lab -g backend-lab -m 0644 \
  target/order-lab-0.0.1-SNAPSHOT.jar /opt/backend-lab/app.jar
sudo install -m 0644 deploy/linux-lab/backend-lab.env.example \
  /etc/backend-lab/backend-lab.env
sudo install -m 0644 deploy/linux-lab/backend-lab.service \
  /etc/systemd/system/backend-lab.service
sudo systemctl daemon-reload
```

Nếu `useradd` báo user đã tồn tại, tiếp tục các lệnh còn lại. Sửa env nếu
WSL không truy cập được `localhost` của Docker Desktop:

```bash
sudoedit /etc/backend-lab/backend-lab.env
```

Nếu Java không nằm ở `/usr/bin/java`, xem bằng `command -v java` và sửa
`ExecStart` trong unit.

## 5. systemctl, journalctl và quan sát hệ thống

```bash
sudo systemctl start backend-lab
systemctl status backend-lab --no-pager
sudo journalctl -u backend-lab -f
```

Terminal khác:

```bash
ps aux | grep '[j]ava'
pgrep -af 'order-lab|backend-lab'
ss -ltnp | grep ':8080'
free -h
df -h
```

Dừng và khởi động lại:

```bash
sudo systemctl stop backend-lab
sudo systemctl restart backend-lab
systemctl is-active backend-lab
```

Xem log gần đây:

```bash
sudo journalctl -u backend-lab -b --no-pager
sudo journalctl -u backend-lab -n 100 --no-pager
```

`journalctl -f` theo dõi realtime; thoát bằng `Ctrl-C`.

## 6. Cố tình làm app lỗi

Chiếm port 8080 bằng listener thử nghiệm:

```bash
python3 -m http.server 8080
```

Terminal khác:

```bash
sudo systemctl restart backend-lab
systemctl status backend-lab --no-pager
sudo journalctl -u backend-lab -n 100 --no-pager
```

Dừng listener bằng `Ctrl-C`, rồi khôi phục service:

```bash
sudo systemctl restart backend-lab
systemctl status backend-lab --no-pager
```

Journal sẽ cho thấy lỗi bind port và việc systemd thử restart process.

## 7. Cập nhật jar

```bash
mvn clean package -DskipTests
sudo install -o backend-lab -g backend-lab -m 0644 \
  target/order-lab-0.0.1-SNAPSHOT.jar /opt/backend-lab/app.jar
sudo systemctl restart backend-lab
sudo journalctl -u backend-lab -n 50 --no-pager
```

> Không dùng `docker compose down -v`: tùy chọn `-v` xóa volume PostgreSQL và
> dữ liệu lab.
