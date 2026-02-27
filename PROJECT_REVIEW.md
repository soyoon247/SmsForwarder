# SmsForwarder 프로젝트 검토 리포트

## 1. 현재 상태 요약

Android Studio에서 자동 생성한 **기본 Compose 템플릿** 상태이며, SMS 전달 관련 기능은 **전혀 구현되지 않았다**.

| 항목 | 현재 상태 | 목표 |
|------|----------|------|
| SMS 수신 | 미구현 | BroadcastReceiver로 SMS 감지 |
| SMS 전달 | 미구현 | SmsManager로 자동 전송 |
| 설정 UI | 미구현 (Hello Android만 표시) | 발신번호/키워드/대상번호 설정 화면 |
| 설정 저장 | 미구현 | SharedPreferences 기반 저장 |
| 권한 처리 | 미구현 | Runtime 권한 요청 |
| 백그라운드 동작 | 미구현 | Foreground Service |
| 재부팅 자동 시작 | 미구현 | BootReceiver |

---

## 2. Gradle / 의존성 버전 문제

현재 프로젝트의 버전들이 **2022년 기준**으로 상당히 오래되었다. 빌드 호환성과 최신 API 지원을 위해 업그레이드가 필요하다.

### 2.1 프로젝트 레벨 (`build.gradle`)

```groovy
// 현재
id 'com.android.application' version '8.0.0' apply false
id 'org.jetbrains.kotlin.android' version '1.7.20' apply false

// 권장
id 'com.android.application' version '8.7.3' apply false
id 'org.jetbrains.kotlin.android' version '2.0.21' apply false
// Compose를 사용하므로 Kotlin Compose 컴파일러 플러그인 추가 필요
id 'org.jetbrains.kotlin.plugin.compose' version '2.0.21' apply false
```

> Kotlin 2.0부터 Compose 컴파일러가 Kotlin 플러그인으로 통합되었다. 기존 `composeOptions.kotlinCompilerExtensionVersion` 설정은 삭제하고 `kotlin-compose` 플러그인을 사용해야 한다.

### 2.2 앱 레벨 (`app/build.gradle`)

| 항목 | 현재 | 권장 |
|------|------|------|
| `compileSdk` | 33 | 35 |
| `minSdk` | 24 | 26 |
| `targetSdk` | 33 | 35 |
| `jvmTarget` | 1.8 | 17 |
| `sourceCompatibility` | 1.8 | 17 |
| Compose BOM | 2022.10.00 | 2024.12.01 |
| `core-ktx` | 1.8.0 | 1.15.0 |
| `lifecycle-runtime-ktx` | 2.3.1 | 2.8.7 |
| `activity-compose` | 1.5.1 | 1.9.3 |

```groovy
// 현재: 더 이상 필요 없음 (Kotlin 2.0+에서는 플러그인으로 대체)
composeOptions {
    kotlinCompilerExtensionVersion '1.3.2'  // 삭제 대상
}

// 대신 plugins 블록에 추가:
id 'org.jetbrains.kotlin.plugin.compose'
```

### 2.3 Gradle Wrapper

```properties
# 현재
distributionUrl=https\://services.gradle.org/distributions/gradle-8.0-bin.zip

# 권장 (AGP 8.7.x 호환)
distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-bin.zip
```

---

## 3. AndroidManifest.xml 누락 사항

현재 매니페스트에는 기본 Activity만 등록되어 있다. SMS 전달 앱에 필요한 다음 항목들이 **모두 누락**되어 있다.

### 3.1 필요한 권한 선언

```xml
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### 3.2 필요한 컴포넌트 등록

```xml
<!-- SMS 수신 BroadcastReceiver -->
<receiver android:name=".receiver.SmsReceiver"
    android:exported="true"
    android:permission="android.permission.BROADCAST_SMS">
    <intent-filter android:priority="999">
        <action android:name="android.provider.Telephony.SMS_RECEIVED" />
    </intent-filter>
</receiver>

<!-- 재부팅 감지 BroadcastReceiver -->
<receiver android:name=".receiver.BootReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>

<!-- Foreground Service -->
<service android:name=".service.SmsForwarderService"
    android:foregroundServiceType="specialUse" />
```

---

## 4. 구현이 필요한 소스 파일

현재 프로젝트에는 `MainActivity.kt`와 theme 파일들만 존재한다. 다음 파일들을 **새로 생성**해야 한다.

### 4.1 핵심 기능

| 파일 | 역할 | 우선순위 |
|------|------|---------|
| `receiver/SmsReceiver.kt` | SMS 수신 감지 + 조건 필터링 + 전달 | 최우선 |
| `receiver/BootReceiver.kt` | 재부팅 시 서비스 자동 시작 | 높음 |
| `service/SmsForwarderService.kt` | Foreground Service로 백그라운드 유지 | 높음 |
| `repository/SettingsRepository.kt` | SharedPreferences 기반 설정 관리 | 높음 |

### 4.2 데이터 모델

| 파일 | 역할 |
|------|------|
| `model/ForwardRule.kt` | 전달 규칙 (발신번호, 키워드, 대상번호) |
| `model/ForwardLog.kt` | 전달 로그 (시간, 발신번호, 내용, 전달 결과) |

### 4.3 UI

| 파일 | 역할 |
|------|------|
| `ui/screens/MainScreen.kt` | 메인 설정 화면 (Compose) |

### 4.4 기존 파일 수정

| 파일 | 수정 내용 |
|------|----------|
| `MainActivity.kt` | Runtime 권한 요청, 기본 SMS 앱 설정, 배터리 최적화 예외 처리 추가 |

---

## 5. MainActivity.kt 현재 문제점

현재 `MainActivity.kt`는 Android Studio 기본 템플릿 그대로이다.

```kotlin
// 현재: "Hello Android" 텍스트만 표시
Greeting("Android")
```

SMS 전달 앱으로서 다음 로직이 필요하다:

1. **Runtime 권한 요청**: `RECEIVE_SMS`, `READ_SMS`, `SEND_SMS` (Android 6.0+)
2. **기본 SMS 앱 설정 요청**: Android 10+에서는 기본 SMS 앱이 아니면 SMS 수신 제한됨
3. **배터리 최적화 예외 요청**: 백그라운드 동작 안정성을 위해
4. **Notification 권한 요청**: Android 13+에서 `POST_NOTIFICATIONS` 필요
5. **MainScreen Composable 호스팅**: 실제 설정 UI 표시

---

## 6. 패키지 네이밍

현재 패키지명이 `com.example.smsforwarder`로 되어 있다. 개인 프로젝트용으로 `com.soyoon.smsforwarder`로 변경하는 것을 권장한다.

- `app/build.gradle`의 `namespace`, `applicationId`
- 모든 `.kt` 파일의 `package` 선언
- `AndroidManifest.xml`의 컴포넌트 참조

---

## 7. 구현 순서 제안

```
1단계: Gradle 버전 업그레이드 + 빌드 확인
   ↓
2단계: 데이터 모델 + SettingsRepository 생성
   ↓
3단계: SmsReceiver 구현 (핵심 기능)
   ↓
4단계: SmsForwarderService + BootReceiver 구현
   ↓
5단계: AndroidManifest.xml 권한/컴포넌트 등록
   ↓
6단계: MainActivity 권한 요청 로직 추가
   ↓
7단계: MainScreen UI 구현
   ↓
8단계: 에뮬레이터 테스트 (adb emu sms send)
```

---

## 8. 에뮬레이터 테스트 방법

프로젝트 완성 후 다음과 같이 테스트할 수 있다:

```bash
# SMS 수신 시뮬레이션
adb emu sms send 01012345678 "테스트 메시지입니다"

# 특정 키워드 포함 메시지
adb emu sms send 01012345678 "[인증] 인증번호는 123456 입니다"

# 로그 확인
adb logcat | grep SmsForwarder
```

---

## 9. 주의사항

- **기본 SMS 앱 설정**: Android 10+에서 SMS 수신을 안정적으로 받으려면 기본 SMS 앱으로 설정되어야 한다. 이를 위해 `RoleManager` 또는 `Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT` 활용이 필요하다.
- **Google Play 배포 불가**: SMS 접근 앱은 정책상 심사 거절 가능성이 높으므로 APK 직접 설치 방식으로 배포해야 한다.
- **제조사별 배터리 최적화**: Samsung, Xiaomi 등 제조사별로 추가적인 배터리 최적화 예외 설정이 필요할 수 있다.
