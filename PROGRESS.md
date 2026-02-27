# SmsForwarder 프로젝트 진행 현황

## 완료된 작업

### 1. Git 저장소 초기화
- Android Studio 기본 Compose 템플릿 상태에서 git init
- `.gitignore`에서 `.idea/` 전체 제외하도록 수정

### 2. Gradle / Kotlin / Compose 버전 업그레이드

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

### 3. 패키지명 변경
- `com.example.smsforwarder` → `com.soyoon.smsforwarder`
- 변경 대상: `build.gradle`(namespace, applicationId), 모든 `.kt` 파일의 package 선언, 디렉토리 구조

### 4. JDK 17 설정
- `brew install openjdk@17`로 JDK 17 설치
- `gradle.properties`에 `org.gradle.java.home` 설정 추가
- Android Studio도 Meerkat(2024.3.1)으로 업그레이드

### 5. 빌드 및 에뮬레이터 실행 확인
- `./gradlew assembleDebug` 빌드 성공
- `adb install` + `adb shell am start`로 에뮬레이터(Pixel 6 Pro API 33)에서 앱 실행 확인
- 현재 화면: "Hello Android!" (기본 템플릿 상태)

---

## 커밋 히스토리

| 커밋 | 내용 |
|------|------|
| `dd497d6` | chore: init project with Android Studio Compose template |
| `f20c5cf` | chore: upgrade Gradle, Kotlin, Compose versions |
| `bc04c3d` | refactor: rename package from com.example to com.soyoon |
| `8645028` | chore: configure JDK 17 for Gradle builds |

---

## 미구현 항목 (다음 단계)

PROJECT_REVIEW.md의 구현 순서 기준으로, 아래 항목들이 남아 있다.

| 단계 | 항목 | 상태 |
|------|------|------|
| 2단계 | 데이터 모델 (ForwardRule, ForwardLog) + SettingsRepository | 미구현 |
| 3단계 | SmsReceiver (BroadcastReceiver) | 미구현 |
| 4단계 | SmsForwarderService + BootReceiver | 미구현 |
| 5단계 | AndroidManifest.xml 권한/컴포넌트 등록 | 미구현 |
| 6단계 | MainActivity 권한 요청 로직 | 미구현 |
| 7단계 | MainScreen UI (Compose) | 미구현 |
| 8단계 | 에뮬레이터 테스트 | 미구현 |
