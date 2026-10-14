from pathlib import Path
import urllib.request
import zipfile
import hashlib

base = Path(__file__).resolve().parent
version = '10.0.26100.2454'
expected = {
    'microsoft.windows.sdk.cpp': 'e1ada25d24444ac62fd7cb33894d7a7827ec4fa55a22956d0c1e51c2a66f7462',
    'microsoft.windows.sdk.cpp.x64': '8a5390763fbe8bbcbbeb8e9d1c46b196d36dd8d5c13a6eadc8113ff043bce0a8',
}
def fetch_extract(archive, target, url, sha256):
    if not archive.exists():
        partial = archive.with_suffix(archive.suffix + '.partial')
        urllib.request.urlretrieve(url, partial)
        partial.replace(archive)
    actual = hashlib.sha256(archive.read_bytes()).hexdigest()
    if actual != sha256:
        raise RuntimeError(f'Official dependency SHA-256 mismatch: {archive.name}')
    with zipfile.ZipFile(archive) as z:
        prefix = target.resolve()
        for member in z.infolist():
            destination = (prefix / member.filename).resolve()
            if not destination.is_relative_to(prefix):
                raise RuntimeError('Archive path leaves dependency folder')
        z.extractall(target)
    print(archive.name, archive.stat().st_size, actual, flush=True)
for package in ('microsoft.windows.sdk.cpp', 'microsoft.windows.sdk.cpp.x64'):
    archive = base / (package + '.' + version + '.nupkg')
    url = f'https://api.nuget.org/v3-flatcontainer/{package}/{version}/{package}.{version}.nupkg'
    target = base / package
    fetch_extract(archive, target, url, expected[package])
fetch_extract(base / 'npcap-sdk-1.16.zip', base / 'npcap-sdk', 'https://npcap.com/dist/npcap-sdk-1.16.zip',
              'f0a8be7778ee3ae1b99bbbecb27a3ff0f6c111a4093f1c78c5c5a099607184db')
