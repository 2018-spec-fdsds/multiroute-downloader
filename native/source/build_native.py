from pathlib import Path
import os
import subprocess

base = Path(__file__).resolve().parent
def version_key(path):
    return tuple(int(part) for part in path.name.split('.') if part.isdigit())
def find_msvc():
    override = os.environ.get('MULTIROUTE_MSVC_ROOT')
    if override:
        candidates = [Path(override)]
    else:
        candidates = []
        for variable in ('ProgramFiles(x86)', 'ProgramFiles'):
            root = os.environ.get(variable)
            if root:
                candidates += list((Path(root) / 'Microsoft Visual Studio').glob('*/*/VC/Tools/MSVC/*'))
    candidates = [path for path in candidates if (path / 'bin/Hostx64/x64/cl.exe').is_file()]
    if not candidates:
        raise SystemExit('MSVC x64 toolchain not found. Install Visual Studio C++ Build Tools or set MULTIROUTE_MSVC_ROOT to VC/Tools/MSVC/<version>.')
    return max(candidates, key=version_key)
msvc = find_msvc()
cl = msvc / 'bin/Hostx64/x64/cl.exe'
dependencies = Path(os.environ.get('MULTIROUTE_BUILD_DEPS_ROOT') or base)
sdk = dependencies / 'microsoft.windows.sdk.cpp'
sdk_x64 = dependencies / 'microsoft.windows.sdk.cpp.x64'
if sdk.exists() and sdk_x64.exists():
    include_root = next(p.parent.parent for p in sdk.rglob('um/Windows.h'))
    lib_um = next(sdk_x64.rglob('um/x64/WS2_32.Lib')).parent
    lib_ucrt = next(sdk_x64.rglob('ucrt/x64/libucrt.lib')).parent
else:
    kits = Path(os.environ.get('WindowsSdkDir') or (Path(os.environ['ProgramFiles(x86)']) / 'Windows Kits/10'))
    versions = [path for path in (kits / 'Include').glob('*') if (path / 'um/Windows.h').is_file()]
    if not versions:
        raise SystemExit('Windows SDK not found. From this folder run: python fetch_sdk.py')
    include_root = max(versions, key=version_key)
    lib_um = kits / 'Lib' / include_root.name / 'um/x64'
    lib_ucrt = kits / 'Lib' / include_root.name / 'ucrt/x64'
npcap_sdk = Path(os.environ.get('MULTIROUTE_NPCAP_SDK') or (dependencies / 'npcap-sdk'))
if not (npcap_sdk / 'Include/pcap.h').is_file() or not (npcap_sdk / 'Lib/x64/wpcap.lib').is_file():
    raise SystemExit('Npcap SDK headers/import library not found. Run: python fetch_sdk.py or set MULTIROUTE_NPCAP_SDK.')
build = Path(os.environ.get('MULTIROUTE_BUILD_OUTPUT_DIR') or base / 'build')
build.mkdir(exist_ok=True)
port = base / 'lwip/contrib/ports/win32/sys_arch.c'
# Keep the official BSD port, but use an ephemeral crypto context (never create a user key container).
patched_port = build / 'sys_arch.c'
text = port.read_text()
text = text.replace('PROV_RSA_FULL, 0)', 'PROV_RSA_FULL, CRYPT_VERIFYCONTEXT)')
text = text.replace('PROV_RSA_FULL, CRYPT_NEWKEYSET)', 'PROV_RSA_FULL, CRYPT_VERIFYCONTEXT)')
patched_port.write_text(text)
sources = [base / 'gateway_native.c', patched_port]
for directory in ('src/core', 'src/core/ipv4', 'src/api'):
    sources += sorted((base / 'lwip' / directory).glob('*.c'))
sources += [base / 'lwip/src/netif/ethernet.c']
env = os.environ.copy()
includes = [msvc / 'include', include_root / 'ucrt', include_root / 'shared', include_root / 'um',
            base, base / 'lwip/src/include', base / 'lwip/contrib/ports/win32/include', npcap_sdk / 'Include']
libraries = [msvc / 'lib/x64', lib_um, lib_ucrt, npcap_sdk / 'Lib/x64']
env['INCLUDE'] = ';'.join(map(str, includes))
env['LIB'] = ';'.join(map(str, libraries))
env['PATH'] = str(cl.parent) + os.pathsep + env['PATH']
exe = build / 'gateway-net.exe'
args = [str(cl), '/nologo', '/O2', '/MT', '/W3', '/DWIN32', '/D_CRT_SECURE_NO_WARNINGS',
        '/D_WIN32_WINNT=0x0601', f'/Fe{exe}', f'/Fo{build}\\'] + list(map(str, sources)) + [
        '/link', 'wpcap.lib', 'Ws2_32.lib', 'Iphlpapi.lib', 'Advapi32.lib', 'delayimp.lib',
        '/DELAYLOAD:wpcap.dll', '/DYNAMICBASE', '/NXCOMPAT']
print('sdk_include', include_root, 'sdk_lib', lib_um, flush=True)
completed = subprocess.run(args, cwd=build, env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
(build / 'build.log').write_bytes(completed.stdout)
print(completed.stdout.decode('utf-8', errors='replace'))
raise SystemExit(completed.returncode)
