# 배포 가이드

## 개요

main 브랜치에 코드가 머지되면 GitHub Actions가 자동으로 APK를 빌드하고 GitHub Releases에 배포합니다.

## 자동 배포 흐름

```
main 브랜치에 push/merge
         ↓
GitHub Actions 실행 (.github/workflows/release.yml)
         ↓
versionCode = GITHUB_RUN_NUMBER (자동 증가 정수)
versionName = "1.{RUN_NUMBER}"
         ↓
서명된 Release APK 빌드
         ↓
GitHub Releases에 태그(v1.X) 및 APK 자동 업로드
```

---

## 최초 설정 (한 번만)

### 1. 키스토어 생성

로컬 환경에서 아래 명령어로 키스토어를 생성합니다.

```bash
keytool -genkey -v -keystore release.keystore \
  -alias smsforwarder -keyalg RSA -keysize 2048 -validity 10000 \
  -storetype PKCS12
```

> `-storetype PKCS12` 옵션으로 표준 포맷을 사용합니다. JKS 포맷 경고가 나오지 않습니다.

> 생성 시 입력한 비밀번호와 alias를 반드시 기록해두세요.

키스토어를 base64로 인코딩합니다.

```bash
# macOS
base64 -i release.keystore | tr -d '\n'

# Linux (GNU)
base64 -w 0 release.keystore

# Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore"))
```

> `release.keystore` 파일은 절대 저장소에 커밋하지 마세요.

---

### 2. GitHub Secrets 등록

GitHub 저장소 → **Settings → Secrets and variables → Actions → New repository secret**

| Secret 이름 | 설명 | 예시 |
|------------|------|------|
| `KEYSTORE_BASE64` | 키스토어 파일의 base64 인코딩 값 | `MIIE...` |
| `STORE_PASSWORD` | 키스토어 비밀번호 | `mypassword` |
| `KEY_ALIAS` | 키 별칭 | `smsforwarder` |
| `KEY_PASSWORD` | 키 비밀번호 | `mypassword` |

---

## 사용자 설치 방법

배포된 APK를 사용자가 설치하는 절차입니다.

1. GitHub Releases 페이지에서 최신 `.apk` 파일 다운로드
2. 안드로이드 설정 → **알 수 없는 출처의 앱 설치** 허용
   - Android 8.0 이상: 설정 → 앱 → 특별한 앱 접근 권한 → 알 수 없는 앱 설치
3. 다운로드된 APK 파일 실행 후 설치

---

## 수동 빌드 (로컬)

CI 없이 로컬에서 직접 빌드하려면:

```bash
export VERSION_CODE=1
export VERSION_NAME="1.0"
export KEYSTORE_PATH=/path/to/release.keystore
export STORE_PASSWORD=your_store_password
export KEY_ALIAS=smsforwarder
export KEY_PASSWORD=your_key_password

./gradlew assembleRelease
# 결과물: app/build/outputs/apk/release/app-release.apk
```

---

## 버저닝 규칙

| 항목 | 값 | 예시 |
|-----|-----|------|
| `versionCode` | GitHub Actions `run_number` | `42` |
| `versionName` | `"1.{run_number}"` | `"1.42"` |
| Git 태그 | `v{versionName}` | `v1.42` |

메이저 버전(`1.x`)을 올리려면 워크플로우의 `VERSION_NAME` 형식을 수정합니다.
