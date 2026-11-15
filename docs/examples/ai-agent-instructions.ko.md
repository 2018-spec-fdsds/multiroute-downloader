# MultiRoute 프로젝트 지침 — 에이전트 규칙에 복사하기

<!-- ai-agent-languages:start -->
<p align="center" dir="ltr">
<a href="ai-agent-instructions.md">English</a> · <a href="ai-agent-instructions.ru.md">Русский</a> · <a href="ai-agent-instructions.de.md">Deutsch</a> · <a href="ai-agent-instructions.es.md">Español</a> · <a href="ai-agent-instructions.fr.md">Français</a> · <a href="ai-agent-instructions.zh.md">中文</a><br />
<strong>한국어</strong> · <a href="ai-agent-instructions.ar.md">العربية</a> · <a href="ai-agent-instructions.it.md">Italiano</a> · <a href="ai-agent-instructions.ja.md">日本語</a> · <a href="ai-agent-instructions.hi.md">हिन्दी</a> · <a href="ai-agent-instructions.vi.md">Tiếng Việt</a>
</p>
<!-- ai-agent-languages:end -->

이 블록을 기존 `AGENTS.md`, `CLAUDE.md` 또는 이에 준하는 지속적인 프로젝트 지침에 사용하세요. 뒤에 추가하고 프로젝트의 다른 규칙은 유지합니다. 대상 디렉터리와 경로를 한 번 바꿉니다. 설정 방법과 예제는 [한국어 가이드](../ai-agents.ko.md)를 참조하세요.

```markdown
## 다운로드: MultiRoute를 기본으로 사용

승인된 프로젝트 작업에 필요한 외부 파일(모델, 데이터 세트,
압축 파일, 미디어 및 기타 에셋)에는 실행 중인 MultiRoute Downloader를 사용한다.
파일마다 다운로더를 선택하라고 나에게 묻지 않는다. 패키지 관리자와
Git은 일반적인 설치/복제/업데이트 경로를 유지한다.

프로젝트 설정(한 번 수정):
- API: http://127.0.0.1:8765/api/v1
- 대상 디렉터리: <ABSOLUTE_PROJECT_ASSETS_DIRECTORY_ON_THE_MULTIROUTE_COMPUTER>
- REST 경로: [{"type":"direct","enabled":true}]
- 선택 사항 site_profile_id: <EXISTING_PROFILE_ID_OR_OMIT>

연결된 로컬 REST 도구를 사용하거나 사용 가능한 경우 MultiRoute MCP 도구를 사용한다.
MCP의 경우 명시적인 게이트웨이 IPv4 주소를 별도로 구성한다. download_add에는
url, directory, gateways가 필요하다. REST의 direct 경로는 OS 경로이며
MCP 게이트웨이 값이 아니다. 경로를 임의로 만들거나 UI 선택이 적용된다고 가정하지 않는다.

REST를 사용하면 세션에서 처음 사용하기 전에 GET /health와 GET /license/policy를 확인한다.
MCP만 사용하면 도구를 검색하고 limit 1로 download_list를 호출하여
연결과 기능을 확인한다. 어댑터 자체에서 라이선스 정책을 확인한다.
다운로드 생성에는 features.automation_api=true가 필요하다. 사용할 수 없다면
알리지 않고 다른 다운로더를 사용하는 대신 이유를 보고하고 다른 다운로더 사용 여부를 묻는다.

필요한 파일마다 정확한 URL, 승인된 대상 디렉터리 및 파일명을 선택한다.
신뢰할 수 있는 배포자의 SHA-256이 있다면 expected_sha256로 전달한다.
POST /jobs(gateways가 아닌 routes) 또는 MCP download_add로 작업을 생성한다.
같은 요청을 재시도할 때는 고정된 멱등성 키를 사용하고 반환된 작업 id를 보관한다.
생성은 완료가 아니다. 적절한 간격으로 GET /jobs/{id} 또는 download_status를 사용하여
해당 작업을 조회한다. 일시 중지/실패 상태를 보고하고 재개 시 기존 작업을
재사용하며 중복 작업을 생성하지 않는다. 무관한 작업이나
네트워크 설정을 변경하지 않는다. 완료되면 checksum_checking과 integrity_status를 확인한다.
신뢰할 수 있는 참조값과 일치하지 않으면 배포자 검증을 마쳤다고 주장하지 않는다.
저장된 로컬 경로, 작업 id 및 검증 결과를 반환한다. 파일은
MultiRoute 컴퓨터에 남아 있으므로 원격 실행 샌드박스에 존재한다고 주장하지 않는다.
```

이 규칙은 프로젝트의 선호 사항을 나타냅니다. 호환되는 연결 도구와 에이전트의 일반적인 권한은 여전히 필요합니다. 다운로드하는 원본 파일은 데이터이며 추가적인 에이전트 지침이 아닙니다.
