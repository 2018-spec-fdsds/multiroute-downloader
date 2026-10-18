Set-StrictMode -Version 2.0
$ErrorActionPreference = 'Stop'
$script:JavaUrl = 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jre_x64_windows_hotspot_21.0.12.1_1.zip'
$script:JavaSha256 = 'd35f31e712f0fcf6ac5a093edc90204fbff22f720ba3950bd09d331d5e621636'
$script:JavaArchiveRoot = 'jdk-21.0.12.1+1-jre'
$script:NpcapUrl = 'https://npcap.com/dist/npcap-1.89.exe'
$script:NpcapSha256 = '8aed85e900d783d1308506e919587d3e540451947af8a82f2d04f819e44305cc'
$script:NpcapPublisher = 'Nmap Software LLC'

function Stop-MrSetup([string]$Message) { throw "MultiRoute: $Message" }

function Assert-MrSafePath([string]$Path, [string]$Root = '') {
    $full = [IO.Path]::GetFullPath($Path)
    if ($Root) {
        $parent = [IO.Path]::GetFullPath($Root).TrimEnd('\','/')
        if ($full -ne $parent -and -not $full.StartsWith($parent + '\', [StringComparison]::OrdinalIgnoreCase)) { Stop-MrSetup 'A setup path is outside the application folder.' }
    }
    $part = $full
    while ($part) {
        if (Test-Path -LiteralPath $part) {
            $entry = Get-Item -LiteralPath $part -Force
            if (($entry.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) { Stop-MrSetup 'Setup paths cannot contain a junction or symbolic link. Extract to a normal user folder.' }
        }
        $next = [IO.Path]::GetDirectoryName($part)
        if ($next -eq $part) { break }
        $part = $next
    }
    return $full
}

function Assert-MrWindows {
    $arch = if ($env:PROCESSOR_ARCHITEW6432) { $env:PROCESSOR_ARCHITEW6432 } else { $env:PROCESSOR_ARCHITECTURE }
    if ($arch -ne 'AMD64' -or -not [Environment]::Is64BitProcess) { Stop-MrSetup 'This distribution requires Windows 11 x64 and native 64-bit PowerShell. Windows ARM64 is not a supported native target.' }
    $build = (Get-ItemProperty -LiteralPath 'HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion').CurrentBuildNumber
    if ([int]$build -lt 22000) { Stop-MrSetup 'Windows 11 or newer is required for this launcher.' }
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($identity)
    if ($principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) { Stop-MrSetup 'Open the launcher normally, without Run as administrator. Only the Npcap installer receives UAC authorization.' }
}

function Assert-MrPayload([string]$Root) {
    Assert-MrSafePath $Root | Out-Null
    foreach ($relative in @('MultiRoute-Downloader.jar','native\windows-x64\gateway-net.exe')) {
        $file = Assert-MrSafePath (Join-Path $Root $relative) $Root
        if (-not [IO.File]::Exists($file)) { Stop-MrSetup 'The runnable release is incomplete. Download and extract the Windows distribution from GitHub Releases, or build the Java JAR and native Windows helper from source. A source-only checkout is not a ready-to-run release.' }
    }
}

function ConvertTo-MrNativeArguments([string[]]$Values) {
    $quoted = foreach ($value in $Values) {
        # Windows argv quoting: double backslashes before quotes and terminal backslashes.
        $escaped = [regex]::Replace($value, '(\\*)"', '$1$1\"')
        $escaped = [regex]::Replace($escaped, '(\\+)$', '$1$1')
        '"' + $escaped + '"'
    }
    return ($quoted -join ' ')
}

function Invoke-MrJavaProbe([string]$Executable, [string[]]$Arguments) {
    $info = New-Object Diagnostics.ProcessStartInfo
    $info.FileName = $Executable
    $info.Arguments = ConvertTo-MrNativeArguments $Arguments
    $info.UseShellExecute = $false; $info.CreateNoWindow = $true
    $info.RedirectStandardOutput = $true; $info.RedirectStandardError = $true
    foreach ($name in @('JAVA_TOOL_OPTIONS','_JAVA_OPTIONS','JDK_JAVA_OPTIONS')) { $info.EnvironmentVariables.Remove($name) }
    $process = New-Object Diagnostics.Process
    $process.StartInfo = $info
    try {
        [void]$process.Start()
        $stdout = $process.StandardOutput.ReadToEndAsync(); $stderr = $process.StandardError.ReadToEndAsync()
        if (-not $process.WaitForExit(15000)) { $process.Kill(); return $null }
        $process.WaitForExit()
        if ($process.ExitCode -ne 0) { return $null }
        $text = $stdout.Result + "`n" + $stderr.Result
        if ($text.Length -gt 1048576) { return $null }
        return $text
    } catch { return $null } finally { $process.Dispose() }
}

function Test-MrJava([string]$Executable) {
    if (-not $Executable -or -not [IO.File]::Exists($Executable)) { return $false }
    $properties = Invoke-MrJavaProbe $Executable @('-XshowSettings:properties','-version')
    if (-not $properties) { return $false }
    $major = [regex]::Match($properties, '(?m)^\s*java\.specification\.version\s*=\s*(\d+)\s*$')
    $arch = [regex]::Match($properties, '(?m)^\s*os\.arch\s*=\s*(amd64|x86_64)\s*$')
    if (-not $major.Success -or [int]$major.Groups[1].Value -lt 17 -or -not $arch.Success) { return $false }
    $modules = Invoke-MrJavaProbe $Executable @('--list-modules')
    foreach ($required in @('java.desktop','java.net.http','jdk.httpserver','java.xml')) {
        if (-not $modules -or $modules -notmatch ('(?m)^' + [regex]::Escape($required) + '(?:@[^\r\n]+)?\r?$')) { return $false }
    }
    return $true
}

function Find-MrJava([string]$Root) {
    if ($env:JAVA_EXE) {
        $candidate = $env:JAVA_EXE
        if (-not [IO.Path]::IsPathRooted($candidate)) {
            $command = Get-Command $candidate -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($command) { $candidate = $command.Source }
        }
        if (-not (Test-MrJava $candidate)) { Stop-MrSetup 'JAVA_EXE must identify a working full Java 17+ runtime for x64. Correct or remove that override and retry.' }
        return $candidate
    }
    $privateJava = Assert-MrSafePath (Join-Path $Root 'runtime\bin\java.exe') $Root
    $candidates = @($privateJava)
    if ($env:JAVA_HOME) { $candidates += Join-Path $env:JAVA_HOME 'bin\java.exe' }
    $command = Get-Command java.exe -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($command) { $candidates += $command.Source }
    foreach ($candidate in ($candidates | Select-Object -Unique)) { if (Test-MrJava $candidate) { return $candidate } }
    return $null
}

function New-MrStage([string]$Root, [string]$Purpose) {
    $stage = Assert-MrSafePath (Join-Path $Root ('.multiroute-' + $Purpose + '-' + [guid]::NewGuid().ToString('N'))) $Root
    [void][IO.Directory]::CreateDirectory($stage)
    $acl = New-Object Security.AccessControl.DirectorySecurity
    $acl.SetAccessRuleProtection($true, $false)
    $owner = [Security.Principal.WindowsIdentity]::GetCurrent().User
    $acl.SetOwner($owner)
    foreach ($sid in @($owner, (New-Object Security.Principal.SecurityIdentifier('S-1-5-18')), (New-Object Security.Principal.SecurityIdentifier('S-1-5-32-544')))) {
        $rule = New-Object Security.AccessControl.FileSystemAccessRule($sid, 'FullControl', 'ContainerInherit,ObjectInherit', 'None', 'Allow')
        [void]$acl.AddAccessRule($rule)
    }
    try { [IO.Directory]::SetAccessControl($stage, $acl) }
    catch { Stop-MrSetup 'Setup needs a writable folder with Windows access-control support, such as a local NTFS folder. Extract there and retry.' }
    return $stage
}

function Invoke-MrDownload([string]$Url, [string]$Destination, [long]$Limit) {
    Assert-MrSafePath $Destination | Out-Null
    $allowed = @('github.com','release-assets.githubusercontent.com','objects.githubusercontent.com','npcap.com')
    $uri = [Uri]$Url
    $response = $null; $inputStream = $null; $outputStream = $null
    $priorTls = [Net.ServicePointManager]::SecurityProtocol
    $timer = [Diagnostics.Stopwatch]::StartNew()
    try {
        [Net.ServicePointManager]::SecurityProtocol = $priorTls -bor [Net.SecurityProtocolType]::Tls12
        for ($hop = 0; $hop -lt 8; $hop++) {
            if ($uri.Scheme -ne 'https' -or $uri.UserInfo -or $uri.Port -ne 443 -or $allowed -notcontains $uri.DnsSafeHost) { Stop-MrSetup 'Download redirect is outside the approved HTTPS sources.' }
            $request = [Net.HttpWebRequest]::Create($uri)
            $request.AllowAutoRedirect = $false; $request.Timeout = 20000; $request.ReadWriteTimeout = 20000
            $response = $request.GetResponse()
            $code = [int]$response.StatusCode
            if ($code -ge 300 -and $code -le 399) {
                $location = $response.Headers['Location']; $response.Dispose(); $response = $null
                if (-not $location) { Stop-MrSetup 'The official download returned an empty redirect.' }
                $uri = New-Object Uri($uri, $location)
                continue
            }
            if ($code -ne 200) { Stop-MrSetup 'The official download returned an unexpected status.' }
            break
        }
        if (-not $response -or $response.ContentLength -gt $Limit) { Stop-MrSetup 'The official download exceeded its size or redirect limit.' }
        $inputStream = $response.GetResponseStream()
        $outputStream = New-Object IO.FileStream($Destination, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None)
        $buffer = New-Object byte[] 65536
        $received = 0L
        while (($read = $inputStream.Read($buffer, 0, $buffer.Length)) -gt 0) {
            $received += $read
            if ($received -gt $Limit -or $timer.Elapsed.TotalSeconds -gt 900) { Stop-MrSetup 'The official download exceeded its time or size limit.' }
            $outputStream.Write($buffer, 0, $read)
        }
        if ($received -eq 0 -or ($response.ContentLength -ge 0 -and $received -ne $response.ContentLength)) { Stop-MrSetup 'The official download was incomplete.' }
    } catch {
        if ($_.Exception.Message -like 'MultiRoute:*') { throw }
        Stop-MrSetup 'Download failed. Check Internet access to GitHub and npcap.com, then reopen the launcher. Existing compatible software works without downloads.'
    } finally {
        if ($outputStream) { $outputStream.Dispose() }
        if ($inputStream) { $inputStream.Dispose() }
        if ($response) { $response.Dispose() }
        [Net.ServicePointManager]::SecurityProtocol = $priorTls
    }
}

function Assert-MrHash([string]$File, [string]$Expected) {
    if ($Expected -notmatch '^[0-9a-f]{64}$' -or (Get-FileHash -LiteralPath $File -Algorithm SHA256).Hash.ToLowerInvariant() -ne $Expected) { Stop-MrSetup 'Downloaded file SHA-256 does not match the pinned official file. It will not be installed.' }
}

function Expand-MrJavaArchive([string]$Archive, [string]$Destination) {
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    Assert-MrSafePath $Destination | Out-Null
    [void][IO.Directory]::CreateDirectory($Destination)
    $zip = [IO.Compression.ZipFile]::OpenRead($Archive)
    $seen = New-Object 'Collections.Generic.HashSet[string]' ([StringComparer]::OrdinalIgnoreCase)
    try {
        $total = 0L
        if ($zip.Entries.Count -gt 5000 -or $zip.Entries.Count -eq 0) { Stop-MrSetup 'Unexpected Java archive entry count.' }
        foreach ($entry in $zip.Entries) {
            $name = $entry.FullName
            if ($name -notlike ($script:JavaArchiveRoot + '/*') -or $name.Contains('\') -or $name.Contains(':') -or $name.StartsWith('/')) { Stop-MrSetup 'Unexpected path in the Java archive.' }
            foreach ($part in $name.TrimEnd('/').Split('/')) {
                if (-not $part -or $part -in @('.','..') -or $part -match '[ .]$' -or $part -match '^(?i:CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\.|$)') { Stop-MrSetup 'Unsafe path in the Java archive.' }
            }
            $mode = ($entry.ExternalAttributes -shr 16) -band 61440
            if ($mode -eq 40960 -or ($entry.ExternalAttributes -band 1024) -ne 0) { Stop-MrSetup 'Links are not accepted in the Java archive.' }
            $target = Assert-MrSafePath (Join-Path $Destination ($name.Replace('/', '\'))) $Destination
            if (-not $seen.Add($target.TrimEnd('\'))) { Stop-MrSetup 'Duplicate path in the Java archive.' }
            $total += $entry.Length
            if ($total -gt 805306368) { Stop-MrSetup 'Java archive is larger than the extraction limit.' }
            if ($name.EndsWith('/')) { [void][IO.Directory]::CreateDirectory($target); continue }
            [void][IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($target))
            Assert-MrSafePath $target $Destination | Out-Null
            $source = $entry.Open(); $output = $null
            try {
                $output = New-Object IO.FileStream($target, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None)
                $buffer = New-Object byte[] 65536
                $copied = 0L
                while (($read = $source.Read($buffer,0,$buffer.Length)) -gt 0) {
                    $copied += $read
                    if ($copied -gt $entry.Length -or $copied -gt 805306368) { Stop-MrSetup 'Java archive entry exceeded its declared extraction size.' }
                    $output.Write($buffer,0,$read)
                }
                if ($output.Length -ne $entry.Length) { Stop-MrSetup 'Incomplete Java archive entry.' }
            } finally { $source.Dispose(); if ($output) { $output.Dispose() } }
        }
    } finally { $zip.Dispose() }
}

function Install-MrJava([string]$Root) {
    $drive = New-Object IO.DriveInfo([IO.Path]::GetPathRoot($Root))
    if ($drive.AvailableFreeSpace -lt 536870912) { Stop-MrSetup 'At least 512 MiB must be free for private Java deployment; downloads need additional space.' }
    Write-Host 'Downloading verified Eclipse Temurin 21 for this application only. System Java will remain unchanged.'
    $stage = New-MrStage $Root 'java'
    $archive = Join-Path $stage 'runtime.zip'
    Invoke-MrDownload $script:JavaUrl $archive 134217728
    Assert-MrHash $archive $script:JavaSha256
    $unpack = Join-Path $stage 'unpacked'
    Expand-MrJavaArchive $archive $unpack
    $candidate = Join-Path $unpack $script:JavaArchiveRoot
    if (-not (Test-MrJava (Join-Path $candidate 'bin\java.exe'))) { Stop-MrSetup 'The verified Java runtime failed the x64/version/full-module checks.' }
    $destination = Assert-MrSafePath (Join-Path $Root 'runtime') $Root
    $previous = $null
    if (Test-Path -LiteralPath $destination) {
        $previous = Assert-MrSafePath (Join-Path $Root ('runtime.previous-' + [DateTime]::UtcNow.ToString('yyyyMMddHHmmss') + '-' + [guid]::NewGuid().ToString('N'))) $Root
        Move-Item -LiteralPath $destination -Destination $previous
    }
    try { Move-Item -LiteralPath $candidate -Destination $destination }
    catch {
        if ($previous -and -not (Test-Path -LiteralPath $destination)) { Move-Item -LiteralPath $previous -Destination $destination }
        Stop-MrSetup 'Private Java commit failed; the previous runtime was restored when possible. Staging files remain available for inspection.'
    }
    # Preserve failed staging/previous data; no recursive removal of computed paths.
    Remove-Item -LiteralPath $archive
    return (Join-Path $destination 'bin\java.exe')
}

function Test-MrPe64([string]$File) {
    if (-not [IO.File]::Exists($File)) { return $false }
    $stream = [IO.File]::OpenRead($File)
    $reader = New-Object IO.BinaryReader($stream)
    try {
        if ($reader.ReadUInt16() -ne 23117 -or $stream.Length -lt 64) { return $false }
        $stream.Position = 60; $offset = $reader.ReadUInt32()
        if ($offset -lt 64 -or $offset -gt $stream.Length - 6) { return $false }
        $stream.Position = $offset
        return ($reader.ReadUInt32() -eq 17744 -and $reader.ReadUInt16() -eq 34404)
    } catch { return $false } finally { $reader.Dispose(); $stream.Dispose() }
}

function Get-MrNpcapState {
    $directory = Join-Path ([Environment]::GetFolderPath([Environment+SpecialFolder]::System)) 'Npcap'
    $service = Get-Service -Name npcap -ErrorAction SilentlyContinue
    $dlls = (Test-MrPe64 (Join-Path $directory 'wpcap.dll')) -and (Test-MrPe64 (Join-Path $directory 'Packet.dll'))
    $adminOnly = $false
    $config = Get-ItemProperty -LiteralPath 'HKLM:\SYSTEM\CurrentControlSet\Services\npcap\Parameters' -ErrorAction SilentlyContinue
    if ($config -and $config.PSObject.Properties['AdminOnly']) { $adminOnly = [int]$config.AdminOnly -ne 0 }
    return @{ Present = [bool]($service -and $dlls); Ready = [bool]($service -and $dlls -and $service.Status -eq 'Running'); AdminOnly = $adminOnly }
}

function Assert-MrNpcapSignature([string]$File) {
    Assert-MrHash $File $script:NpcapSha256
    $signature = Get-AuthenticodeSignature -LiteralPath $File
    if ($signature.Status -ne [Management.Automation.SignatureStatus]::Valid -or -not $signature.SignerCertificate -or
        $signature.SignerCertificate.GetNameInfo([Security.Cryptography.X509Certificates.X509NameType]::SimpleName, $false) -ne $script:NpcapPublisher -or
        $signature.SignerCertificate.Subject -notmatch '(?:^|,\s*)O=Nmap Software LLC(?:,|$)') { Stop-MrSetup 'Npcap publisher/signature verification failed. The installer will not run.' }
}

function Install-MrNpcap([string]$Root) {
    Write-Host 'Npcap is missing. Downloading its official signed installer. Windows will request UAC; read and accept the Npcap license in its visible installer.'
    Write-Host 'Npcap Free Edition has license limits, including redistribution restrictions. See https://npcap.com/#download. No silent installation is performed.'
    $stage = New-MrStage $Root 'npcap'
    $file = Join-Path $stage 'npcap-1.89.exe'
    Invoke-MrDownload $script:NpcapUrl $file 33554432
    Assert-MrNpcapSignature $file
    Assert-MrSafePath $file $Root | Out-Null
    $process = Start-Process -FilePath $file -WorkingDirectory $stage -Verb RunAs -Wait -PassThru
    if ($process.ExitCode -in @(3010,350)) { Stop-MrSetup 'Npcap requires a Windows restart. Restart when convenient, then reopen this launcher.' }
    if ($process.ExitCode -ne 0) { Stop-MrSetup 'Npcap installation was cancelled or failed. Existing private Java is preserved; reopen the launcher when ready.' }
    $state = Get-MrNpcapState
    if (-not $state.Ready) { Stop-MrSetup 'Npcap installation finished but its service/DLLs are not ready. Check the official installer or restart Windows, then retry.' }
    Remove-Item -LiteralPath $file
    return $state
}

function Start-MrApplication([string]$Root, [string]$Java, [string[]]$ApplicationArguments) {
    $arguments = @('-jar',(Join-Path $Root 'MultiRoute-Downloader.jar')) + $ApplicationArguments
    $gui = Join-Path ([IO.Path]::GetDirectoryName($Java)) 'javaw.exe'
    if ($ApplicationArguments.Count -eq 0 -and [IO.File]::Exists($gui)) {
        Start-Process -FilePath $gui -ArgumentList (ConvertTo-MrNativeArguments $arguments) -WorkingDirectory $Root | Out-Null
        return 0
    }
    $process = Start-Process -FilePath $Java -ArgumentList (ConvertTo-MrNativeArguments $arguments) -WorkingDirectory $Root -NoNewWindow -Wait -PassThru
    return $process.ExitCode
}

function Invoke-MrBootstrap([string]$Root, [string]$Mode, [string[]]$ApplicationArguments = @()) {
    Assert-MrWindows
    Assert-MrPayload $Root
    $java = Find-MrJava $Root
    if (-not $java) { $java = Install-MrJava $Root }
    $npcap = Get-MrNpcapState
    if (-not $npcap.Present) { $npcap = Install-MrNpcap $Root }
    if (-not $npcap.Ready) { Stop-MrSetup 'Npcap is installed but its driver service is not running. Check Npcap or restart Windows; no service/security policy was changed by this launcher.' }
    if ($npcap.AdminOnly) { Write-Host 'Npcap access is restricted to Administrators. Authorize its UAC capture prompt when checking a gateway; Java itself runs as your normal user.' }
    Write-Host 'Environment checks completed. Starting MultiRoute Downloader as your normal user.'
    return (Start-MrApplication $Root $java $ApplicationArguments)
}

function Invoke-MrNativePrerequisites([string]$Root, [string]$StateRoot) {
    Assert-MrWindows
    Assert-MrPayload $Root
    $statePath = Assert-MrSafePath $StateRoot
    [void][IO.Directory]::CreateDirectory($statePath)
    $npcap = Get-MrNpcapState
    if (-not $npcap.Present) { $npcap = Install-MrNpcap $statePath }
    if (-not $npcap.Ready) { Stop-MrSetup 'Npcap is installed but its driver service is not running. Check Npcap or restart Windows, then reopen the application.' }
}

Export-ModuleMember -Function Invoke-MrBootstrap,Invoke-MrNativePrerequisites
