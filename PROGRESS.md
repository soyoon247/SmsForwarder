# SmsForwarder 프로젝트 진행 현황

## 완료된 작업

### Phase 1: 프로젝트 기본 세팅

#### 1. Git 저장소 초기화
- Android Studio 기본 Compose 템플릿 상태에서 git init
- `.gitignore`에서 `.idea/` 전체 제외하도록 수정

#### 2. Gradle / Kotlin / Compose 버전 업그레이드

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| AGP | 8.0.0 | 8.7.3 |
| Kotlin | 1.7.20 | 2.0.21 |
| Gradle Wrapper | 8.0 | 8.11.1 |
| Compose BOM | 2022.10.00 | 2024.12.01 |
| compileSdk / targetSdk | 33 | 35 |
| minSdk | 24 | 26 |
| JVM target | 1.8 | 17 |
| core-ktx | 1.8.0 | 1.15.0 |
| lifecycle-runtime-ktx | 2.3.1 | 2.8.7 |
| activity-compose | 1.5.1 | 1.9.3 |

- Kotlin 2.0+ 대응: `composeOptions` 블록 삭제, `kotlin-compose` 플러그인으로 대체

#### 3. 패키지명 변경
- `com.example.smsforwarder` → `com.soyoon.smsforwarder`
- 변경 대상: `build.gradle`(namespace, applicationId), 모든 `.kt` 파일의 package 선언, 디렉토리 구조

#### 4. JDK 17 설정
- `brew install openjdk@17`로 JDK 17 설치
- `gradle.properties`에 `org.gradle.java.home` 설정 추가
- Android Studio Meerkat(2024.3.1)으로 업그레이드

---

### Phase 2: 기능 구현 (TECH_SPEC_V2 기준)

#### 1단계: 데이터 모델 + 유틸리티
- `model/ForwardRule.kt`: 전달 규칙 (발신번호, 키워드, 대상번호, 활성화)
- `model/ForwardLog.kt`: 전달 로그 (시간, 발신번호, 내용, 대상, 성공여부)
- `util/PhoneNumberUtils.kt`: 전화번호 정규화 (공백/하이픈 제거, +82→0 변환)

#### 2단계: SettingsRepository
- SharedPreferences + JSON 직렬화로 규칙/로그 저장
- 로그 최대 100개 제한 (초과 시 오래된 것 자동 삭제)
- JSON 파싱 실패 시 빈 리스트 반환 (크래시 방지)

#### 3단계: SmsReceiver
- SMS_RECEIVED 브로드캐스트 수신 및 조건 기반 전달
- Multipart SMS 결합 처리
- 전화번호 정규화 후 규칙 매칭
- 중복 전달 방지 (sender+body+timestamp hash, 5분 캐시)
- 전달 루프 방지 (sender == forwardTo 차단)
- 분당 전달 10회 제한
- 160자 초과 시 sendMultipartTextMessage 사용

#### 4단계: AndroidManifest + 권한 요청
- AndroidManifest: RECEIVE_SMS, READ_SMS, SEND_SMS, POST_NOTIFICATIONS 권한 추가
- AndroidManifest: SmsReceiver 컴포넌트 등록 (exported=true, BROADCAST_SMS 권한)
- MainActivity: 앱 시작 시 Runtime 권한 요청 (SMS 3종 + Android 13 알림)
- MainActivity: 배터리 최적화 예외 요청

#### 5단계: MainScreen UI
- 전달 기능 ON/OFF 토글
- 전달 규칙 목록 (개별 활성/비활성, 삭제)
- 규칙 추가 다이얼로그 (발신번호/키워드/대상번호 입력, 유효성 검증, 루프 방지)
- 전달 로그 표시 (시간순, 성공/실패 색상 구분)
- 기기 제한 사항 안내 문구

---

## 최종 파일 구조

```
app/src/main/java/com/soyoon/smsforwarder/
├── MainActivity.kt
├── model/
│   ├── ForwardRule.kt
│   └── ForwardLog.kt
├── receiver/
│   └── SmsReceiver.kt
├── repository/
│   └── SettingsRepository.kt
├── util/
│   └── PhoneNumberUtils.kt
└── ui/
    ├── screens/
    │   └── MainScreen.kt
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

---

## 커밋 히스토리

| 커밋 | 내용 |
|------|------|
| `dd497d6` | chore: init project with Android Studio Compose template |
| `f20c5cf` | chore: upgrade Gradle, Kotlin, Compose versions |
| `bc04c3d` | refactor: rename package from com.example to com.soyoon |
| `8645028` | chore: configure JDK 17 for Gradle builds |
| `46e2667` | docs: add project progress report |
| `931ac8c` | docs: add detailed tech spec for remaining implementation |
| `b42a270` | docs: add tech spec v2 with feedback incorporated |
| `c0f6bb4` | feat: add data models and phone number utils |
| `fafa5dc` | feat: add SettingsRepository with SharedPreferences |
| `115fa58` | feat: add SmsReceiver with forwarding logic |
| `5866561` | feat: add SMS permissions and runtime permission requests |
| `0aa0aad` | feat: add MainScreen UI with rule management and log display |

---

## 실제 기기에 APK 설치하는 방법

### 방법 1: CLI에서 직접 설치 (USB 연결)

#### 사전 준비
1. 안드로이드 폰에서 **개발자 옵션** 활성화
   - 설정 → 휴대전화 정보 → 소프트웨어 정보 → **빌드번호 7회 탭**
2. **USB 디버깅** 켜기
   - 설정 → 개발자 옵션 → USB 디버깅 ON
3. USB 케이블로 Mac과 연결
4. 폰에 "USB 디버깅 허용" 팝업이 뜨면 **허용**

#### APK 빌드 및 설치
```bash
# 프로젝트 디렉토리에서 실행
cd /Users/soyoon/ToyProjects/SmsForwarder

# Release APK 빌드 (서명 없는 debug 버전)
./gradlew assembleDebug

# 연결된 기기 확인
adb devices

# 설치
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 앱 실행
adb shell am start -n com.soyoon.smsforwarder/.MainActivity
```

### 방법 2: APK 파일을 폰으로 전송하여 설치

#### APK 빌드
```bash
./gradlew assembleDebug
```

#### APK 파일 위치
```
app/build/outputs/apk/debug/app-debug.apk
```

#### 전송 및 설치
1. 위 APK 파일을 카카오톡, 이메일, Google Drive 등으로 폰에 전송
2. 폰에서 파일을 열기 전에 **출처를 알 수 없는 앱 설치** 허용
   - 설정 → 앱 → 특별한 앱 액세스 → 출처를 알 수 없는 앱 설치
   - 파일을 열 앱(카카오톡, 파일관리자 등)에 대해 허용
3. APK 파일 열어서 설치

### 방법 3: 무선 디버깅 (USB 없이)

#### 사전 준비
- Mac과 폰이 같은 Wi-Fi에 연결되어 있어야 함
- Android 11 이상 필요

#### 연결
1. 폰: 설정 → 개발자 옵션 → **무선 디버깅** ON
2. 폰: 무선 디버깅 → **페어링 코드로 기기 페어링** 탭
3. Mac 터미널:
   ```bash
   # 페어링 (폰에 표시된 IP:포트와 코드 입력)
   adb pair <IP:포트>
   # 페어링 코드 입력

   # 연결
   adb connect <IP:포트>
   ```
4. 이후 방법 1과 동일하게 `adb install` 실행

### 설치 후 필수 설정

1. **권한 허용**: 앱 최초 실행 시 SMS 수신/읽기/전송, 알림 권한 허용
2. **배터리 최적화 해제**: 자동으로 요청되지만, 수동으로도 확인 권장
   - 설정 → 배터리 → 배터리 최적화 → SmsForwarder → 최적화하지 않음
3. **기본 SMS 앱 설정 (선택)**: 일부 기기에서 SMS 수신이 불안정할 경우
   - 설정 → 앱 → 기본 앱 → SMS 앱 → SmsForwarder 선택

### 참고: 서명된 Release APK 빌드 (선택)

debug APK는 디버그 서명이 포함되어 있어 설치·테스트에 문제없지만,
장기 사용이나 타인 배포 시에는 서명된 release APK가 권장된다.

```bash
# keystore 생성 (최초 1회)
keytool -genkey -v -keystore smsforwarder-release.keystore \
  -alias smsforwarder -keyalg RSA -keysize 2048 -validity 10000

# app/build.gradle에 signingConfigs 추가 후
./gradlew assembleRelease
```
