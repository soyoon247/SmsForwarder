# SmsForwarder

수신된 SMS를 설정한 규칙에 따라 다른 번호로 자동 전달하는 안드로이드 앱입니다.

## 주요 기능

- **전달 규칙 관리** - 발신번호, 키워드 조건으로 전달 규칙 설정
- **전달 ON/OFF** - 전체 전달 기능을 스위치로 간편하게 제어
- **전달 로그** - 성공/실패 이력 확인
- **백그라운드 동작** - 배터리 최적화 예외 처리로 안정적인 수신

## 스크린샷

> (추후 추가 예정)

## 요구사항

- Android 8.0 (API 26) 이상
- SMS 수신 권한 허용 필요

## 설치

### 다운로드 (일반 사용자)

1. [Releases](../../releases) 페이지에서 최신 `.apk` 파일 다운로드
2. 안드로이드 설정 → **알 수 없는 출처의 앱 설치** 허용
3. 다운로드된 APK 실행 후 설치

자세한 설치 및 배포 방법은 [docs/RELEASE.md](docs/RELEASE.md)를 참고하세요.

### 직접 빌드

```bash
git clone https://github.com/soyoon247/SmsForwarder.git
cd SmsForwarder
./gradlew assembleDebug
# 결과물: app/build/outputs/apk/debug/app-debug.apk
```

## 기술 스택

- **언어**: Kotlin
- **UI**: Jetpack Compose + Material3
- **최소 SDK**: API 26 (Android 8.0)
- **빌드**: Gradle 8.x

## 자동 배포

main 브랜치에 머지될 때마다 GitHub Actions가 자동으로 APK를 빌드하고 GitHub Releases에 배포합니다.

```
main 브랜치 push → GitHub Actions → 서명된 APK 빌드 → GitHub Releases 자동 업로드
```

배포 설정 방법: [docs/RELEASE.md](docs/RELEASE.md)

## 주의사항 / Troubleshooting

일부 기기에서는 제조사 정책으로 인해 SMS 수신이 제한될 수 있습니다.
정상 동작하지 않을 경우 아래 설정을 확인하세요.

### 한국어

1. **배터리 최적화 해제**
   설정 → 앱 → 해당 앱 → 배터리 → "제한 없음" 선택

2. **사용하지 않는 앱 관리 해제**
   설정 → 앱 → 해당 앱 → "사용하지 않는 앱 관리" 토글 끄기

3. **자동 절전 예외 앱 등록**
   설정 → 배터리 → 백그라운드 앱 사용 제한 → "자동 절전 예외 앱"에 해당 앱 추가

### English

1. **Disable Battery Optimization**
   Settings → Apps → Select the app → Battery → Choose "Unrestricted"

2. **Disable Manage app if unused**
   Settings → Apps → Select the app → Turn off "Remove permissions if app is unused"

3. **Add to Never auto sleeping apps**
   Settings → Battery → Background usage limits → Add the app to "Never auto sleeping apps"

## 라이선스

MIT
