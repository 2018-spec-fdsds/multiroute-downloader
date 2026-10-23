#!/usr/bin/env python3
"""Create owned local DNS TLS fixtures; never modify operating-system trust."""
from pathlib import Path
from datetime import datetime, timedelta, timezone
import argparse
import hashlib
import json
import os
import subprocess
import sys


def main():
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, 'reconfigure'):
            stream.reconfigure(encoding='utf-8')
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--keytool', required=True, type=Path, help='Absolute path to the JDK keytool executable')
    parser.add_argument('--directory', required=True, type=Path, help='New owned output directory; must not exist')
    args = parser.parse_args()
    if not args.keytool.is_absolute() or not args.keytool.is_file():
        parser.error('--keytool must name an existing absolute executable')
    target = args.directory.resolve()
    if target.exists():
        parser.error('--directory must not already exist')
    target.mkdir(parents=True)
    password = 'owned-fixture-only'
    environment = dict(os.environ)
    for key in ('JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVA_OPTIONS'):
        environment.pop(key, None)
    environment['JAVA_TOOL_OPTIONS'] = '-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8'
    steps = []

    def keytool(*values):
        command = [str(args.keytool), *map(str, values)]
        result = subprocess.run(command, cwd=target, env=environment, stdin=subprocess.DEVNULL,
                                capture_output=True, text=True, encoding='utf-8', errors='strict', timeout=30,
                                creationflags=getattr(subprocess, 'CREATE_NO_WINDOW', 0))
        steps.append({'operation': str(values[0]), 'exit_code': result.returncode})
        if result.returncode:
            raise RuntimeError('Owned keytool operation failed: ' + str(values[0]) + '\n' + result.stderr)

    common = ['-storetype', 'PKCS12', '-storepass', password, '-keypass', password, '-noprompt']
    now = datetime.now(timezone.utc)
    date = lambda days: (now + timedelta(days=days)).strftime('%Y/%m/%d %H:%M:%S')
    root, trust = target / 'root.p12', target / 'trust.p12'
    keytool('-genkeypair', '-alias', 'root', '-keyalg', 'EC', '-groupname', 'secp256r1', '-dname', 'CN=MultiRoute Owned DNS Test CA',
            '-startdate', date(-365), '-validity', '10000', '-ext', 'bc=ca:true', '-keystore', root, *common)
    keytool('-exportcert', '-alias', 'root', '-keystore', root, '-storepass', password, '-rfc', '-file', target / 'root.pem')
    keytool('-importcert', '-alias', 'root', '-keystore', trust, '-file', target / 'root.pem', *common)
    for name, expired in [('server', False), ('expired', True)]:
        store = target / (name + '.p12')
        keytool('-genkeypair', '-alias', 'server', '-keyalg', 'EC', '-groupname', 'secp256r1', '-dname', 'CN=resolver.invalid', '-keystore', store, *common)
        keytool('-certreq', '-alias', 'server', '-keystore', store, '-storepass', password, '-file', target / (name + '.csr'))
        keytool('-gencert', '-alias', 'root', '-keystore', root, '-storepass', password, '-infile', target / (name + '.csr'),
                '-outfile', target / (name + '.pem'), '-rfc', '-ext', 'SAN=dns:resolver.invalid', '-ext', 'KU=digitalSignature', '-ext', 'EKU=serverAuth',
                '-startdate', date(-30 if expired else -1), '-validity', '1' if expired else '3650')
        keytool('-importcert', '-alias', 'root', '-keystore', store, '-file', target / 'root.pem', *common)
        keytool('-importcert', '-alias', 'server', '-keystore', store, '-file', target / (name + '.pem'), *common)
    report = {'status': 'PASS', 'owned_fixtures_only': True, 'os_trust_modified': False, 'network_used': False,
              'hostname': 'resolver.invalid', 'password': password, 'steps': steps,
              'files_sha256': {name: hashlib.sha256((target / name).read_bytes()).hexdigest() for name in ('trust.p12', 'server.p12', 'expired.p12')}}
    (target / 'DNS-TLS-FIXTURE.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8', newline='\n')
    print(json.dumps({'status': 'PASS', 'directory': str(target), 'report': str(target / 'DNS-TLS-FIXTURE.json'), 'os_trust_modified': False}, ensure_ascii=False))


if __name__ == '__main__':
    main()
