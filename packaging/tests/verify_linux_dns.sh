#!/usr/bin/env bash
# Ubuntu prerequisites: build-essential, pkg-config, libpcap-dev.
# The verification runs as an ordinary user: no capture or OS network changes.
set -euo pipefail
REPO="$(cd -- "${1:?usage: verify_linux_dns.sh REPOSITORY OUTPUT}" && pwd -P)"
mkdir -p -- "${2:?OUTPUT directory required}"
OUTPUT="$(cd -- "$2" && pwd -P)"
BASE="$REPO/native/source"
CC="${CC:-cc}"
export LC_ALL=C.UTF-8 PYTHONUTF8=1 PYTHONIOENCODING=utf-8
test "$(uname -s)" = Linux
test "$(id -u)" -ne 0
command -v "$CC"
pkg-config --exists libpcap
python3 - "$REPO" "$OUTPUT" <<'PY'
from pathlib import Path
import hashlib,json,sys
repo=Path(sys.argv[1]);out=Path(sys.argv[2])
sources=sorted(path for path in (repo/'native/source').rglob('*') if path.is_file() and path.suffix in ('.c','.h','.py'))
sources += [repo/'native/source/build_linux.sh', repo/'packaging/tests/verify_linux_dns.sh']
snapshot={path.relative_to(repo).as_posix():hashlib.sha256(path.read_bytes()).hexdigest() for path in sources}
(out/'SOURCE-INPUTS.json').write_text(json.dumps(snapshot,sort_keys=True,indent=2)+'\n',encoding='utf-8')
PY
for FIXTURE in gateway_diagnostics gateway_topology gateway_dns_transport gateway_socks_resolve gateway_dns_broker_protocol gateway_dns_broker_transport; do
  "$CC" -std=c11 -D_DEFAULT_SOURCE -pthread -Wall -Wextra -Werror \
    "$BASE/tests/${FIXTURE}_test.c" -o "$OUTPUT/${FIXTURE}_test" >"$OUTPUT/${FIXTURE}-build.log" 2>&1
  "$OUTPUT/${FIXTURE}_test" >"$OUTPUT/${FIXTURE}-output.txt" 2>"$OUTPUT/${FIXTURE}-stderr.txt"
  printf 'PASS %s\n' "$FIXTURE"
done
MULTIROUTE_BUILD_OUTPUT="$OUTPUT/gateway-net" bash "$BASE/build_linux.sh" >"$OUTPUT/native-build.log" 2>&1
"$OUTPUT/gateway-net" --self-test >"$OUTPUT/self-test.txt" 2>"$OUTPUT/self-test-stderr.txt"
grep -F 'MANAGED_DNS_CONTROL_SELF_TEST_OK' "$OUTPUT/self-test.txt"
grep -F 'MANAGED_DNS_SELF_TEST_OK' "$OUTPUT/self-test.txt"
READY="$OUTPUT/ready-managed-missing.json"
set +e
"$OUTPUT/gateway-net" --managed-dns --ready-file "$READY" --stop-file "$READY.stop" \
  --parent-pid "$$" --drop-uid "$(id -u)" --drop-gid "$(id -g)" \
  >"$OUTPUT/managed-missing.txt" 2>"$OUTPUT/managed-missing-stderr.txt"
RESULT=$?
set -e
test "$RESULT" -eq 2
python3 "$REPO/packaging/tests/verify_linux_java_dns.py" "$REPO" "$OUTPUT/java-dns"
python3 - "$REPO" "$OUTPUT" <<'PY'
from pathlib import Path
import hashlib,json,re,subprocess,sys
repo=Path(sys.argv[1]);out=Path(sys.argv[2])
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
before=json.loads((out/'SOURCE-INPUTS.json').read_text(encoding='utf-8'))
assert before=={name:sha(repo/name) for name in before},'Native sources changed during verification'
missing=json.loads((out/'ready-managed-missing.json').read_text(encoding='utf-8'))
assert missing['ok'] is False and missing['managed_dns'] is True and missing['dns_revision']==0
assert 'no plaintext fallback' in missing['error']
expected={
 'gateway_diagnostics':('GATEWAY_DIAGNOSTICS_TEST_OK',None),
 'gateway_topology':('GATEWAY_TOPOLOGY_TEST_OK',None),
 'gateway_dns_transport':('GATEWAY_DNS_TRANSPORT_TEST_OK checks=169 capture=false os_network_changed=false',169),
 'gateway_socks_resolve':('GATEWAY_SOCKS_RESOLVE_TEST_OK checks=145 capture=false sockets=false os_dns=false',145),
 'gateway_dns_broker_protocol':('GATEWAY_DNS_BROKER_PROTOCOL_TEST_OK checks=191 network_handles=false os_network_changed=false',191),
 'gateway_dns_broker_transport':('GATEWAY_DNS_BROKER_TRANSPORT_TEST_OK checks=63 loopback_only=true fake_lwip=true selected_profiles=4 simulated_payload_slots=96 reserved_slots=8 stale_revision_rejected=true os_network_changed=false',63)
}
fixtures=[]
for name,(marker,count) in expected.items():
 text=(out/(name+'-output.txt')).read_text(encoding='utf-8')
 error=(out/(name+'-stderr.txt')).read_text(encoding='utf-8')
 assert text.splitlines()[-1].startswith(marker) and '\ufffd' not in text+error
 fixtures.append({'name':name,'status':'PASS','checks':count,'stdout':text,'stderr':error,
  'stdout_sha256':hashlib.sha256(text.encode('utf-8')).hexdigest(),'stderr_sha256':hashlib.sha256(error.encode('utf-8')).hexdigest(),
  'source_sha256':sha(repo/'native/source/tests'/(name+'_test.c'))})
selftest=(out/'self-test.txt').read_text(encoding='utf-8')+(out/'self-test-stderr.txt').read_text(encoding='utf-8')
assert 'MANAGED_DNS_CONTROL_SELF_TEST_OK' in selftest and 'MANAGED_DNS_SELF_TEST_OK' in selftest
java_dns=json.loads((out/'java-dns/JAVA-DNS-VERIFICATION.json').read_text(encoding='utf-8'))
assert java_dns['status']=='PASS' and java_dns['fixture_groups']==3 and java_dns['source_snapshot_unchanged']
report={'status':'PASS','platform':'Linux','source_commit':subprocess.check_output(['git','-C',str(repo),'rev-parse','HEAD']).decode('ascii').strip(),
 'helper_sha256':sha(out/'gateway-net'),'source_tree':before,'source_snapshot_unchanged':True,'fixtures':fixtures,'fixture_groups':6,
 'managed_missing_rejected_before_capture':True,'capture_started':False,'os_network_changed':False,
 'network_scope':'owned loopback IPC and TLS only; fake lwIP, no public connections','selftest':selftest,
 'java_dns_verification':java_dns,'total_fixture_groups':9}
(out/'VERIFICATION.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('LINUX_MANAGED_DNS_VERIFIED native_fixture_groups=6 java_fixture_groups=3')
PY
