#!/usr/bin/env python3
"""Compile the current saved Java sources into a Java 17 application JAR."""
from pathlib import Path
import argparse,hashlib,json,os,re,shutil,subprocess,tempfile,zipfile

root=Path(__file__).resolve().parent.parent
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--javac',help='Path to a Java 17+ javac executable')
parser.add_argument('--platform',choices=['all','windows','linux','macos'],default='all',help='Select runtime JNI JARs in the manifest')
args=parser.parse_args()
javac=args.javac or (str(Path(os.environ['JAVA_HOME'])/'bin'/('javac.exe' if os.name=='nt' else 'javac')) if os.environ.get('JAVA_HOME') else shutil.which('javac'))
if not javac:parser.error('JDK 17+ is required; set JAVA_HOME or use --javac.')
sources=sorted((root/'src/multiroute').glob('*.java'))
resources=sorted((root/'src/multiroute/i18n').glob('*.json'))
if {path.stem for path in resources}!={'zh','ko','ar','it'}:parser.error('The four additional UTF-8 language catalogs are required.')
libraries=sorted((root/'lib').glob('*.jar'))
if not sources or not libraries:parser.error('Extract/clone the complete repository including src/ and lib/.')
verification=root/'scripts/dependencies.sha256.json'
if verification.is_file():
    hashes=json.loads(verification.read_text(encoding='utf-8'))
    for file in libraries:
        expected=hashes.get(file.name)
        if not expected or hashlib.sha256(file.read_bytes()).hexdigest()!=expected:parser.error('Dependency SHA-256 mismatch: '+file.name)
selected=[p for p in libraries if args.platform=='all' or not p.name.startswith(('jlibtorrent-linux-','jlibtorrent-macosx-','jlibtorrent-windows-')) or
          p.name.startswith('jlibtorrent-'+{'windows':'windows-','linux':'linux-','macos':'macosx-'}.get(args.platform,''))]
def attribute(name,value):
    line=name+': '+value;chunks=[line[:70]];line=line[70:]
    while line:chunks.append(' '+line[:69]);line=line[69:]
    return '\r\n'.join(chunks)+'\r\n'
target=root/'MultiRoute-Resume.jar'
with tempfile.TemporaryDirectory(prefix='multiroute-java-build-') as folder:
    classes=Path(folder)/'classes';classes.mkdir()
    subprocess.run([str(javac),'--release','17','-encoding','UTF-8','-cp',os.pathsep.join(map(str,libraries)),'-d',str(classes),*map(str,sources)],check=True)
    names={s.stem for s in sources};compiled=sorted(classes.rglob('*.class'))
    if not compiled or not all(p.stem.split('$')[0] in names and p.read_bytes()[:8]==bytes.fromhex('cafebabe0000003d') for p in compiled):raise SystemExit('Unexpected compiler output.')
    version=re.search(r'static final String VERSION = "([0-9.]+)";', (root/'src/multiroute/Main.java').read_text(encoding='utf-8')).group(1)
    manifest='Manifest-Version: 1.0\r\nMain-Class: multiroute.Main\r\nImplementation-Version: '+version+'\r\n'
    manifest+=attribute('Class-Path',' '.join('lib/'+p.name for p in selected))+'\r\n'
    candidate=Path(folder)/'MultiRoute-Resume.jar'
    with zipfile.ZipFile(candidate,'w',zipfile.ZIP_DEFLATED) as archive:
        archive.writestr('META-INF/MANIFEST.MF',manifest)
        for file in compiled:archive.write(file,file.relative_to(classes).as_posix())
        for file in resources:
            json.loads(file.read_text(encoding='utf-8'))
            archive.write(file,'multiroute/i18n/'+file.name)
    # Existing local builds remain recoverable; the verified distribution is not touched.
    if target.exists():
        backup=target.with_name('MultiRoute-Resume.previous-'+hashlib.sha256(target.read_bytes()).hexdigest()[:12]+'.jar')
        if not backup.exists():shutil.copy2(target,backup)
    shutil.copyfile(candidate,target)
print('Built '+str(target))
print('SHA-256 '+hashlib.sha256(target.read_bytes()).hexdigest())
