# Owned local fixtures only. Never installs Npcap, requests UAC, launches the application,
# changes services/routes, or replaces an existing user runtime.
[CmdletBinding()]
param([string]$FixtureRoot, [string]$Report)
$ErrorActionPreference = 'Stop'
$modulePath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\bootstrap-common.psm1'))
$FixtureRoot = [IO.Path]::GetFullPath($FixtureRoot)
[void][IO.Directory]::CreateDirectory($FixtureRoot)
$results = New-Object 'Collections.Generic.List[object]'
function Reset-Module { Remove-Module bootstrap-common -ErrorAction SilentlyContinue; Import-Module $modulePath -Force; Get-Module bootstrap-common }
function Assert([bool]$Value, [string]$Reason) { if (-not $Value) { throw "Fixture assertion: $Reason" } }
function Case([string]$Name, [scriptblock]$Run) {
    try { & $Run; $results.Add([pscustomobject]@{name=$Name;passed=$true}) }
    catch { $results.Add([pscustomobject]@{name=$Name;passed=$false;error=$_.Exception.Message}) }
}
function Rejected([scriptblock]$Run) { try { & $Run | Out-Null; return $false } catch { return $_.Exception.Message -like 'MultiRoute:*' } }
function Zip([string]$File, [string[]]$Names, [bool]$Link = $false) {
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $stream = [IO.File]::Open($File,[IO.FileMode]::CreateNew)
    $zip = New-Object IO.Compression.ZipArchive($stream,[IO.Compression.ZipArchiveMode]::Create)
    try { foreach($name in $Names) { $entry=$zip.CreateEntry($name);if($Link){$entry.ExternalAttributes=([int]40960 -shl 16)};$out=$entry.Open();try{$bytes=[Text.Encoding]::UTF8.GetBytes('owned fixture');$out.Write($bytes,0,$bytes.Length)}finally{$out.Dispose()} } }
    finally { $zip.Dispose();$stream.Dispose() }
}
$module = Reset-Module
Case 'PS5.1 scripts parse' {
    foreach($file in @(Get-ChildItem (Join-Path $PSScriptRoot '..') -Filter '*.ps*')) {
        $tokens=$null;$errors=$null;[Management.Automation.Language.Parser]::ParseFile($file.FullName,[ref]$tokens,[ref]$errors)|Out-Null
        Assert (-not $errors) $file.Name
    }
}
Case 'Source checkout rejects before download' {
    Assert (Rejected { & $module {param($r) Assert-MrPayload $r} $FixtureRoot }) 'Missing JAR/helper must reject.'
}
Case 'Path outside fixture rejected' {
    Assert (Rejected { & $module {param($r) Assert-MrSafePath ([IO.Path]::Combine($r,'..','outside')) $r} $FixtureRoot }) 'Traversal must reject.'
}
Case 'Safe child path accepted' {
    $value=& $module {param($r) Assert-MrSafePath ([IO.Path]::Combine($r,'inside')) $r} $FixtureRoot
    Assert ($value -eq [IO.Path]::Combine($FixtureRoot,'inside')) 'Normal path should survive.'
}
Case 'Real owned private Java full modules' {
    $java=Join-Path $FixtureRoot 'real-java\runtime\bin\java.exe'
    Assert ([IO.File]::Exists($java)) 'Run owned private Java setup fixture first.'
    Assert (& $module {param($j) Test-MrJava $j} $java) 'Pinned JRE must pass version/architecture/modules.'
}
Case 'Pinned Npcap signature valid without execution' {
    $file=Join-Path $FixtureRoot 'npcap-1.89.exe.download-only'
    & $module {param($f) Assert-MrNpcapSignature $f} $file
}
Case 'Bad checksum rejects' {
    $file=Join-Path $FixtureRoot ('bad-hash-'+[guid]::NewGuid().ToString('N'));[IO.File]::WriteAllText($file,'owned fixture')
    Assert (Rejected { & $module {param($f) Assert-MrHash $f ('0'*64)} $file }) 'Checksum must reject.'
}
Case 'Unknown Npcap publisher rejected' {
    $m=Reset-Module
    & $m {
        function script:Assert-MrHash { }
        function script:Get-AuthenticodeSignature {
            $cert=[pscustomobject]@{Subject='CN=Other Publisher, O=Other Publisher'}
            $cert|Add-Member ScriptMethod GetNameInfo { 'Other Publisher' }
            [pscustomobject]@{Status=[Management.Automation.SignatureStatus]::Valid;SignerCertificate=$cert}
        }
    }
    Assert (Rejected { & $m {Assert-MrNpcapSignature 'owned fixture'} }) 'Unknown publisher must not reach UAC.'
}
Case 'Invalid Npcap signature rejected' {
    $m=Reset-Module
    & $m { function script:Assert-MrHash{};function script:Get-AuthenticodeSignature{[pscustomobject]@{Status=[Management.Automation.SignatureStatus]::HashMismatch;SignerCertificate=$null}} }
    Assert (Rejected { & $m {Assert-MrNpcapSignature 'owned fixture'} }) 'Invalid signature must not reach UAC.'
}
$fakeJava=Join-Path $FixtureRoot 'java-fixture.exe';[IO.File]::WriteAllText($fakeJava,'not executed')
foreach($case in @(
    @{name='Java16 rejects';major='16';arch='amd64';modules="java.desktop@16`njava.net.http@16`njdk.httpserver@16`njava.xml@16";expected=$false},
    @{name='JavaARM rejects';major='21';arch='aarch64';modules="java.desktop@21`njava.net.http@21`njdk.httpserver@21`njava.xml@21";expected=$false},
    @{name='Trimmed Java missing desktop rejects';major='21';arch='amd64';modules="java.net.http@21`njdk.httpserver@21`njava.xml@21";expected=$false},
    @{name='Trimmed Java missing httpserver rejects';major='21';arch='amd64';modules="java.desktop@21`njava.net.http@21`njava.xml@21";expected=$false},
    @{name='FullJava17 accepts';major='17';arch='amd64';modules="java.desktop@17`njava.net.http@17`njdk.httpserver@17`njava.xml@17";expected=$true}
)) {
    Case $case.name {
        $m=Reset-Module
        & $m {param($c) $script:Fixture=$c;function script:Invoke-MrJavaProbe {param($exe,$a) if($a -contains '--list-modules'){$script:Fixture.modules}else{"java.specification.version = $($script:Fixture.major)`nos.arch = $($script:Fixture.arch)"}} } $case
        $actual=& $m {param($j) Test-MrJava $j} $fakeJava
        Assert ($actual -eq $case.expected) 'Version/arch/module validation mismatch.'
    }
}
Case 'Offline prerequisites reuse and argument forwarding' {
    $m=Reset-Module
    & $m {
        function script:Assert-MrWindows{};function script:Assert-MrPayload{};function script:Find-MrJava{'owned-java.exe'}
        function script:Install-MrJava{throw 'Fixture: forbidden install'};function script:Install-MrNpcap{throw 'Fixture: forbidden UAC'}
        function script:Get-MrNpcapState{@{Present=$true;Ready=$true;AdminOnly=$false}}
        function script:Start-MrApplication {param($r,$j,$a) $script:Launched=$a;return 0}
    }
    $exit=& $m {param($r) Invoke-MrBootstrap $r Start @('--headless','--port','8766','--data-dir','C:\own space\Unicode')} $FixtureRoot
    Assert ($exit -eq 0) 'Offline should launch.'
    $args=& $m {$script:Launched}
    Assert ($args.Count -eq 5 -and $args[4] -eq 'C:\own space\Unicode') 'Arguments should remain intact.'
}
Case 'Missing prerequisites invoke installers before launch mocks only' {
    $m=Reset-Module
    & $m {
        $script:Order=New-Object 'Collections.Generic.List[string]'
        function script:Assert-MrWindows{};function script:Assert-MrPayload{};function script:Find-MrJava{$null}
        function script:Install-MrJava{$script:Order.Add('java');'owned-java.exe'}
        function script:Get-MrNpcapState{@{Present=$false;Ready=$false;AdminOnly=$false}}
        function script:Install-MrNpcap{$script:Order.Add('npcap');@{Present=$true;Ready=$true;AdminOnly=$false}}
        function script:Start-MrApplication{$script:Order.Add('launch');0}
    }
    $exit=& $m {param($r) Invoke-MrBootstrap $r Deploy @()} $FixtureRoot
    $order=& $m {$script:Order -join ','}
    Assert ($exit -eq 0 -and $order -eq 'java,npcap,launch') 'Install order incorrect.'
}
Case 'Cancelled Npcap cannot launch' {
    $m=Reset-Module
    & $m {
        function script:Assert-MrWindows{};function script:Assert-MrPayload{};function script:Find-MrJava{'owned-java.exe'}
        function script:Get-MrNpcapState{@{Present=$false;Ready=$false;AdminOnly=$false}}
        function script:Install-MrNpcap{Stop-MrSetup 'Owned mocked cancellation.'}
        function script:Start-MrApplication{throw 'Fixture: forbidden launch'}
    }
    Assert (Rejected { & $m {param($r) Invoke-MrBootstrap $r Deploy @()} $FixtureRoot }) 'Cancellation must fail closed.'
}
Case 'Stopped existing Npcap cannot silently change service' {
    $m=Reset-Module
    & $m {
        function script:Assert-MrWindows{};function script:Assert-MrPayload{};function script:Find-MrJava{'owned-java.exe'}
        function script:Get-MrNpcapState{@{Present=$true;Ready=$false;AdminOnly=$false}}
        function script:Install-MrNpcap{throw 'Fixture: forbidden reinstall'}
        function script:Start-MrApplication{throw 'Fixture: forbidden launch'}
    }
    Assert (Rejected { & $m {param($r) Invoke-MrBootstrap $r Start @()} $FixtureRoot }) 'Stopped service must report a clear error.'
}
Case 'Windows native argv quote roundtrip' {
    Add-Type -TypeDefinition @'
using System;using System.Runtime.InteropServices;
public static class MRFixtureArgv {
 [DllImport("shell32.dll",CharSet=CharSet.Unicode)]static extern IntPtr CommandLineToArgvW(string cmd,out int argc);
 [DllImport("kernel32.dll")]static extern IntPtr LocalFree(IntPtr p);
 public static string[] Parse(string cmd){int n;IntPtr p=CommandLineToArgvW(cmd,out n);if(p==IntPtr.Zero)throw new Exception("argv allocation");try{string[] a=new string[n];for(int i=0;i<n;i++)a[i]=Marshal.PtrToStringUni(Marshal.ReadIntPtr(p,i*IntPtr.Size));return a;}finally{LocalFree(p);}}
}
'@
    $m=Reset-Module
    $values=@('','C:\own space\','one"two','two\"quotes','https://example.org/file?x=1&y=2','&&|;',([string][char]0x4e2d+[char]0x6587))
    $line=& $m {param($v)ConvertTo-MrNativeArguments $v} $values
    $roundtrip=[MRFixtureArgv]::Parse('"java.exe" '+$line)
    Assert ($roundtrip.Length -eq $values.Length+1) 'Argument count mismatch.'
    for($i=0;$i -lt $values.Length;$i++){Assert ($roundtrip[$i+1] -ceq $values[$i]) 'Argument bytes/value altered.'}
}
Case 'Native PE x64 validation without DLL loading' {
    $m=Reset-Module;$file=Join-Path $FixtureRoot ('pe-'+[guid]::NewGuid().ToString('N')+'.dll')
    $bytes=New-Object byte[] 256;$bytes[0]=77;$bytes[1]=90;$bytes[60]=128;$bytes[128]=80;$bytes[129]=69;$bytes[132]=100;$bytes[133]=134
    [IO.File]::WriteAllBytes($file,$bytes)
    Assert (& $m {param($f)Test-MrPe64 $f} $file) 'x64 PE should pass.'
    $bytes[132]=76;$bytes[133]=1;[IO.File]::WriteAllBytes($file,$bytes)
    Assert (-not (& $m {param($f)Test-MrPe64 $f} $file)) 'x86 PE must reject.'
}
Case 'Reparse junction inside fixture rejects' {
    $m=Reset-Module;$target=Join-Path $FixtureRoot ('junction-target-'+[guid]::NewGuid().ToString('N'));[void][IO.Directory]::CreateDirectory($target)
    $junction=Join-Path $FixtureRoot ('junction-'+[guid]::NewGuid().ToString('N'))
    New-Item -ItemType Junction -Path $junction -Target $target | Out-Null
    Assert (Rejected { & $m {param($p,$r)Assert-MrSafePath $p $r} (Join-Path $junction 'file') $FixtureRoot }) 'Junction must reject.'
}
Case 'Both actual CMD wrappers preserve CLI args owned stub only' {
    $folder=Join-Path $FixtureRoot ('cmd fixture '+[guid]::NewGuid().ToString('N'));[void][IO.Directory]::CreateDirectory((Join-Path $folder 'windows'))
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot '..\bootstrap.ps1') -Destination (Join-Path $folder 'windows\bootstrap.ps1')
    $stub=@'
function Invoke-MrBootstrap {param($Root,$Mode,[string[]]$ApplicationArguments)
[IO.File]::WriteAllText((Join-Path $Root 'owned-argv.json'),(@{Mode=$Mode;Arguments=$ApplicationArguments}|ConvertTo-Json -Depth 5),[Text.Encoding]::UTF8)
return 19
}
Export-ModuleMember -Function Invoke-MrBootstrap
'@
    [IO.File]::WriteAllText((Join-Path $folder 'windows\bootstrap-common.psm1'),$stub,[Text.Encoding]::ASCII)
    foreach($mode in @('Start','Deploy')){
        $name=$mode+'-Windows.cmd';Copy-Item -LiteralPath (Join-Path $PSScriptRoot ('..\..\'+$name)) -Destination (Join-Path $folder $name)
        & (Join-Path $folder $name) --headless --port 8766 --data-dir 'C:\owned fixture with space' | Out-Null
        Assert ($LASTEXITCODE -eq 19) 'Exit code must propagate.'
        $captured=[IO.File]::ReadAllText((Join-Path $folder 'owned-argv.json'))|ConvertFrom-Json
        Assert ($captured.Mode -eq $mode -and $captured.Arguments.Count -eq 5 -and $captured.Arguments[4] -eq 'C:\owned fixture with space') 'CMD forwarding mismatch.'
    }
}
$zipRoot='jdk-21.0.12.1+1-jre'
foreach($case in @(
    @{name='ZIP traversal rejects';names=@("$zipRoot/../outside")},
    @{name='ZIP absolute path rejects';names=@('/outside')},
    @{name='ZIP ADS path rejects';names=@("$zipRoot/bin/java.exe:stream")},
    @{name='ZIP Windows backslash path rejects';names=@("$zipRoot\bin\java.exe")},
    @{name='ZIP unexpected top level rejects';names=@('other/bin/java.exe')},
    @{name='ZIP reserved device rejects';names=@("$zipRoot/NUL.txt")},
    @{name='ZIP case insensitive collision rejects';names=@("$zipRoot/bin/Java.exe","$zipRoot/bin/java.exe")},
    @{name='ZIP symbolic link rejects';names=@("$zipRoot/bin/java.exe");link=$true}
)) {
    Case $case.name {
        $m=Reset-Module;$id=[guid]::NewGuid().ToString('N');$file=Join-Path $FixtureRoot ($id+'.zip');$dest=Join-Path $FixtureRoot ($id+'-extract')
        Zip $file $case.names ([bool]$case.link)
        Assert (Rejected { & $m {param($f,$d) Expand-MrJavaArchive $f $d} $file $dest }) 'Unsafe archive must reject.'
    }
}
Case 'ZIP ordinary files extract exactly' {
    $m=Reset-Module;$id=[guid]::NewGuid().ToString('N');$file=Join-Path $FixtureRoot ($id+'.zip');$dest=Join-Path $FixtureRoot ($id+'-extract')
    Zip $file @("$zipRoot/bin/java.exe","$zipRoot/lib/modules")
    & $m {param($f,$d) Expand-MrJavaArchive $f $d} $file $dest
    Assert ([IO.File]::ReadAllText((Join-Path $dest "$zipRoot/bin/java.exe")) -eq 'owned fixture') 'Extraction mismatch.'
}
Case 'Atomic private Java commit preserves prior runtime' {
    $m=Reset-Module;$root=Join-Path $FixtureRoot ('commit-'+[guid]::NewGuid().ToString('N'));[void][IO.Directory]::CreateDirectory((Join-Path $root 'runtime'));[IO.File]::WriteAllText((Join-Path $root 'runtime/old.marker'),'keep')
    $zip=Join-Path $root 'owned.zip';Zip $zip @("$zipRoot/bin/java.exe");$sha=(Get-FileHash $zip -Algorithm SHA256).Hash.ToLowerInvariant()
    & $m {param($z,$s) $script:OwnedZip=$z;$script:JavaSha256=$s;function script:Invoke-MrDownload{param($u,$d,$l)Copy-Item -LiteralPath $script:OwnedZip -Destination $d};function script:Test-MrJava{$true} } $zip $sha
    $java=& $m {param($r)Install-MrJava $r} $root
    Assert ([IO.File]::Exists($java)) 'Candidate should commit.'
    $backup=@(Get-ChildItem -LiteralPath $root -Filter 'runtime.previous-*')
    Assert ($backup.Count -eq 1 -and [IO.File]::ReadAllText((Join-Path $backup[0].FullName 'old.marker')) -eq 'keep') 'Backup should preserve old data.'
}
Case 'Failed atomic commit rolls prior runtime back' {
    $m=Reset-Module;$root=Join-Path $FixtureRoot ('rollback-'+[guid]::NewGuid().ToString('N'));[void][IO.Directory]::CreateDirectory((Join-Path $root 'runtime'));[IO.File]::WriteAllText((Join-Path $root 'runtime/old.marker'),'keep')
    $zip=Join-Path $root 'owned.zip';Zip $zip @("$zipRoot/bin/java.exe");$sha=(Get-FileHash $zip -Algorithm SHA256).Hash.ToLowerInvariant()
    & $m {
        param($z,$s)$script:OwnedZip=$z;$script:JavaSha256=$s
        function script:Invoke-MrDownload{param($u,$d,$l)Copy-Item -LiteralPath $script:OwnedZip -Destination $d};function script:Test-MrJava{$true}
        function script:Move-Item{param($LiteralPath,$Destination)if($LiteralPath -like '*\unpacked\*'){throw 'Owned simulated commit error'};Microsoft.PowerShell.Management\Move-Item -LiteralPath $LiteralPath -Destination $Destination}
    } $zip $sha
    Assert (Rejected { & $m {param($r)Install-MrJava $r} $root }) 'Commit should reject.'
    Assert ([IO.File]::ReadAllText((Join-Path $root 'runtime/old.marker')) -eq 'keep') 'Old runtime must restore.'
}
Case 'Bootstrap source never changes AV or routing' {
    $s=[IO.File]::ReadAllText($modulePath)
    Assert ($s -notmatch 'Set-MpPreference|Add-MpPreference|netsh|route\.exe|Set-ExecutionPolicy|Start-Service|Stop-Service|/S(?:\s|$)') 'No global bypass/service/routing/silent installer operations.'
    Assert ($s -match 'Start-Process[^\r\n]+-Verb RunAs -Wait -PassThru') 'Npcap has interactive authorized installer only.'
}
$failed=@($results|Where-Object{-not $_.passed})
$output=[pscustomobject]@{status=if($failed.Count){'FAIL'}else{'PASS'};powershell=$PSVersionTable.PSVersion.ToString();checks=$results.Count;results=$results;not_performed=@('Npcap installation/UAC','application launch','native helper capture','system Java replacement','service/security/routing changes')}
[IO.File]::WriteAllText([IO.Path]::GetFullPath($Report),($output|ConvertTo-Json -Depth 8),[Text.Encoding]::UTF8)
$output|Select-Object status,powershell,checks|ConvertTo-Json
if($failed.Count){$failed|ConvertTo-Json -Depth 4;exit 1}
