# MultiRoute Downloader 4.0 — 한국어 안내서

[English](en.md) · [Русский](ru.md) · [Deutsch](de.md) · [Español](es.md) · [Français](fr.md) · [中文](zh.md) · [한국어](ko.md) · [العربية](ar.md) · [Italiano](it.md) · [Home](../README.md) · [日本語](ja.md) · [हिन्दी](hi.md) · [Tiếng Việt](vi.md)

프로젝트 소유자: [2018-spec-fdsds](https://github.com/2018-spec-fdsds).

## 1. 용도와 동작

MultiRoute Downloader는 같은 로컬 Ethernet 네트워크의 여러 인터넷 **게이트웨이**로 파일을 나누어 받는 Java 데스크톱 프로그램입니다. 예를 들어 유선 인터넷과 모바일 4G/5G 회선을 함께 사용할 수 있습니다. 선택한 사용 가능한 게이트웨이가 공유 대기열에서 조각을 가져가므로 빠른 회선이 더 많은 조각을 처리합니다. 회선 하나가 끊겨도 나머지는 계속하고, 미완료 조각은 대기열로 돌아가며 검증된 데이터는 보존됩니다. URL 목록 일괄 가져오기와 다른 프로그램을 위한 로컬 API도 제공합니다.

속도 향상은 독립적인 상위 회선 용량, 서버 제한과 프로토콜의 안전한 이어받기 지원에 달려 있습니다. 게이트웨이 수만큼 항상 빨라지지는 않습니다. 웹 인터페이스가 아닌 독립 데스크톱 앱입니다. 새 프로필의 기본 언어는 영어이며 러시아어, 독일어, 스페인어, 프랑스어, 중국어, 한국어, 아랍어, 이탈리아어, 일본어, 힌디어, 베트남어도 선택할 수 있습니다. 선택한 언어는 저장되고 등록된 상태/로그/도움말에 적용됩니다. 외부 서버나 라이브러리의 원문 오류는 번역되지 않을 수 있습니다. 파일명, 경로, URL, JSON 키는 변경하지 않습니다.

## 2. 필요한 컴퓨터 환경

| 플랫폼 | 운영체제와 CPU | 구성 요소 |
| --- | --- | --- |
| Windows | Windows 11 x64; ARM64 패키지는 제공하지 않음 | 기본 설치 프로그램에 Java 21과 x64 네트워크 구성 요소 포함; Npcap은 별도 설치 |
| Apple Silicon Mac | M1/M2/M3/M4 이상, macOS 26+ | arm64 PKG에 Java와 컴파일된 네트워크 구성 요소 포함 |
| Intel Mac | x86_64, macOS 15+ | x86_64 PKG에 Java와 컴파일된 구성 요소 포함 |
| Linux | glibc 배포판, x86_64 또는 ARM64; torrent/magnet은 glibc 2.38+ | 완전한 Java 17+, sudo, C 컴파일러, binutils, pkg-config, libpcap 개발 파일; GUI는 X11 또는 Wayland+XWayland 및 글꼴 라이브러리 필요 |

Java에는 `java.desktop`, `java.net.http`, `jdk.httpserver`, `java.xml` 모듈이 필요하며 `--headless`도 예외가 아닙니다. Windows/macOS 기본 설치 패키지는 Java를 따로 설치할 필요가 없습니다. 구형 portable ZIP 3.0과 Linux 배포는 필요 시 공식 출처에서 전용 Java를 내려받습니다. 서로 다른 배포의 일부 파일만 섞지 마십시오.

선택한 모든 게이트웨이와 같은 IPv4 서브넷에 연결된 **유선 Ethernet** 어댑터가 필요합니다. 각 라우터는 앱의 가상 주소에 인터넷 사용을 허용해야 합니다. Wi-Fi만, VPN만 또는 IPv6만 있는 구성은 이 게이트웨이 전송 방식의 지원 구성이 아닙니다. 편의를 위해 RAM 4 GB 이상을 권장하지만 측정되거나 강제되는 최소치는 아닙니다. GPU/CUDA/AI 모델은 필요 없습니다. 앱, 데이터, 다운로드 폴더는 쓰기 가능해야 합니다. 배포 확인에는 최소 512 MiB 여유 공간이 필요하고, 실제 파일과 임시 조각은 별도입니다. torrent는 전체 조각 경계와 대상/캐시 공간을 여유 있게 잡으십시오.

선택한 언어의 문자를 포함한 글꼴이 시스템에 설치되어야 합니다. 최소 Linux에서는 배포판에서 중국어/한국어용 Noto CJK와 아랍어 Noto 계열 같은 적절한 글꼴을 설치하십시오. Java는 플랫폼 글꼴 대체 기능을 사용하며 앱이 글꼴 팩을 설치하거나 번들하지 않습니다. 문자 지원 범위는 컴퓨터마다 다를 수 있습니다.

## 3. 설치와 보안 확인

[4.0 릴리스](https://github.com/2018-spec-fdsds/multiroute-downloader/releases/tag/v4.0)에 첨부된 경우 시스템에 맞는 하나의 설치 파일을 받고 공개 SHA-256과 비교합니다.

- Windows: `MultiRoute-Downloader-4.0-Windows-x64-Setup.exe`.
- Apple Silicon, macOS 26+: `MultiRoute-Downloader-4.0-macOS-arm64.pkg`.
- Intel, macOS 15+: `MultiRoute-Downloader-4.0-macOS-x86_64.pkg`.

### Windows

EXE를 열어 현재 사용자용 설치를 마치고 시작 메뉴나 바탕 화면 바로 가기로 실행합니다. 명령 스크립트 창으로 실행되지 않습니다. 자체 설치 화면은 영어이고 Windows 보안 창은 운영체제 언어를 사용합니다.

첫 실행에서 Npcap을 확인합니다. 없으면 앱에서 다운로드에 동의한 뒤 고정된 공식 설치 파일의 SHA-256과 Authenticode 게시자를 검증하고 Npcap의 정상 설치 프로그램을 엽니다. 라이선스를 읽고 표준 UAC 요청을 승인하십시오. 재시작이 필요하면 Windows를 재부팅한 뒤 다시 엽니다. Npcap은 앱에 번들되거나 조용히 설치되지 않으며, 없는 경우 이 단계에 인터넷이 필요합니다. AdminOnly 드라이버는 추가 캡처 권한을 요구할 수 있지만 Java 전체를 관리자 권한으로 실행하지는 마십시오.

앱과 설치 파일에는 게시자 서명이 없습니다. 출처와 해시를 확인한 경우에만 SmartScreen의 “추가 정보 → 실행” 선택 여부를 판단하십시오. 악성코드 탐지는 단순한 알 수 없는 게시자 알림과 다릅니다. 백신을 끄거나 예외를 추가하지 마십시오.

### macOS

CPU에 맞는 PKG를 Apple Installer에서 열고 보호된 네트워크 구성 요소 설치를 위한 관리자 요청을 승인합니다. `/Applications/MultiRoute Downloader.app`을 Finder/Spotlight/Dock에서 엽니다. 이 기본 패키지에는 별도 Java, Terminal, Command Line Tools, Homebrew가 필요 없습니다.

Ethernet BPF 장치를 열어야 할 때 정상적인 macOS 관리자 인증이 나타납니다. 네트워크 구성 요소는 캡처 장치를 연 뒤 권한을 낮추고 Java 창은 일반 사용자로 실행됩니다. PKG와 앱은 **Developer ID 서명 및 공증이 없습니다**. 개발자를 확인할 수 없다는 알림에 예외가 제공된다면 출처와 SHA-256을 검증한 후에만 **시스템 설정 → 개인정보 보호 및 보안 → 확인 없이 열기**, 이어서 “열기”를 선택하십시오. [Apple 공식 안내](https://support.apple.com/en-us/102445)를 참고하고 관리형 Mac의 제한을 따릅니다.

설치 승인, 이후 네트워크 권한, 미확인 개발자 예외는 별개입니다. 요청이 나타나면 실제 LAN 게이트웨이를 위한 **로컬 네트워크**와 선택한 다운로드 폴더 접근을 허용합니다. Gatekeeper/SIP를 끄거나 격리 속성을 전체 제거하지 말고, Java 전체를 root로 실행하지 마십시오. 악성/손상 파일 경고는 일반 개발자 알림이 아니므로 중지하고 파일을 확인합니다.

### Linux 및 portable ZIP 3.0

완전한 ZIP을 풀고 Linux 최초에는 `bash ./Deploy-Linux.sh`, 이후에는 `bash ./Start-Linux.sh`를 실행합니다. Windows portable은 `Deploy-Windows.cmd` 다음 `Start-Windows.cmd`, macOS portable은 `Deploy-macOS.command`와 `Start-macOS.command`를 사용합니다. 구형 ZIP은 스크립트 실행 방식이며 4.0 기본 설치 패키지와 별개입니다. Linux에서는 배포판의 컴파일러/libpcap 의존성이 필요하고 macOS portable은 Apple Command Line Tools와 sudo를 요청할 수 있습니다. 해당 권한 터미널을 유지하고 전체 Java를 root로 실행하지 마십시오. Alpine/musl은 포함되지 않으며 배포가 운영체제나 glibc를 교체하지 않습니다.

설치에는 열두 언어의 오프라인 설명서가 포함됩니다. Windows: `%LOCALAPPDATA%/MultiRoute Downloader App/app/docs/`. macOS: 앱의 “패키지 내용 보기”에서 `Contents/app/docs/ko.md`를 엽니다. 브라우저의 릴리스 설명서 링크는 인터넷이 필요합니다.

## 4. 게이트웨이와 가상 IP

기본 주소는 `192.168.1.1`, `192.168.1.2`, `192.168.1.3`, `192.168.1.4`이며 모두 선택됩니다. 주소를 수정하거나 게이트웨이 추가로 **1–32**개의 중복 없는 주소를 관리합니다. 컴퓨터가 하나를 기본 게이트웨이로 사용해도 다섯 번째 회선이 추가되는 것은 아닙니다.

앱의 가상 IPv4는 처음에 `192.168.1.70`입니다. 네트워크 설정에서 확인/적용하고 다른 장치가 사용하지 않는지 검사합니다. DHCP 자동 할당 범위 밖에 예약하십시오. 이 주소는 앱 내부 전송 전용이며 **시스템 어댑터에 추가하거나 OS 라우팅 테이블을 변경하지 않습니다**. 다른 앱은 평소 연결을 사용합니다. OS 네트워크 설정에 직접 이 IP를 넣지 마십시오. 옆의 물음표와 도움말을 참고합니다.

게이트웨이를 검사한 다음 하나/여러 개/전체를 선택합니다. 속도 테스트는 **16 MiB 또는 5초**까지이며 연결 준비는 더 걸릴 수 있습니다. 정상 다운로드에는 이 제한이 적용되지 않습니다. 적당한 직접 HTTP URL이 없으면 Selectel 테스트 파일을 씁니다.

진행 중에도 프로필과 해당 작업의 게이트웨이를 추가/제거할 수 있지만 미완료 작업마다 최소 하나가 남아야 합니다. 새 프로필은 기존 작업에 자동 선택되지 않습니다. 가상 IP 자체를 바꾸기 전에는 활동 작업을 일시 중지합니다. MAC 주소는 사용자 화면에 표시하지 않습니다.

## 5. 다운로드 관리와 새 인터페이스

1. 긴 URL 칸에 Ctrl+V 또는 macOS의 Cmd+V로 붙여 넣거나 붙여넣기 버튼을 사용합니다. magnet을 넣거나 `.torrent`를 열 수도 있습니다.
2. OS 폴더 선택기로 저장 위치를 지정합니다. 기본값은 Windows `%USERPROFILE%\Downloads`, macOS/Linux `~/Downloads`이고 수동으로 고른 폴더는 유지합니다.
3. 게이트웨이를 선택하고 URL 옆의 다운로드 시작을 누릅니다. Hugging Face 저장소, WebDAV 목록, Metalink는 파일을 미리 보고 선택합니다. 일괄 가져오기는 한 줄당 URL 하나 또는 텍스트 파일을 받습니다.
4. 작업별 진행률/경과/남은 시간, 전체 속도, 각 게이트웨이 속도/전송량과 그래프를 확인합니다. HTTP 조각은 기본 **20 MiB**, 설정 범위 1–64 MiB입니다. **60초** 동안 데이터가 없는 조각은 취소 후 공유 풀로 돌아가 다른 게이트웨이가 처리합니다. torrent 조각 크기는 메타데이터에 따릅니다.
5. 일시 중지/계속, 우선순위/속도 제한/예약, 정지한 작업의 폴더 변경을 지원합니다. 기본 동시 작업은 2개이며 재시작 시 진행 중이던 작업은 일시 중지로 복원됩니다. torrent 배포는 기본 꺼짐이며 시간/비율 상한을 정할 수 있습니다.

FlatLaf의 밝게/어둡게/시스템 테마를 선택하면 저장됩니다. 요약은 필터와 무관하게 전체/활성/완료 작업과 전체 속도를 보여 줍니다. 개별 진행률 막대, 넓이에 맞는 열, 정렬/검색/상태 필터와 스크롤을 유지합니다.

진행 중 또는 완료된 행에 마우스를 올리면 **전체 파일 이름과 절대 저장 경로**가 나타납니다. 정렬/필터 후에도 실제 행과 일치하며 선택 파일 제목도 같은 도움말을 제공합니다. 우클릭 메뉴로 전체 경로를 복사합니다. Ctrl+L/Cmd+L은 URL, Ctrl+F/Cmd+F는 검색에 포커스를 주고 Escape는 검색을 지웁니다.

선택한 작업에는 세부 정보와 기록 탭이 있습니다. 새로고침/언어 변경 시 보고서 읽기 위치와 선택 텍스트가 유지되며, 다른 작업을 고르면 그 보고서 처음으로 이동합니다. 전체 로그는 앱 이벤트/다운로드 기록으로 구분됩니다. 끝에 있을 때 새 기록을 따라가고 위를 읽을 때 강제로 이동하지 않습니다.

창을 닫아도 트레이에서 다운로드가 계속됩니다. 아이콘은 모든 작업의 합산 속도를 최대 10 Gbit/s까지 표시하고 툴팁에 정확한 값을 보여 줍니다. 트레이의 앱 열기로 복원하고 앱 종료로 저장/종료합니다. 트레이를 지원하지 않으면 작업 표시줄로 최소화합니다. 업데이트 전에는 반드시 트레이에서 종료하십시오.

작업 삭제 창의 파일 삭제 체크는 **기본 해제**입니다. 목록에서만 제거하고 완료/부분 파일을 남길 수 있습니다. 의도할 때만 파일 삭제를 선택합니다. 작업/파일 삭제가 영구 기록 로그를 지우지는 않습니다.

## 6. 지원 프로토콜과 무결성

| 소스 | 동작과 조건 |
| --- | --- |
| HTTP/HTTPS | 안전한 파일 식별과 바이트 범위가 있을 때 분할/이어받기; 없으면 한 게이트웨이의 전체 스트림이며 중단 후 처음부터 다시 받음 |
| Hugging Face | 파일/저장소 선택과 Hub/Xet 해석; 비공개/제한 리소스에는 적절한 소스 접근 권한 필요 |
| FTP | 수동 EPSV/PASV; 평문 FTP는 자격 증명/데이터를 암호화하지 않음 |
| FTPS | 명시적 TLS 제어/데이터 연결; 암시적 FTPS 미지원 |
| SFTP | 독립적으로 확인해 고정한 SHA256 SSH 서버 호스트키 지문 필수; 알 수 없는 키를 자동 수락하지 않음 |
| WebDAV | HTTPS 목록과 파일 선택 |
| Metalink | `.meta4`, 신뢰할 SHA-256, 미러와 제공된 조각 해시 |
| Torrent/magnet | 선택한 게이트웨이 전송, 메타데이터 기준 조각 검증 필수, 파일 선택/우선순위 |

소스 접근 설정에서 정확한 origin별 사용자/비밀번호, Bearer token, Cookie 또는 허용된 헤더를 저장할 수 있으며 로컬 암호화됩니다. 라우터용 토큰은 필요 없습니다. URL에 비밀번호를 넣지 마십시오. 신뢰할 게시/수동 SHA-256 또는 SHA-512가 없는 FTP/FTPS/SFTP는 전체 파일 모드를 사용합니다.

조립 후 한 번 읽어 **MD5, SHA-256, SHA-512**를 모두 계산합니다. 주 입력란은 32/64/128자리 16진수 길이로 유형을 자동 감지하며 추가 참조도 입력할 수 있습니다. 전체 표현 다이제스트 헤더와 `filename.md5`, `MD5SUMS`, `filename.sha256`, `SHA256SUMS`, `filename.sha512`, `SHA512SUMS`를 탐색하고 Hugging Face LFS/Metalink 검증을 유지합니다. 일부 HTTP 응답의 다이제스트를 전체 파일 값으로 취급하지 않습니다.

알려진 참조는 최종 파일 게시 전에 모두 일치해야 합니다. 불일치 시 실패하며 부분 데이터는 남깁니다. 참조가 없으면 로컬 계산값일 뿐 게시자와 일치했다고 확인한 것이 아닙니다. MD5는 우발적 손상 탐지용이며 강한 파일 식별이나 SHA-256/SHA-512를 대체하지 않습니다. torrent 조각 검증과 전체 파일 해시는 별개이며 수동 전체 해시는 파일 하나만 선택해야 합니다.

<a name="multiroute-api"></a>

## 7. 다른 프로그램에서 API 사용

앱 실행 중 `http://127.0.0.1:8765/api/v1`에 로컬 API가 있습니다. 요청/응답은 JSON UTF-8, 본문 헤더는 `Content-Type: application/json`입니다. 작업 생성/제어용 API token은 필요 없고 소스 자격 증명은 별개입니다. 같은 컴퓨터에서만 사용하며 LAN/인터넷에 포트를 공개하지 마십시오. `--headless`는 API 전용 실행입니다. 기본 설치는 정상 실행기를 사용하고 `.cmd` 예는 portable용입니다.

상태 확인: `GET /api/v1/health`, 정상 예 `{"ok":true,"version":"4.0"}`. 먼저 창에서 네트워크를 설정하거나 다음을 `POST /api/v1/network`에 보냅니다.

```json
{
  "virtual_ip": "192.168.1.70",
  "gateways": ["192.168.1.1", "192.168.1.2", "192.168.1.3", "192.168.1.4"]
}
```

다음 JSON을 `POST /api/v1/jobs`로 전송합니다. URL과 실제 로컬 폴더를 바꾸십시오. Linux `/home/your-user/Downloads`, macOS `/Users/your-user/Downloads`도 가능합니다. API의 `routes`는 명시해야 하며 화면 체크박스를 자동 복사하지 않습니다.

```json
{
  "url": "https://example.org/large-file.bin",
  "directory": "C:/Users/your-user/Downloads",
  "chunk_mb": 20,
  "routes": [
    {"type": "gateway", "gateway": "192.168.1.1", "enabled": true},
    {"type": "gateway", "gateway": "192.168.1.2", "enabled": true},
    {"type": "gateway", "gateway": "192.168.1.3", "enabled": true},
    {"type": "gateway", "gateway": "192.168.1.4", "enabled": true}
  ]
}
```

`HTTP 201`은 작업 생성이지 완료가 아닙니다. 응답 `id`를 저장하고 `GET /api/v1/jobs/{id}`를 조회해 `status: complete`를 확인하거나 실패의 `error`를 읽습니다. `speed_bps`는 **바이트/초**이며 8을 곱하고 1,000,000으로 나누면 Mbit/s입니다.

| HTTP | API |
| --- | --- |
| GET | `/api/v1/health` |
| GET / POST | `/api/v1/jobs` |
| GET | `/api/v1/jobs/{id}` |
| POST | `/api/v1/jobs/{id}/pause` |
| POST | `/api/v1/jobs/{id}/resume` |
| POST | `/api/v1/jobs/{id}/routes` |
| DELETE | `/api/v1/jobs/{id}` |
| GET / POST | `/api/v1/network` |
| POST | `/api/v1/jobs/batch` |
| GET / POST | `/api/v1/queue` |
| GET | `/api/v1/events` |

일시 중지/계속은 빈 JSON `{}`로 호출합니다. 삭제 본문과 기본 파일 보존, `Idempotency-Key`, 대기열/일괄/SSE/소스 접근/제한의 정확한 필드는 [OpenAPI](../openapi.yaml)를 따릅니다. 생성 시 `expected_md5`, `expected_sha256`, `expected_sha512`를 제공할 수 있습니다. 응답에는 `md5`, `sha256`, `sha512`, `checksum_sources`와 경고가 나옵니다. JSON 키와 상태는 UI 언어가 바뀌어도 동일합니다. [추가 API 예제](../API.ru.txt)는 러시아어입니다.

Python 3 예제입니다. 실제 URL로 바꾸고 창에서 네트워크를 설정한 뒤 앱을 실행해 둡니다. UTF-8은 한국어 경로도 보존합니다.

```python
import json
import urllib.request
from pathlib import Path

data = {
    "url": "https://example.org/large-file.bin",
    "directory": str(Path.home() / "Downloads"),
    "chunk_mb": 20,
    "routes": [
        {"type": "gateway", "gateway": f"192.168.1.{n}", "enabled": True}
        for n in range(1, 5)
    ],
}
request = urllib.request.Request(
    "http://127.0.0.1:8765/api/v1/jobs",
    data=json.dumps(data, ensure_ascii=False).encode("utf-8"),
    headers={"Content-Type": "application/json; charset=utf-8"},
    method="POST",
)
with urllib.request.urlopen(request, timeout=30) as response:
    job = json.loads(response.read().decode("utf-8"))
print(job["id"])
```

## 8. 기록, 데이터와 업데이트

| 시스템 | 데이터 | 앱/런타임 |
| --- | --- | --- |
| Windows | `%LOCALAPPDATA%\MultiRoute Downloader\data` | `%LOCALAPPDATA%\MultiRoute Downloader App` |
| macOS | `~/Library/Application Support/MultiRoute Downloader/data` | `/Applications/MultiRoute Downloader.app` |
| Linux | `~/.local/share/MultiRouteResume/data` | `~/.local/share/MultiRouteResume/` |

macOS 기본 보호 구성 요소는 `/Library/PrivilegedHelperTools/MultiRouteResume/4.0/gateway-net`입니다. Windows portable 3.0은 실행 폴더의 `data/`이며 `data-location.config`/`--data-dir`로 바꿀 수 있습니다. Linux 전용 Java는 `~/.local/share/MultiRouteResume/runtime`, 보호 구성 요소는 `/usr/local/libexec/MultiRouteResume/3.0/gateway-net`입니다. 다운로드 파일은 사용자가 고른 별도 위치에 있습니다.

`logs/downloads-history.jsonl`은 UTF-8 줄 단위 JSON, `logs/downloads-history.log`는 날짜/시간이 있는 읽기용 로그입니다. 재시작 후에도 추가되고 작업 삭제/언어 변경으로 과거 기록을 다시 쓰지 않습니다. 텍스트는 기록 당시 언어를 유지하지만 화면은 현재 언어로 렌더링합니다. 창은 최대 최근 1,000개 이벤트/2 MiB만 읽고 전체 파일은 디스크에 남아 증가하므로 필요 시 따로 보관합니다. 이름과 절대 경로가 들어가며 소스 URL, 비밀번호, token은 기록하지 않습니다. 공유 전 개인 경로를 확인하십시오.

API `lifecycle_history`에는 `event_id`, `job_id`, `kind`, `time`, `filename`, `path`가 있고 torrent는 `paths`, 일시 중지는 `reason`(`pause`, `shutdown`, `maintenance`)가 있을 수 있습니다. 유형은 `started`, `resumed`, `paused`, `completed`, `recovered`, `failed`입니다. 시간은 ISO 8601 UTC(예: `2026-10-18T12:34:56.123Z`)이고 읽기용 로그에는 현지 전체 날짜/시간과 UTC 오프셋이 있습니다. 선택 필드 `first_started_at`, `last_resumed_at`, `completed_at`는 실제 관측한 시작/최근 계속/검증 게시 후 완료만 나타냅니다. 대기 중은 시작이 아니고 오래된 날짜를 추측하지 않습니다. `recovered`는 과거 게시 파일의 현재 검증이며 원래 완료일을 안다는 뜻은 아닙니다.

업데이트 전 트레이에서 종료하고 완전한 새 패키지를 설치하며 기록/부분 파일을 보존합니다. Windows는 설정 → 앱 → 설치된 앱에서 제거합니다. macOS의 `.app`만 지워도 데이터/보호 구성 요소는 남습니다. 제거하려고 다운로드 폴더를 삭제하지 마십시오. Linux 앱 폴더를 옮기면 배포를 다시 실행합니다.

## 9. 문제 해결과 검증 한계

- 시작 실패: 버전/CPU, 완전한 설치와 Java 모듈을 확인합니다. 잘못 지정한 `JAVA_EXE`/`JAVA_HOME`을 수정합니다. 3.9의 임시 출력 파일 잠금 수정과 이전 인스턴스 식별 확인을 유지합니다.
- 속도 0: 유선 어댑터, 같은 서브넷, 사용하지 않는 가상 IP, 실제 인터넷 권한과 소스 URL/접근 제한을 확인합니다. 속도 테스트 성공이 모든 실제 다운로드 성공을 증명하지는 않습니다.
- torrent glibc/macOS 오류: 지원 버전의 OS를 사용하고 시스템 라이브러리를 임의 교체하지 않습니다.
- SFTP 호스트키 불일치: 서버 관리자와 확인하고 키 검증을 끄지 않습니다.
- 다른 프로그램의 8765 포트: `--port 8766`을 사용하고 무관한 프로세스를 종료하지 않습니다. 본 앱으로 검증된 이전 인스턴스만 교체합니다.
- 저장 공간/권한 오류: 파일/조각/캐시 공간을 확인하고 폴더 변경 전 일시 중지합니다. 동일 대상 작업이 있으면 기존 작업을 계속합니다.

버전, OS/CPU, 단계와 정확한 오류를 제보하되 비밀번호/token, 비공개 링크와 개인 정보를 제거하십시오. 통제된 Windows/POSIX 테스트와 네이티브 Mac CI/패키지 검사는 물리 라우터 속도/장애 전환, 실제 사용자 설치나 Finder/Gatekeeper/BPF 승인 성공을 증명하지 않습니다. 게시자 서명/공증이 없는 **사전 릴리스**입니다. 설치 후 알려진 작은 파일을 게이트웨이별로, 이어서 조합으로 확인하십시오.

## 10. 라이선스

원본 앱은 개인/조직 및 업무에서 무료로 사용할 수 있습니다. 별도 허가 없이 앱 판매, 라이선스 요금 청구나 유료 배포는 금지합니다. 사용자 정의 라이선스는 일반적인 수정/재배포 권한을 주지 않으며 OSI 승인 오픈소스 라이선스가 아닙니다. 영어 [LICENSE.txt](../LICENSE.txt)가 우선합니다. FlatLaf(Apache-2.0) 등 타사 구성 요소는 각자 라이선스를 유지합니다. 소유자: [2018-spec-fdsds](https://github.com/2018-spec-fdsds).

## 이름과 기존 프로필 호환

MultiRoute Downloader의 이전 이름은 MultiRoute Resume입니다. 새 프로필은 위의 새 경로를 쓰지만 새 데이터 폴더가 없고 이전 프로필이 있으면 파일 이동/삭제 없이 그대로 재사용합니다. 이전 Windows 데이터는 `%LOCALAPPDATA%\MultiRoute Resume\data`, Mac 데이터는 `~/Library/Application Support/MultiRoute Resume/data`에 남을 수 있습니다. 보호 디렉터리 `MultiRouteResume`, Windows 업그레이드 ID, 내부 API 식별자/포트는 호환성을 유지합니다. 설치 후 새 MultiRoute Downloader 바로 가기나 앱을 사용합니다. 과거 릴리스 파일명은 그대로입니다.

## 저장한 사이트, 업로드와 압축 파일 미리 보기 (4.0)

**사이트 프로필…**에서 프로필을 만들거나 수정한 뒤 **다운로드** 또는 **업로드**에서 선택합니다. 게이트웨이와 가상 IP는 **네트워크 설정**에 있습니다. **업로드**에서 **원격 폴더 URL**을 입력하고 **파일을 선택하여 업로드…**로 시스템 선택기에서 한 개 이상의 로컬 파일을 선택합니다. 화면은 파일명을 붙이지만 API `target_url`은 전체 원격 파일 URL을 사용합니다. 압축 파일 URL을 기본 입력란에 붙이고 **압축 파일 내용…**를 누르면 다운로드 없이 미리 봅니다.

사이트 프로필에 이름, 기본 URL, 로컬 다운로드 폴더, 원격 폴더와 게이트웨이를 저장합니다. 익명 접근, HTTP Basic/Bearer/Cookie/사용자 헤더, FTP 및 사전에 확인한 서버 SHA-256 키를 사용하는 SFTP를 지원합니다. 같은 서버의 여러 계정도 별도로 보관합니다. 명시한 폴더/게이트웨이가 기본값보다 우선하며 상대 URL은 기본 URL을 기준으로 합니다. 비밀 정보는 소유자 전용 `site-profiles/vault.bin`에 암호화되고 키는 `site-profiles/key.bin`에 있습니다. 계정과 키를 장악한 사람으로부터는 보호하지 못합니다. 공개 API는 비밀을 반환하지 않습니다. 출처와 인증 방식이 같을 때만 빈 입력은 이전 비밀을 유지하며 `replace_credentials:true`로 교체합니다. 다른 출처로 HTTP 리디렉션하면 자격 증명을 보내지 않습니다. 프로필 삭제는 작업이나 로컬/원격 파일을 삭제하지 않으며 연결된 작업은 원래 프로필 연결을 유지하며 계정을 바꿀 수 없으므로 삭제했다면 필요에 따라 유효한 프로필로 작업을 다시 만드십시오.

업로드와 원격 폴더 탐색은 선택한 게이트웨이를 통한 **FTP, 명시적 FTPS, SFTP**를 지원합니다. 일반 로컬 파일과 폴더가 아닌 전체 원격 파일 URL(`target_url`)을 선택합니다. 원격 임시 파일의 기존 접두부를 비교한 뒤 이어 올릴 수 있습니다. 완료하려면 원격 파일 전체를 다시 읽어 SHA-256을 비교하므로 **읽기·쓰기·이름 변경** 권한이 필요하며 추가 트래픽이 발생합니다. 불일치나 읽기 거부는 실패입니다. 한 파일은 한 번에 한 게이트웨이를 사용하고 여러 파일은 게이트웨이에 나뉘어 병렬 전송됩니다. 유휴 제한 후 다른 게이트웨이로 바꿀 수 있지만 한 업로드를 동시에 분할하지는 않습니다. 일시 중지/재개와 진행률은 재시작 후에도 유지됩니다. 작업 삭제는 로컬 파일과 원격 완성/임시 파일을 남깁니다.

덮어쓰기는 기본으로 꺼져 있습니다. FTP/SFTP는 대상이 없을 때만 원자적으로 이름을 바꾸는 기능을 항상 제공하지 않으므로 외부 클라이언트와 최종 대상이 충돌할 수 있습니다. 공유 대상을 조정하십시오. 폴더 탐색은 파일을 올리거나 삭제하지 않습니다.

**ZIP, TAR, 7Z, RAR4, RAR5** 미리 보기는 이름, 경로, 크기, 폴더/암호화 표시만 읽고 압축 해제나 실행은 하지 않습니다. 선택한 게이트웨이, 바이트 범위 및 안정적인 ETag 또는 Last-Modified가 필요합니다. 메타데이터 16 MiB, 10,000개, 60초 제한이며 7z 헤더 해독 메모리는 64 MiB입니다. 목록은 잘릴 수 있고 내용의 무결성을 확인하지 않습니다. 이름 오류에는 원래 문자셋을 지정하십시오. 지원하는 암호화 7z 헤더에는 저장하지 않는 암호를 사용할 수 있습니다. 암호화 RAR 헤더는 미지원/암호 필요로 표시되며 분할 볼륨은 일부만 보일 수 있습니다.

다운로드·업로드·목록·미리 보기 API는 `site_profile_id`를 받습니다. `GET /api/v1/site-profiles`는 `{profiles:[...]}`, `POST`/`PUT`는 평면 프로필 저장과 공개 메타데이터 반환, `DELETE /api/v1/site-profiles/{id}`는 프로필만 삭제합니다. `POST /api/v1/uploads`는 `local_path`, `target_url`, `routes`, 선택적 `site_profile_id`, `overwrite:false`를 받아 HTTP 201을 반환합니다. GET은 `{uploads:[...]}`를 반환하며 개별 GET/DELETE, POST `/{id}/pause` / `/{id}/resume`도 지원합니다. `POST /api/v1/remote/list`는 `url`과 `route`/`routes`, `POST /api/v1/archives/preview`는 추가로 선택적 `password`, `charset`을 받습니다. [openapi.yaml](../openapi.yaml)을 참조하십시오. `data/uploads/`의 UTF-8 매니페스트에는 암호 대신 프로필 ID가 있습니다. 미리 보기는 다운로드 작업을 만들지 않습니다.

## 다운로드 일정과 우선순위

큐/일정에서 `max_active_jobs`(1–32), 전체/게이트웨이 속도 제한, 일일 시간대(`schedule_enabled`, `schedule_start`, `schedule_end`, HH:mm)를 설정합니다. 컴퓨터 현지 시간대이며 `schedule_timezone`으로 확인할 수 있습니다. 자정을 넘길 수 있고 시작/끝이 같으면 하루 종일입니다. 시간대 밖에서는 큐로 돌아가며 수동 일시 중지는 유지됩니다. `priority` -100~100에서 높은 대기 작업부터, 같으면 생성 순서로 시작하며 실행 중인 작업을 중단하지 않습니다. `start_after_ms`는 Unix 밀리초 기준 가장 이른 시작 시각이고 0은 지연 해제입니다. 재시작 후 유지되며 다운로드에만 적용되고 업로드 큐는 별개입니다. 앱과 컴퓨터가 깨어 있어야 하며 OS 깨우기는 지원하지 않습니다. API: `GET`/`PUT`/`POST /api/v1/queue`, `PUT`/`POST /api/v1/jobs/{id}/options`.

## Chrome 및 Edge 통합

앱의 **Chrome / Edge** 통합에서 링크 전달을 켜고 확장 폴더를 엽니다. 한 번 `chrome://extensions` 또는 `edge://extensions`에서 개발자 모드를 켜고 “압축해제된 확장 프로그램 로드”로 `manifest.json`이 있는 `browser-extension`을 선택합니다. 네이티브 설치본은 JAR 옆에 포함하며 소스에도 [browser-extension/](../browser-extension/)이 있습니다. 폴더를 옮기지 마십시오. 별도 `MultiRoute-Downloader-4.0-Chrome-Edge.zip`은 먼저 풉니다. 스토어 게시본은 아닙니다.

MultiRoute가 실행 중이어야 하며 트레이여도 됩니다. 링크 메뉴 또는 확장 팝업에서 URL을 보냅니다. 현재 폴더, 게이트웨이, 사이트 프로필을 사용하며 필요한 저장소/torrent 선택은 계속 나타납니다. 기본 포트 8765를 API 포트와 맞추십시오. URL만 보내며 브라우저 쿠키/암호를 복사하거나 다른 다운로드를 가로채지 않습니다. 원본 인증은 앱에서 설정합니다. 로컬 연결 권한 요청은 이 확장에만 허용하십시오.

기본으로 꺼진 브리지는 `chrome-extension://ljdklefabmaecjalfhjoicmgedmjjkbl`의 `/api/v1/browser/status`, `/api/v1/browser/download`만 허용하며 다른 API 규칙을 바꾸지 않습니다. HTTP 202는 화면 전달이지 다운로드 완료가 아닙니다. headless 서버만으로는 화면 선택이 열리지 않습니다. 브라우저에서 확장을 삭제하거나 앱 통합을 끄면 중단됩니다.

[Chrome](https://developer.chrome.com/docs/extensions/get-started/tutorial/hello-world) · [Edge](https://learn.microsoft.com/en-us/microsoft-edge/extensions-chromium/getting-started/extension-sideloading).
