# macOS: 설치, 첫 실행 및 권한

<!-- macos-languages:start -->
[English](en.md) · [Русский](ru.md) · [Deutsch](de.md) · [Español](es.md) · [Français](fr.md) · [中文](zh.md) · **한국어** · [العربية](ar.md) · [Italiano](it.md) · [日本語](ja.md) · [हिन्दी](hi.md) · [Tiếng Việt](vi.md)
<!-- macos-languages:end -->

## 설치하고 열기

업데이트 전에 전송이 끝날 때까지 기다린 후 메뉴 막대의 MultiRoute 메뉴에서 **Quit/Exit**를 선택하세요. 빨간 닫기 버튼은 창만 숨길 수 있습니다. [다운로드](../../downloads/README.md)에서 Apple Silicon 또는 Intel용 PKG를 받고 SHA-256을 확인한 뒤 엽니다. macOS 관리자 시스템 대화상자에서 설치를 승인하세요. Finder나 Spotlight로 `/Applications/MultiRoute Downloader.app`을 실행합니다. 별도 Java나 터미널은 필요 없습니다.

## 열기 승인

개발자를 확인할 수 없다고 나오면 먼저 PKG나 앱 열기를 시도하세요. 확인한 신뢰할 수 있는 파일에 한해서 **시스템 설정 → 개인정보 보호 및 보안 → Open Anyway → Open**을 선택합니다. 번역과 버전에 따라 이름은 다릅니다. 악성 소프트웨어나 손상된 파일 경고는 무시하지 말고 중단하여 파일을 확인하세요. [Apple 안내](https://support.apple.com/en-us/102445).

## 네트워크와 암호

앱을 처음 시작하거나 가상 IP, DHCP 또는 앱 MAC을 적용할 때 Ethernet BPF를 열기 위한 관리자 승인이 필요할 수 있습니다. 구성 변경이나 구성 요소 재시작 후 다시 물을 수 있습니다. 정상적인 재확인과 속도 검사는 준비된 구성 요소를 재사용하므로 파일이나 게이트웨이마다 암호를 요구하지 않습니다. OS 경로나 물리 MAC은 변경하지 않습니다.

**로컬 네트워크** 요청이 나타나면 개인정보 보호 및 보안 → 로컬 네트워크에서 확인된 MultiRoute 항목을 허용하세요. 모든 설치에서 이 요청이 표시되지는 않습니다. [Apple TN3179](https://developer.apple.com/documentation/technotes/tn3179-understanding-local-network-privacy).

Mac 관리자 암호는 시스템 대화상자에만 입력하세요. 라이선스나 Hugging Face 토큰이 아닙니다. 요청자가 `DesktopLauncher`/Java로 표시될 수 있으므로 방금 시작한 신뢰할 수 있는 MultiRoute 작업인지 확인하세요. 취소했다면 명시적으로 다시 검사하고 로그를 확인하세요. Gatekeeper/SIP를 전역 해제하지 마세요.

[MultiRoute Downloader — 한국어](../ko.md) · [macOS — English / 12 languages](README.md)
