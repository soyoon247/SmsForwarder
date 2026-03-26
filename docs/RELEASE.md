# 배포 가이드

## 개요

private 레포(이 레포)에서 개발하고, 태그를 push하면 GitHub Actions가 자동으로 APK를 빌드한 뒤 public 레포에 소스코드와 Release를 배포합니다.
개발 히스토리는 private 레포에만 남고, public 레포에는 단일 squash 커밋만 노출됩니다.

## 자동 배포 흐름

```
v* 태그 push (private 레포)
         ↓
GitHub Actions 실행 (.github/workflows/release.yml)
         ↓
서명된 Release APK 빌드
         ↓
소스코드 squash → public 레포 main 브랜치에 push
         ↓
APK를 public 레포 GitHub Releases에 업로드
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
| `PUBLIC_REPO_TOKEN` | Public 레포에 push/release 권한이 있는 PAT | `ghp_...` |

`PUBLIC_REPO_TOKEN` 발급 방법: GitHub → **Settings → Developer settings → Personal access tokens → Tokens (classic)** → `repo` scope 선택

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

---

## Public 레포에 자동 배포 (Private → Public)

APK 빌드와 public 레포 배포는 `release.yml` 하나로 통합되어 있습니다.
태그를 push하면 아래가 순서대로 실행됩니다.

1. APK 빌드 (키스토어는 private 레포 Secrets 사용)
2. 소스코드 squash → public 레포 main에 push (개발 히스토리 비공개)
3. APK를 public 레포 GitHub Releases에 업로드

### Public 레포 경로 변경

`release.yml`에서 `soyoon247/SmsForwarder-public` 두 곳을 실제 public 레포 경로로 교체합니다.

```yaml
# Push source to public repo
git remote add public https://...@github.com/soyoon247/SmsForwarder-public.git  # ← 교체

# Create GitHub Release
repository: soyoon247/SmsForwarder-public  # ← 교체
```

### 주의사항

- Public 레포 main 브랜치에는 항상 `--force` push 됩니다. Public 레포에서 직접 작업한 내용이 있으면 덮어씌워집니다.
- Public 레포의 커밋 히스토리는 `v1.x.x release` 단일 커밋만 남습니다. Private 레포의 세부 커밋은 공개되지 않습니다.
- `PUBLIC_REPO_TOKEN`이 만료되거나 권한이 변경되면 워크플로우가 실패합니다. 주기적으로 토큰 유효기간을 확인하세요.
