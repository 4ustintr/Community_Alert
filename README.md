# Community Alert App

## Giới thiệu

**Community Alert** là ứng dụng di động trên nền tảng Android, sử dụng sức mạnh của cộng đồng (crowdsourcing) để xây dựng một bản đồ cảnh báo thiên tai và sự cố trong thời gian thực.

### Tính năng chính:

- ✅ **Đăng nhập/Đăng ký** với Firebase Authentication
- ✅ **Google Sign In** integration
- ✅ **Bản đồ thời gian thực** với Google Maps SDK
- ✅ **Tạo cảnh báo** với camera/gallery picker
- ✅ **Location services** với GPS
- ✅ **Firestore Database** lưu trữ alerts
- ✅ **Firebase Storage** cho hình ảnh
- ✅ **Xem chi tiết cảnh báo** với xác nhận
- ✅ **My Alerts** - quản lý cảnh báo của bạn

## Công nghệ sử dụng

- **Language**: Java
- **Build System**: Gradle
- **Backend**: Firebase (Authentication, Firestore, Storage)
- **Maps**: Google Maps SDK for Android
- **Image Loading**: Glide
- **Location**: Google Play Services Location

## Cấu hình Firebase

### Bước 1: Tạo Firebase Project

1. Truy cập [Firebase Console](https://console.firebase.google.com/)
2. Tạo project mới hoặc chọn project có sẵn
3. Thêm Android app với package name: `com.example.communityalert`

### Bước 2: Download google-services.json

1. Trong Firebase Console, vào **Project Settings**
2. Scroll xuống phần **Your apps**
3. Download file `google-services.json`
4. Copy file vào thư mục: `app/google-services.json` (thay thế file template)

### Bước 3: Enable Firebase Services

#### Authentication
1. Vào **Authentication** → **Sign-in method**
2. Enable **Email/Password**
3. Enable **Google Sign-In**
4. Copy **Web client ID** và paste vào `app/src/main/res/values/strings.xml`:
   ```xml
   <string name="default_web_client_id">YOUR_WEB_CLIENT_ID_HERE</string>
   ```

#### Firestore Database
1. Vào **Firestore Database** → **Create database**
2. Chọn **Start in test mode** (hoặc production mode với security rules)
3. Chọn location gần nhất (asia-southeast1)

#### Storage
1. Vào **Storage** → **Get started**
2. Chọn **Start in test mode**
3. Storage sẽ được dùng để lưu hình ảnh alerts

### Bước 4: Firestore Security Rules (Optional)

Để bảo mật, update Firestore rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /alerts/{alertId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update: if request.auth != null;
    }

    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth.uid == userId;
    }
  }
}
```

### Bước 5: Storage Security Rules (Optional)

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /alerts/{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
  }
}
```

## Cài đặt và chạy

### Yêu cầu

- Android Studio Arctic Fox hoặc mới hơn
- JDK 11
- Android SDK API 24+ (Android 7.0)
- Thiết bị Android hoặc Emulator

### Các bước

1. **Clone repository**
   ```bash
   git clone <repository-url>
   cd Community_Alert
   ```

2. **Cấu hình Firebase** (theo hướng dẫn ở trên)

3. **Sync Gradle**
   ```
   File → Sync Project with Gradle Files
   ```

4. **Build và chạy**
   ```
   Run → Run 'app'
   ```

## Cấu trúc project

```
app/
├── src/main/
│   ├── java/com/example/communityalert/
│   │   ├── SplashActivity.java          # Màn hình splash
│   │   ├── LoginActivity.java           # Đăng nhập
│   │   ├── RegisterActivity.java        # Đăng ký
│   │   ├── MainActivity.java            # Bản đồ chính
│   │   ├── CreateAlertActivity.java     # Tạo cảnh báo
│   │   ├── MyAlertActivity.java         # Danh sách alerts của user
│   │   ├── AlertDetailActivity.java     # Chi tiết alert
│   │   ├── adapter/
│   │   │   └── AlertHistoryAdapter.java # Adapter cho RecyclerView
│   │   └── data/db/
│   │       ├── Alert.java               # Model class
│   │       ├── AlertContract.java       # Database contract
│   │       └── AlertDatabaseHelper.java # SQLite helper
│   └── res/
│       ├── layout/                      # XML layouts
│       ├── values/                      # Strings, colors, themes
│       └── xml/                         # FileProvider paths
└── google-services.json                 # Firebase config
```

## User Flow

### 1. Người dùng A (Người báo tin):
- Mở app → Splash → Login/Register
- Gặp điểm ngập lụt
- Nhấn FAB "+" → Chọn loại "Ngập lụt"
- GPS tự động lấy vị trí
- Chụp ảnh, viết mô tả
- Nhấn "Post Alert" → Upload lên Firebase

### 2. Người dùng B (Người xem tin):
- Mở app → Xem bản đồ
- Thấy marker cảnh báo trên tuyến đường
- Nhấn vào marker → Xem chi tiết
- Xem ảnh, mô tả, vị trí
- Nhấn "Confirm" nếu thông tin đúng

## Permissions

App yêu cầu các permissions sau:

- ✅ `INTERNET` - Kết nối Firebase
- ✅ `ACCESS_FINE_LOCATION` - GPS chính xác
- ✅ `ACCESS_COARSE_LOCATION` - GPS gần đúng
- ✅ `CAMERA` - Chụp ảnh
- ✅ `READ_EXTERNAL_STORAGE` - Đọc ảnh từ gallery
- ✅ `WRITE_EXTERNAL_STORAGE` - Lưu ảnh (Android < 10)

## Troubleshooting

### Lỗi: "google-services.json not found"
- Đảm bảo file `google-services.json` nằm ở `app/google-services.json`
- Sync lại Gradle

### Lỗi: "FirebaseApp not initialized"
- Kiểm tra file `google-services.json` có đúng package name
- Clean và rebuild project

### Lỗi: Google Sign In failed
- Kiểm tra `default_web_client_id` trong `strings.xml`
- Đảm bảo SHA-1 fingerprint đã được thêm vào Firebase Console

### Map không hiển thị
- Kiểm tra internet connection
- OSMDroid cần internet để load tiles

## Tác giả

Developed by Community Alert Team

## License

This project is for educational purposes.
