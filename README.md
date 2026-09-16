# EnglishExamApp

Ứng dụng hỗ trợ luyện thi IELTS, gồm:

- **Backend:** Spring Boot + MySQL
- **Frontend:** Flutter
- **AI:** Gemini API
- **Speech-to-Text:** Google Cloud Speech-to-Text
- **Lưu trữ media:** Cloudinary
- **Thanh toán:** MoMo Sandbox
- **Triển khai backend demo:** Railway

---

## 1. Cấu trúc dự án

```text
EnglishExamApp/
├── backend/
│   ├── src/
│   ├── pom.xml
│   ├── mvnw
│   ├── mvnw.cmd
│   └── Dockerfile
│
└── frontend/
    └── englishexamapp/
        ├── android/
        ├── ios/
        ├── lib/
        └── pubspec.yaml
```

---

## 2. Yêu cầu môi trường

### Backend

- **Java 21**
- **MySQL 8.x**
- Không bắt buộc cài Maven riêng vì dự án đã có **Maven Wrapper** (`mvnw`, `mvnw.cmd`).

Kiểm tra Java:

```bash
java -version
```

### Frontend

- **Flutter SDK**
- **Dart SDK 3.13.x trở lên tương thích với project**
- Android Studio/Android Emulator hoặc thiết bị Android thật

Kiểm tra Flutter:

```bash
flutter --version
flutter doctor
```

Nếu Windows không nhận lệnh `flutter`, kiểm tra thư mục `flutter\bin` đã được cấu hình trong biến môi trường `Path`.

---

# 3. Cách chạy nhanh nhất để demo

Đây là cách đơn giản nhất vì **backend đã được triển khai trên Railway**, không cần chạy Spring Boot và MySQL trên máy local.

Backend demo:

```text
https://englishexamapp-production.up.railway.app
```

Trong frontend hiện tại, `lib/config/api_config.dart` đã cấu hình:

```dart
class ApiConfig {
  static const String baseUrl =
      'https://englishexamapp-production.up.railway.app';
}
```

## Bước 1: Mở thư mục frontend

```bash
cd frontend/englishexamapp
```

## Bước 2: Cài các package Flutter

```bash
flutter pub get
```

## Bước 3: Kiểm tra thiết bị

```bash
flutter devices
```

Khởi động Android Emulator trước nếu muốn chạy bằng máy ảo.

## Bước 4: Chạy ứng dụng

```bash
flutter run
```

Ứng dụng Flutter sẽ kết nối trực tiếp đến backend trên Railway.

### Giao diện Admin

Có thể truy cập giao diện Admin bằng trình duyệt:

```text
https://englishexamapp-production.up.railway.app/admin/login
```

---

# 4. Chạy toàn bộ hệ thống ở local

Phần này dùng khi muốn tự chạy **MySQL + Spring Boot backend + Flutter frontend** trên máy.

## 4.1. Chuẩn bị MySQL

Tạo database:

```sql
CREATE DATABASE englishexamapp
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

> **Lưu ý:** Trong hai gói mã nguồn backend/frontend hiện tại không có file SQL seed hoặc database dump.  
> Backend có sử dụng các dữ liệu nền như role `USER`, vì vậy khi chạy với database mới hoàn toàn cần import dữ liệu khởi tạo của dự án nếu có.

---

## 4.2. Cấu hình backend

Backend đọc cấu hình thông qua biến môi trường.

Các biến cơ bản:

```text
SERVER_PORT
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
JWT_SECRET
JWT_ACCESS_TOKEN_EXPIRATION_MINUTES
```

Ví dụ trên **PowerShell**:

```powershell
$env:SERVER_PORT="8081"
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/englishexamapp"
$env:SPRING_DATASOURCE_USERNAME="root"
$env:SPRING_DATASOURCE_PASSWORD="your_mysql_password"
$env:JWT_SECRET="replace_with_a_long_random_secret"
$env:JWT_ACCESS_TOKEN_EXPIRATION_MINUTES="60"
```

### Chạy thử không dùng Gemini thật

Backend hỗ trợ chế độ AI mock:

```powershell
$env:AI_MOCK_ENABLED="true"
```

Khi `AI_MOCK_ENABLED=true`, phần đánh giá AI sử dụng kết quả mock và không cần Gemini API key cho bước đánh giá AI.

### Chạy thử thanh toán không dùng MoMo Sandbox

```powershell
$env:PAYMENT_MOCK_ENABLED="true"
```

Khi bật chế độ này, backend tạo giao dịch Premium mock để phục vụ demo.

---

## 4.3. Chạy backend

Mở terminal tại thư mục:

```bash
cd backend
```

Trên Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Backend mặc định chạy tại:

```text
http://localhost:8081
```

Giao diện Admin local:

```text
http://localhost:8081/admin/login
```

---

# 5. Cấu hình các dịch vụ bên ngoài

Chỉ cần cấu hình các dịch vụ dưới đây khi muốn sử dụng chức năng thật tương ứng.

## 5.1. Gemini API

Nếu muốn dùng Gemini thật:

```powershell
$env:AI_MOCK_ENABLED="false"
$env:GEMINI_API_KEY="your_gemini_api_key"
$env:GEMINI_MODEL="gemini-3.5-flash"
```

Không đưa API key trực tiếp vào mã nguồn hoặc commit lên Git.

---

## 5.2. Google Cloud Speech-to-Text

Speaking sử dụng Google Cloud Speech-to-Text để chuyển âm thanh thành transcript.

Trên Windows, có thể cấu hình đường dẫn tới file service account:

```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS="C:\path\to\application-credentials.json"
```

Cấu hình mặc định trong backend:

```text
GOOGLE_SPEECH_LANGUAGE_CODE=en-US
GOOGLE_SPEECH_AUDIO_ENCODING=ENCODING_UNSPECIFIED
GOOGLE_SPEECH_SAMPLE_RATE_HERTZ=0
```

Ví dụ:

```powershell
$env:GOOGLE_SPEECH_LANGUAGE_CODE="en-US"
```

---

## 5.3. Cloudinary

Để upload media:

```powershell
$env:CLOUDINARY_CLOUD_NAME="your_cloud_name"
$env:CLOUDINARY_API_KEY="your_api_key"
$env:CLOUDINARY_API_SECRET="your_api_secret"
```

---

## 5.4. MoMo Sandbox

Nếu sử dụng thanh toán MoMo thật trong môi trường Sandbox:

```powershell
$env:PAYMENT_MOCK_ENABLED="false"
$env:MOMO_PARTNER_CODE="your_partner_code"
$env:MOMO_ACCESS_KEY="your_access_key"
$env:MOMO_SECRET_KEY="your_secret_key"
$env:MOMO_REDIRECT_URL="your_redirect_url"
$env:MOMO_IPN_URL="your_ipn_url"
```

Backend mặc định sử dụng endpoint tạo giao dịch Sandbox:

```text
https://test-payment.momo.vn/v2/gateway/api/create
```

---

# 6. Chạy frontend với backend local

Frontend hiện đang trỏ tới Railway.

Mở file:

```text
frontend/englishexamapp/lib/config/api_config.dart
```

Nếu chạy Android Emulator, đổi thành:

```dart
class ApiConfig {
  static const String baseUrl = 'http://10.0.2.2:8081';
}
```

`10.0.2.2` là địa chỉ Android Emulator dùng để truy cập `localhost` của máy tính.

Nếu chạy trên thiết bị Android thật, dùng địa chỉ IP LAN của máy chạy backend, ví dụ:

```dart
class ApiConfig {
  static const String baseUrl = 'http://192.168.1.10:8081';
}
```

Điện thoại và máy tính cần kết nối cùng mạng.

Sau đó:

```bash
cd frontend/englishexamapp
flutter pub get
flutter run
```

---

# 7. Kiểm thử

## Backend

```powershell
cd backend
.\mvnw.cmd test
```

## Frontend

```bash
cd frontend/englishexamapp
flutter test
```

---

# 8. Một số lỗi thường gặp

### `flutter is not recognized`

Kiểm tra Flutter:

```cmd
where flutter
```

Nếu Flutter SDK nằm tại:

```text
D:\flutter
```

thì `Path` của Windows cần có:

```text
D:\flutter\bin
```

Sau khi cập nhật `Path`, đóng và mở lại VS Code/Terminal.

---

### Frontend không kết nối được backend local

Không dùng:

```text
http://localhost:8081
```

trên Android Emulator.

Dùng:

```text
http://10.0.2.2:8081
```

---

### Backend không kết nối được MySQL

Kiểm tra:

- MySQL đang chạy.
- Database `englishexamapp` đã được tạo.
- Username/password đúng.
- `SPRING_DATASOURCE_URL` đúng.

---

### Đăng ký tài khoản lỗi trên database mới

Backend tìm role mặc định có tên:

```text
USER
```

Do đó database local cần có dữ liệu role tương ứng. Nếu dự án có file SQL dump/seed riêng, hãy import file đó trước khi demo bằng database mới.

---

### Speaking không tạo được transcript

Kiểm tra:

- File Google Cloud service account hợp lệ.
- `GOOGLE_APPLICATION_CREDENTIALS` trỏ đúng file JSON.
- Google Cloud Speech-to-Text API đã được bật.
- Ứng dụng có quyền sử dụng microphone.
- File âm thanh gửi lên backend là WAV hợp lệ.

---

# 9. Docker

Backend có sẵn `Dockerfile`.

Tuy nhiên, trong hai gói mã nguồn được cung cấp ở đây **không có `docker-compose.yml`**, nên README này không ghi một quy trình Docker Compose giả định.

Nếu repository chính có `docker-compose.yml`, có thể bổ sung riêng phần hướng dẫn chạy backend và MySQL bằng Docker Compose dựa trên file đó.

---

# 10. Ghi chú bảo mật

Không commit các thông tin sau lên Git:

```text
JWT_SECRET
GEMINI_API_KEY
CLOUDINARY_API_SECRET
MOMO_SECRET_KEY
Google service account JSON
Database password
```

Nên lưu các thông tin này bằng biến môi trường hoặc secret manager của môi trường triển khai.
