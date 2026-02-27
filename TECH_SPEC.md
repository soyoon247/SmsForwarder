# SmsForwarder 기능 구현 테크 스펙

## 개요

특정 발신 번호 + 키워드 조건에 맞는 SMS를 감지하여 지정 번호로 자동 전달하는 Android 앱.
현재 프로젝트는 빌드/실행 가능한 빈 템플릿 상태이며, 아래 7단계를 순서대로 구현한다.

---

## 1단계: 데이터 모델 정의

### 목적
전달 규칙과 전달 로그를 표현하는 데이터 클래스 생성.

### 파일 생성

#### `model/ForwardRule.kt`
```kotlin
data class ForwardRule(
    val senderNumber: String,    // 감시할 발신 번호 (예: "01012345678")
    val keyword: String,         // 필터링 키워드 (예: "인증", 빈 문자열이면 키워드 필터 없음)
    val forwardTo: String,       // 전달 대상 번호
    val enabled: Boolean = true  // 규칙 활성화 여부
)
```

#### `model/ForwardLog.kt`
```kotlin
data class ForwardLog(
    val timestamp: Long,         // 수신 시각 (System.currentTimeMillis())
    val senderNumber: String,    // 원본 발신 번호
    val messageBody: String,     // SMS 내용
    val forwardedTo: String,     // 전달한 번호
    val success: Boolean         // 전달 성공 여부
)
```

### 패키지 경로
`app/src/main/java/com/soyoon/smsforwarder/model/`

---

## 2단계: SettingsRepository 구현

### 목적
SharedPreferences 기반으로 전달 규칙, ON/OFF 상태, 전달 로그를 저장·조회한다.

### 파일 생성

#### `repository/SettingsRepository.kt`

### 저장 항목

| 키 | 타입 | 설명 |
|---|------|------|
| `forwarding_enabled` | Boolean | 전체 기능 ON/OFF |
| `forward_rules` | String (JSON) | ForwardRule 목록을 JSON 직렬화하여 저장 |
| `forward_logs` | String (JSON) | ForwardLog 목록을 JSON 직렬화하여 저장 |

### 주요 메서드

```kotlin
class SettingsRepository(context: Context) {
    fun isForwardingEnabled(): Boolean
    fun setForwardingEnabled(enabled: Boolean)

    fun getForwardRules(): List<ForwardRule>
    fun saveForwardRules(rules: List<ForwardRule>)

    fun getForwardLogs(): List<ForwardLog>
    fun addForwardLog(log: ForwardLog)
    fun clearForwardLogs()
}
```

### JSON 직렬화
- `org.json.JSONArray` / `JSONObject` 사용 (Android SDK 내장, 추가 의존성 불필요)
- 규칙/로그 수가 소규모(수십 개 이하)이므로 SharedPreferences + JSON이면 충분

### 패키지 경로
`app/src/main/java/com/soyoon/smsforwarder/repository/`

---

## 3단계: SmsReceiver 구현

### 목적
SMS 수신 이벤트를 감지하고, 설정된 규칙에 따라 조건을 검사한 뒤 전달한다.

### 파일 생성

#### `receiver/SmsReceiver.kt`

### 동작 흐름

```
SMS_RECEIVED 브로드캐스트 수신
  → forwarding_enabled 확인 (OFF면 무시)
  → PDU에서 발신번호, 메시지 본문 추출
  → ForwardRule 목록 순회
    → 발신번호 일치 확인
    → 키워드 포함 확인 (빈 문자열이면 항상 통과)
    → 조건 만족 시 SmsManager.sendTextMessage() 호출
    → ForwardLog 저장
```

### 핵심 코드 구조

```kotlin
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val repo = SettingsRepository(context)
        if (!repo.isForwardingEnabled()) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        // 발신번호 + 본문 추출
        // 규칙 매칭
        // SmsManager.getDefault().sendTextMessage() 호출
        // 로그 저장
    }
}
```

### SMS 전달 시 주의사항
- `SmsManager.sendTextMessage()`의 5번째 파라미터 `sentIntent`로 전송 결과 확인
- 메시지가 160자 초과 시 `sendMultipartTextMessage()` 사용
- 전달 실패 시에도 로그에 `success = false`로 기록

### 패키지 경로
`app/src/main/java/com/soyoon/smsforwarder/receiver/`

---

## 4단계: Foreground Service + BootReceiver 구현

### 목적
- Foreground Service: 백그라운드에서 앱이 시스템에 의해 종료되지 않도록 유지
- BootReceiver: 기기 재부팅 후 자동으로 서비스를 시작

### 파일 생성

#### `service/SmsForwarderService.kt`

```kotlin
class SmsForwarderService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY  // 시스템이 종료해도 재시작
    }

    private fun createNotification(): Notification {
        // NotificationChannel 생성 (Android 8.0+, minSdk 26이므로 필수)
        // "SMS 전달 서비스 실행 중" 알림 표시
    }
}
```

- `foregroundServiceType`: `specialUse` (SMS 관련 특수 용도)
- Notification Channel ID: `"sms_forwarder_service"`

#### `receiver/BootReceiver.kt`

```kotlin
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val repo = SettingsRepository(context)
            if (repo.isForwardingEnabled()) {
                val serviceIntent = Intent(context, SmsForwarderService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
    }
}
```

### 패키지 경로
- `app/src/main/java/com/soyoon/smsforwarder/service/`
- `app/src/main/java/com/soyoon/smsforwarder/receiver/`

---

## 5단계: AndroidManifest.xml 권한 및 컴포넌트 등록

### 목적
SMS 수신/전송 권한, BroadcastReceiver, Service를 매니페스트에 등록한다.

### 추가할 권한

```xml
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### 추가할 컴포넌트

```xml
<!-- SMS 수신 감지 -->
<receiver android:name=".receiver.SmsReceiver"
    android:exported="true"
    android:permission="android.permission.BROADCAST_SMS">
    <intent-filter android:priority="999">
        <action android:name="android.provider.Telephony.SMS_RECEIVED" />
    </intent-filter>
</receiver>

<!-- 재부팅 시 서비스 자동 시작 -->
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

### 수정 대상 파일
`app/src/main/AndroidManifest.xml`

---

## 6단계: MainActivity 권한 요청 로직

### 목적
앱 실행 시 필요한 Runtime 권한을 요청하고, 기본 SMS 앱 설정 및 배터리 최적화 예외를 안내한다.

### 수정 대상 파일
`app/src/main/java/com/soyoon/smsforwarder/MainActivity.kt`

### 요청할 Runtime 권한 (Android 6.0+)

| 권한 | 용도 |
|------|------|
| `RECEIVE_SMS` | SMS 수신 감지 |
| `READ_SMS` | SMS 내용 읽기 |
| `SEND_SMS` | SMS 자동 전달 |
| `POST_NOTIFICATIONS` | Foreground Service 알림 (Android 13+) |

### 권한 요청 흐름

```
앱 시작
  → 권한 상태 확인
  → 미허용 권한이 있으면 ActivityResultLauncher로 요청
  → 권한 거부 시 기능 제한 안내 (Toast 또는 Dialog)
```

### 구현 방식
- `registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())` 사용
- Compose에서는 `rememberLauncherForActivityResult()` 활용

### 추가 요청 사항

#### 기본 SMS 앱 설정 (Android 10+)
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    val roleManager = getSystemService(RoleManager::class.java)
    if (!roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
        roleRequestLauncher.launch(intent)
    }
}
```
> 기본 SMS 앱으로 설정하지 않으면 Android 10+에서 SMS 수신이 불안정할 수 있음.
> 단, 기본 SMS 앱이 되면 SMS 수신/발신 UI도 제공해야 할 수 있어 복잡도가 증가하므로, 먼저 기본 SMS 앱 없이 테스트 후 필요 시 적용.

#### 배터리 최적화 예외
```kotlin
val pm = getSystemService(PowerManager::class.java)
if (!pm.isIgnoringBatteryOptimizations(packageName)) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
    intent.data = Uri.parse("package:$packageName")
    startActivity(intent)
}
```

---

## 7단계: MainScreen UI 구현

### 목적
사용자가 전달 규칙을 설정하고, 기능을 켜고 끄며, 전달 로그를 확인할 수 있는 화면.

### 파일 생성

#### `ui/screens/MainScreen.kt`

### 화면 구성

```
┌─────────────────────────────┐
│  SMS 자동 전달               │
│  ┌───────────────────────┐  │
│  │ [Switch] 전달 기능 ON  │  │
│  └───────────────────────┘  │
│                             │
│  ── 전달 규칙 ──────────────│
│  ┌───────────────────────┐  │
│  │ 발신번호: 01012345678  │  │
│  │ 키워드:   인증          │  │
│  │ 전달대상: 01098765432  │  │
│  │          [삭제]        │  │
│  └───────────────────────┘  │
│  [+ 규칙 추가]              │
│                             │
│  ── 전달 로그 ──────────────│
│  ┌───────────────────────┐  │
│  │ 10:30 01012345678     │  │
│  │ → 01098765432 ✓       │  │
│  │ "인증번호는 1234입니다" │  │
│  └───────────────────────┘  │
│  [로그 삭제]                │
└─────────────────────────────┘
```

### UI 컴포넌트 목록

| 컴포넌트 | 역할 |
|----------|------|
| ForwardingToggle | 전체 기능 ON/OFF Switch |
| ForwardRuleCard | 개별 전달 규칙 표시 (발신번호, 키워드, 대상번호, 삭제 버튼) |
| AddRuleDialog | 새 규칙 추가 다이얼로그 (TextField 3개 + 확인/취소) |
| ForwardLogItem | 개별 전달 로그 표시 (시간, 번호, 내용, 성공 여부) |

### 상태 관리
- `SettingsRepository`에서 직접 읽기/쓰기 (ViewModel 없이 단순하게 시작)
- `remember` + `mutableStateOf`로 UI 상태 관리
- 규칙/로그 변경 시 `SettingsRepository`에 즉시 저장

### 수정 대상 파일
- `MainActivity.kt`: `Greeting("Android")`를 `MainScreen()`으로 교체

### 패키지 경로
`app/src/main/java/com/soyoon/smsforwarder/ui/screens/`

---

## 최종 파일 구조

```
app/src/main/java/com/soyoon/smsforwarder/
├── MainActivity.kt                    (수정)
├── model/
│   ├── ForwardRule.kt                 (신규)
│   └── ForwardLog.kt                 (신규)
├── receiver/
│   ├── SmsReceiver.kt                (신규)
│   └── BootReceiver.kt               (신규)
├── repository/
│   └── SettingsRepository.kt         (신규)
├── service/
│   └── SmsForwarderService.kt        (신규)
└── ui/
    ├── screens/
    │   └── MainScreen.kt             (신규)
    └── theme/
        ├── Color.kt                   (기존)
        ├── Theme.kt                   (기존)
        └── Type.kt                    (기존)
```

---

## 에뮬레이터 테스트 방법

### SMS 수신 시뮬레이션
```bash
adb emu sms send 01012345678 "테스트 메시지"
adb emu sms send 01012345678 "[인증] 인증번호는 123456 입니다"
```

### 로그 확인
```bash
adb logcat -s SmsForwarder:* SmsReceiver:*
```

### 재부팅 테스트
```bash
adb reboot
# 부팅 완료 후 Foreground Service 알림 확인
```
