# SmsForwarder 기능 구현 테크 스펙 v2

## 개요

특정 발신 번호 + 키워드 조건에 맞는 SMS를 감지하여 지정 번호로 자동 전달하는 Android 앱.
현재 프로젝트는 빌드/실행 가능한 빈 템플릿 상태이며, 아래 5단계를 순서대로 구현한다.

### v1 대비 주요 변경점

- Foreground Service / BootReceiver 제거 (BroadcastReceiver 단독 처리)
- 전화번호 정규화 로직 추가
- Multipart SMS 수신 결합 처리 명시
- 중복 전달 방지 (hash 기반)
- 전달 루프 방지
- 로그 최대 100개 제한
- 분당 전달 횟수 제한
- Manifest priority 제거 (Android 8+ 무의미)

---

## 1단계: 데이터 모델 + 유틸리티

### 파일 생성

#### `model/ForwardRule.kt`
```kotlin
data class ForwardRule(
    val senderNumber: String,    // 감시할 발신 번호 (정규화된 형태: "01012345678")
    val keyword: String,         // 필터링 키워드 (빈 문자열이면 키워드 필터 없음)
    val forwardTo: String,       // 전달 대상 번호
    val enabled: Boolean = true  // 개별 규칙 활성화 여부
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

#### `util/PhoneNumberUtils.kt`

전화번호 정규화 유틸리티. SMS 발신번호는 다양한 형식으로 수신되므로 통일된 형태로 변환한다.

```kotlin
object PhoneNumberUtils {
    fun normalize(number: String): String {
        // 1. 공백, 하이픈, 괄호 제거
        // 2. 국가번호 +82 → 0 변환
        // 예: "+82-10-1234-5678" → "01012345678"
        //     "010 1234 5678" → "01012345678"
    }
}
```

변환 규칙:
- 공백, `-`, `(`, `)` 제거
- `+82` 접두사 → `0`으로 대체
- 결과는 숫자만으로 구성된 문자열

### 패키지 경로
- `app/src/main/java/com/soyoon/smsforwarder/model/`
- `app/src/main/java/com/soyoon/smsforwarder/util/`

---

## 2단계: SettingsRepository

### 목적
SharedPreferences 기반으로 전달 규칙, ON/OFF 상태, 전달 로그를 저장·조회한다.

### 파일 생성

#### `repository/SettingsRepository.kt`

### 저장 항목

| 키 | 타입 | 설명 |
|---|------|------|
| `forwarding_enabled` | Boolean | 전체 기능 ON/OFF |
| `forward_rules` | String (JSON) | ForwardRule 목록 |
| `forward_logs` | String (JSON) | ForwardLog 목록 |

### 주요 메서드

```kotlin
class SettingsRepository(context: Context) {
    companion object {
        const val MAX_LOG_COUNT = 100
    }

    fun isForwardingEnabled(): Boolean
    fun setForwardingEnabled(enabled: Boolean)

    fun getForwardRules(): List<ForwardRule>
    fun saveForwardRules(rules: List<ForwardRule>)

    fun getForwardLogs(): List<ForwardLog>
    fun addForwardLog(log: ForwardLog)   // MAX_LOG_COUNT 초과 시 오래된 로그 자동 삭제
    fun clearForwardLogs()
}
```

### 로그 개수 제한
- `addForwardLog()` 호출 시 저장된 로그가 100개를 초과하면 오래된 것부터 삭제
- timestamp 기준 오름차순 정렬, 초과분은 앞에서 제거

### JSON 직렬화
- `org.json.JSONArray` / `JSONObject` 사용 (Android SDK 내장, 추가 의존성 불필요)
- 직렬화/역직렬화 실패 시 빈 리스트 반환 (앱 크래시 방지)

### 패키지 경로
`app/src/main/java/com/soyoon/smsforwarder/repository/`

---

## 3단계: SmsReceiver

### 목적
SMS 수신 이벤트를 감지하고, 설정된 규칙에 따라 조건을 검사한 뒤 전달한다.

### 파일 생성

#### `receiver/SmsReceiver.kt`

### 동작 흐름

```
SMS_RECEIVED 브로드캐스트 수신
  → forwarding_enabled 확인 (OFF면 무시)
  → Multipart 메시지 결합 (여러 PDU의 body를 하나로 합침)
  → 발신번호 정규화 (PhoneNumberUtils.normalize())
  → 중복 전달 체크 (sender + body + timestamp hash)
  → 분당 전달 횟수 체크
  → ForwardRule 목록 순회
    → 규칙 enabled 확인
    → 발신번호 일치 확인 (정규화된 번호 기준)
    → 키워드 포함 확인 (빈 문자열이면 항상 통과)
    → 전달 루프 체크 (forwardTo ≠ senderNumber)
    → 조건 만족 시 SmsManager로 전달
    → ForwardLog 저장
```

### 핵심 구현 사항

#### Multipart SMS 결합
```kotlin
val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
val sender = messages[0].originatingAddress ?: return
val body = messages.joinToString("") { it.messageBody ?: "" }
```

#### 중복 전달 방지
```kotlin
private val recentMessages = LinkedHashMap<String, Long>()  // hash → timestamp

private fun isDuplicate(sender: String, body: String, timestamp: Long): Boolean {
    val hash = "$sender|$body|$timestamp".hashCode().toString()
    // 5분 이내 동일 hash가 있으면 중복으로 판단
    // 오래된 캐시 항목 정리
}
```
- `sender + body + timestamp` 기반 hash 생성
- 최근 5분 이내 처리된 메시지와 비교
- companion object에 캐시 유지 (BroadcastReceiver 인스턴스는 매번 새로 생성되므로)

#### 전달 루프 방지
```kotlin
val normalizedSender = PhoneNumberUtils.normalize(sender)
val normalizedForwardTo = PhoneNumberUtils.normalize(rule.forwardTo)
if (normalizedSender == normalizedForwardTo) {
    // 전달 건너뛰기 (로그 남김)
}
```

#### 분당 전달 횟수 제한
```kotlin
companion object {
    const val MAX_FORWARDS_PER_MINUTE = 10
    private val forwardTimestamps = mutableListOf<Long>()
}
```
- 1분 이내 전달 횟수가 10회를 초과하면 전달 차단
- 과도한 SMS 수신 시 시스템 부하 및 통신사 차단 방지

#### SMS 전달
```kotlin
val smsManager = SmsManager.getDefault()
if (body.length > 160) {
    val parts = smsManager.divideMessage(body)
    smsManager.sendMultipartTextMessage(rule.forwardTo, null, parts, null, null)
} else {
    smsManager.sendTextMessage(rule.forwardTo, null, body, null, null)
}
```

### 패키지 경로
`app/src/main/java/com/soyoon/smsforwarder/receiver/`

---

## 4단계: AndroidManifest.xml + MainActivity 권한 요청

### 4-1. AndroidManifest.xml

#### 추가할 권한
```xml
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

> v1 대비 제거: `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`

#### 추가할 컴포넌트
```xml
<receiver android:name=".receiver.SmsReceiver"
    android:exported="true"
    android:permission="android.permission.BROADCAST_SMS">
    <intent-filter>
        <action android:name="android.provider.Telephony.SMS_RECEIVED" />
    </intent-filter>
</receiver>
```

> v1 대비 변경:
> - `android:priority="999"` 제거 (Android 8+ 무의미)
> - `exported="true"` 유지 (시스템 브로드캐스트 수신에 필수. `false`로 설정하면 SMS_RECEIVED를 수신할 수 없음)
> - BootReceiver, SmsForwarderService 등록 제거

#### 수정 대상 파일
`app/src/main/AndroidManifest.xml`

---

### 4-2. MainActivity 권한 요청

#### 수정 대상 파일
`app/src/main/java/com/soyoon/smsforwarder/MainActivity.kt`

#### 요청할 Runtime 권한

| 권한 | 용도 |
|------|------|
| `RECEIVE_SMS` | SMS 수신 감지 |
| `READ_SMS` | SMS 내용 읽기 |
| `SEND_SMS` | SMS 자동 전달 |
| `POST_NOTIFICATIONS` | 알림 표시 (Android 13+) |

#### 권한 요청 흐름

```
앱 시작 (onCreate)
  → 미허용 권한 필터링
  → 미허용 권한이 있으면 requestMultiplePermissions 실행
  → 결과 콜백에서 거부된 권한이 있으면 안내 표시
```

#### 구현 방식
- `rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions())` 사용
- `LaunchedEffect(Unit)`에서 권한 체크 후 요청 트리거

#### 배터리 최적화 예외 요청
```kotlin
val pm = getSystemService(PowerManager::class.java)
if (!pm.isIgnoringBatteryOptimizations(packageName)) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
    intent.data = Uri.parse("package:$packageName")
    startActivity(intent)
}
```

#### 기본 SMS 앱 설정
초기 구현에서는 강제하지 않는다. UI에 안내 문구만 표시:
> "일부 기기에서 SMS 수신이 제한될 수 있습니다. 정상 동작하지 않을 경우 설정에서 기본 SMS 앱을 변경해주세요."

---

## 5단계: MainScreen UI

### 목적
사용자가 전달 규칙을 설정하고, 기능을 켜고 끄며, 전달 로그를 확인할 수 있는 화면.

### 파일 생성

#### `ui/screens/MainScreen.kt`

### 화면 구성

```
┌─────────────────────────────┐
│  SMS 자동 전달               │
│                             │
│  ⚠ 안내                     │
│  일부 기기에서 SMS 수신이     │
│  제한될 수 있습니다.          │
│  배터리 최적화를 해제해주세요. │
│                             │
│  ┌───────────────────────┐  │
│  │ [Switch] 전달 기능 ON  │  │
│  └───────────────────────┘  │
│                             │
│  ── 전달 규칙 ──────────────│
│  ┌───────────────────────┐  │
│  │ 발신번호: 01012345678  │  │
│  │ 키워드:   인증          │  │
│  │ 전달대상: 01098765432  │  │
│  │    [ON/OFF]    [삭제]  │  │
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
| NoticeSection | 기기 제한 사항 안내 문구 |
| ForwardingToggle | 전체 기능 ON/OFF Switch |
| ForwardRuleCard | 개별 규칙 표시 (발신번호, 키워드, 대상번호, 개별 ON/OFF, 삭제) |
| AddRuleDialog | 새 규칙 추가 다이얼로그 (TextField 3개 + 확인/취소) |
| ForwardLogItem | 개별 전달 로그 (시간, 번호, 내용, 성공 여부) |

### 규칙 추가 시 검증

| 검증 항목 | 조건 |
|-----------|------|
| 발신번호 | 빈 문자열 불가, 숫자/+/- 만 허용 |
| 전달대상 | 빈 문자열 불가, 숫자/+/- 만 허용 |
| 루프 방지 | 정규화 후 발신번호 == 전달대상이면 등록 차단 |
| 키워드 | 빈 문자열 허용 (필터 없이 전부 전달) |

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
│   └── SmsReceiver.kt                (신규)
├── repository/
│   └── SettingsRepository.kt         (신규)
├── util/
│   └── PhoneNumberUtils.kt           (신규)
└── ui/
    ├── screens/
    │   └── MainScreen.kt             (신규)
    └── theme/
        ├── Color.kt                   (기존)
        ├── Theme.kt                   (기존)
        └── Type.kt                    (기존)
```

> v1 대비 제거: `receiver/BootReceiver.kt`, `service/SmsForwarderService.kt`
> v1 대비 추가: `util/PhoneNumberUtils.kt`

---

## 에뮬레이터 테스트 방법

### SMS 수신 시뮬레이션
```bash
# 기본 테스트
adb emu sms send 01012345678 "테스트 메시지"

# 키워드 필터 테스트
adb emu sms send 01012345678 "[인증] 인증번호는 123456 입니다"

# 국가번호 형식 테스트 (정규화 확인)
adb emu sms send +821012345678 "국가번호 형식 테스트"
```

### 로그 확인
```bash
adb logcat -s SmsForwarder:* SmsReceiver:*
```

---

## 확장 시 고려 사항 (현재 구현 범위 밖)

다음 항목은 초기 구현에 포함하지 않으며, 추후 필요 시 적용한다.

| 항목 | 설명 |
|------|------|
| Room Database | 로그/규칙이 대량으로 증가할 경우 SharedPreferences → Room 전환 |
| ViewModel | UI 복잡도 증가 시 UI → ViewModel → Repository 구조로 전환 |
| 기본 SMS 앱 설정 | 일부 기기에서 수신 불안정 시 RoleManager로 기본 SMS 앱 설정 지원 |
| 전달 실패 재시도 | 네트워크 불안정 시 재시도 정책 |
| Foreground Service | 제조사별 백그라운드 제한이 심한 기기 대응 시 재도입 검토 |
