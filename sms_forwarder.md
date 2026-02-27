# SMS 자동 전달 앱 설계

## 1. 목적

특정 SMS를 감지하여 조건에 맞는 경우 다른 번호로 자동 전달하는 Android 앱 구현

---

## 2. 핵심 기능

### SMS 수신 기반 자동 전달

* 특정 발신 번호에서 온 SMS 감지
* SMS 내용에 특정 키워드 포함 시 전달
* 조건 만족 시 지정된 번호로 자동 전송

### 백그라운드 자동 동작

* 앱이 실행 중이지 않아도 SMS 수신 시 자동 처리

### 재부팅 후 자동 활성화

* 기기 재시작 후에도 기능 유지
* 최소 1회 앱 실행 시 자동 활성화 가능

### 설정 관리

* 감시 발신 번호 목록
* 전달 대상 번호
* 키워드 필터
* 기능 ON/OFF
* 전달 로그 저장

---

## 3. Android 구현 구조

### SMS 수신 처리

* BroadcastReceiver 사용
* SMS_RECEIVED 이벤트 감지

### 전달 조건 검사

* 발신 번호 필터
* 키워드 필터
* 설정 기반 ON/OFF

### SMS 전달

* SmsManager 사용하여 자동 전송

### 재부팅 자동 실행

* BOOT_COMPLETED 이벤트 수신

### 데이터 저장

* SharedPreferences 또는 Room DB 사용

---

## 4. 필수 권한 (Android)

* RECEIVE_SMS
* READ_SMS
* SEND_SMS
* RECEIVE_BOOT_COMPLETED

※ Android 10 이상에서는 기본 SMS 앱 설정 필요

---

## 5. 주요 제약 사항

### Android 시스템 제한

* SMS 읽기/전송은 기본 SMS 앱만 안정적으로 가능
* 백그라운드 실행 제한 존재
* 제조사별 배터리 최적화 영향 있음

### 보안 및 정책 제한

* SMS 자동 전달 기능은 악용 가능성 있음
* 권한 심사 매우 엄격

---

## 6. 플랫폼별 가능 여부

| 플랫폼     | 구현 가능성 | 비고          |
| ------- | ------ | ----------- |
| Android | 가능     | 기본 SMS 앱 필요 |
| iOS     | 불가능    | 시스템 접근 제한   |
| Web     | 불가능    | SMS 접근 불가   |

---

## 7. 스토어 배포 가능성

### Android 배포

* Google Play Store 정책상 SMS 접근 앱은 강하게 제한됨
* 자동 전달 앱은 심사 거절 가능성 높음
* 현실적 배포 방법: APK 직접 배포

### iOS 배포

* Apple App Store 정책상 기능 구현 및 배포 불가

---

## 8. 기술 스택

* Kotlin
* Android SDK
* BroadcastReceiver
* SmsManager
* Foreground Service (선택)
* SharedPreferences / Room

---

## 9. 개발 난이도 평가

| 항목          | 난이도    |
| ----------- | ------ |
| SMS 수신 처리   | 쉬움     |
| 조건 필터링      | 쉬움     |
| 자동 전달       | 보통     |
| 기본 SMS 앱 구현 | 어려움    |
| 스토어 배포      | 매우 어려움 |

---
