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

---

## Public 레포에 자동 배포 (Private → Public)

### 개요

개발 히스토리는 private 레포에만 유지하고, 태그를 push할 때 GitHub Actions가 자동으로 public 레포에 소스코드를 배포합니다.
Private 레포의 커밋 수십 개가 public 레포에는 단일 커밋(`v1.x.x release`)으로만 노출되어 개발 내역이 공개되지 않습니다.

```
v* 태그 push (private 레포)
         ↓
GitHub Actions 실행 (.github/workflows/publish.yml)
         ↓
squash merge → 단일 커밋으로 정리
         ↓
public 레포 main 브랜치에 push
```

> APK 빌드 워크플로우(`release.yml`)와 동일한 태그 트리거를 사용하므로, 태그 하나로 APK 빌드와 소스 배포가 동시에 실행됩니다.

---

### 최초 설정 (한 번만)

#### 1. Personal Access Token(PAT) 생성

Public 레포에 push할 권한을 가진 PAT을 발급합니다.

1. GitHub → **Settings → Developer settings → Personal access tokens → Tokens (classic)**
2. **Generate new token (classic)** 클릭
3. 권한(Scope) 선택: `repo` (또는 Fine-grained token의 경우 public 레포에 대한 `Contents: Read and write`)
4. 생성된 토큰 값을 복사해 안전하게 보관

#### 2. Private 레포 Secrets에 토큰 등록

Private 레포(현재 작업 중인 이 레포) → **Settings → Secrets and variables → Actions → New repository secret**

| Secret 이름 | 설명 |
|------------|------|
| `PUBLIC_REPO_TOKEN` | 위에서 생성한 PAT 값 |

#### 3. Public 레포 URL 확인

배포 대상 public 레포의 HTTPS URL을 확인합니다. 예: `https://github.com/soyoon247/SmsForwarder-public.git`

---

### 워크플로우 파일

Private 레포에 아래 파일을 생성합니다: `.github/workflows/publish.yml`

```yaml
name: Publish to public repo

on:
  push:
    tags:
      - 'v*'

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Push to public repo
        run: |
          git config user.email "actions@github.com"
          git config user.name "GitHub Actions"
          git remote add public https://x-access-token:${{ secrets.PUBLIC_REPO_TOKEN }}@github.com/soyoon247/SmsForwarder-public.git
          git checkout -b release
          git merge --squash main
          git commit -m "${{ github.ref_name }} release"
          git push public release:main --force
```

> `soyoon247/SmsForwarder-public.git` 부분을 실제 public 레포 경로로 교체하세요.

---

### 사용 방법

태그를 push하면 나머지는 자동입니다.

```bash
git tag v1.0.0
git push origin v1.0.0
```

이 한 줄로:
- `release.yml`: 서명된 APK 빌드 후 GitHub Releases 업로드
- `publish.yml`: Squash commit으로 정리 후 public 레포 main에 push

---

### 주의사항

- Public 레포 main 브랜치에는 항상 `--force` push 됩니다. Public 레포에서 직접 작업한 내용이 있으면 덮어씌워집니다.
- Public 레포의 커밋 히스토리는 `v1.x.x release` 단일 커밋만 남습니다. Private 레포의 세부 커밋은 공개되지 않습니다.
- PAT이 만료되거나 권한이 변경되면 워크플로우가 실패합니다. 주기적으로 토큰 유효기간을 확인하세요.
