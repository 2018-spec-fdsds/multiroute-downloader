"""Fresh Linux Java 17 DNS regressions; only owned loopback TLS and isolated state."""
from pathlib import Path
import hashlib,json,os,platform,re,shutil,subprocess,sys

sys.stdout.reconfigure(encoding="utf-8");sys.stderr.reconfigure(encoding="utf-8")
REPO=Path(sys.argv[1]).resolve();OUTPUT=Path(sys.argv[2]).resolve()
assert platform.system()=="Linux" and os.getuid()>0
OUTPUT.mkdir(parents=True,exist_ok=False)
SPECS={"SecureDnsProtocolTest":("SECURE-DNS-PROTOCOL-TESTS.json",42),"ManagedDnsTest":("MANAGED-DNS-TESTS.json",147),"Http2DnsTest":("HTTP2-DNS-TESTS.json",106)}
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def inputs():
    values=[path for path in (REPO/"src").rglob("*") if path.is_file() and path.suffix in {".java",".json"}]
    values+=list((REPO/"lib").glob("*.jar"))
    values+=[REPO/"scripts/dependencies.sha256.json",Path(__file__).resolve(),REPO/"packaging/tests/dns_tls_fixture.py"]
    values+=[REPO/"packaging/tests"/(name+".java") for name in SPECS]
    return {path.relative_to(REPO).as_posix():sha(path) for path in sorted(values)}
snapshot=inputs();pins=json.loads((REPO/"scripts/dependencies.sha256.json").read_text(encoding="utf-8"))
libs=sorted((REPO/"lib").glob("*.jar"));assert set(pins)=={path.name for path in libs}
assert all(sha(path)==pins[path.name] for path in libs)
sources=sorted((REPO/"src").rglob("*.java"));classes=OUTPUT/"classes";classes.mkdir()
env={key:value for key,value in os.environ.items() if key not in {"JAVA_TOOL_OPTIONS","_JAVA_OPTIONS","JDK_JAVA_OPTIONS"}}
env.update(PYTHONUTF8="1",PYTHONIOENCODING="utf-8",LC_ALL="C.UTF-8")
def run(argv,log,timeout=180):
    with log.open("wb") as stream:result=subprocess.run(list(map(str,argv)),cwd=OUTPUT,env=env,stdout=stream,stderr=subprocess.STDOUT,timeout=timeout)
    text=log.read_text(encoding="utf-8");assert "\ufffd" not in text
    if result.returncode:raise RuntimeError(log.name+": "+text[-8000:])
    return text
javac=Path(shutil.which("javac")).resolve();java=Path(shutil.which("java")).resolve();keytool=Path(shutil.which("keytool")).resolve()
assert javac.is_file() and java.is_file() and keytool.is_file()
cp=os.pathsep.join(map(str,[classes,*libs]))
run([javac,"-J-Dfile.encoding=UTF-8","--release","17","-encoding","UTF-8","-cp",cp,"-d",classes,*sources,*[REPO/"packaging/tests"/(name+".java") for name in SPECS]],OUTPUT/"compile.log")
for path in classes.rglob("*.class"):assert path.read_bytes()[:8]==bytes.fromhex("cafebabe0000003d")
for path in (REPO/"src").rglob("*.json"):
    target=classes/path.relative_to(REPO/"src");target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(path,target)
version=re.search(r'static final String VERSION = "([0-9.]+)";', (REPO/"src/multiroute/Main.java").read_text(encoding="utf-8")).group(1)
assert version=="4.2.0"
results={}
for name,(filename,minimum) in SPECS.items():
    case=OUTPUT/name;case.mkdir();state=case/"state"
    for directory in ("home","tmp"):(case/directory).mkdir()
    run([sys.executable,"-X","utf8",REPO/"packaging/tests/dns_tls_fixture.py","--keytool",keytool,"--directory",state],case/"tls.log",120)
    run([java,"-Dfile.encoding=UTF-8","-Dstdout.encoding=UTF-8","-Dstderr.encoding=UTF-8","-Djava.awt.headless=true","-Duser.home="+str(case/"home"),"-Djava.io.tmpdir="+str(case/"tmp"),"-cp",cp,"multiroute."+name,state],case/"test.log")
    report=json.loads((state/filename).read_text(encoding="utf-8"));assert report["status"]=="PASS" and report["checks"]>=minimum
    if "version" in report:assert report["version"]==version
    if name=="SecureDnsProtocolTest":assert report["owned_loopback"] and report["no_public_dns"] and report["no_os_fallback"] and report["headless"] and not report["external_network_used"]
    elif name=="ManagedDnsTest":
        assert len(report["cases"])>=10 and report["loopback_only"] and report["expiry_clock_injected"] and report["generation_race_window_injected"]
        assert not report["external_dns_queries"] and not report["real_os_dns"] and not report["installed_application_accessed"] and not report["os_network_changed"]
    else:
        assert report["cases"]>=43 and len(report["case_names"])==report["cases"]
        assert report["loopback_only"] and report["real_tls_h2_alpn"] and report["no_public_dns"] and report["no_os_fallback"]
        assert not report["installed_app_accessed"] and not report["os_network_changed"]
    results[name]={"report":report,"fixture_source_sha256":snapshot["packaging/tests/"+name+".java"],"checks":report["checks"],"headless":True,
                   "tls_fixture_generator_sha256":snapshot["packaging/tests/dns_tls_fixture.py"],"stdout_sha256":sha(case/"test.log")}
assert snapshot==inputs(),"Java DNS sources changed during Linux verification"
receipt={"status":"PASS","platform":"Linux","version":version,"java_release":17,"fixture_groups":3,"fixtures":results,
         "source_tree":snapshot,"source_snapshot_unchanged":True,"dependencies_sha256":pins,"headless":True,"loopback_only":True,
         "public_dns_used":False,"installed_application_accessed":False,"os_network_changed":False,
         "runtime_version":run([java,"-version"],OUTPUT/"java-version.log").strip()}
(OUTPUT/"JAVA-DNS-VERIFICATION.json").write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
print("LINUX_JAVA_DNS_VERIFIED fixture_groups=3 secure=42 managed=147 http2=106")
