# Read-only MSI database inspection: no installation or product registration.
[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$Path,
      [Parameter(Mandatory=$true)][string]$Report)
$ErrorActionPreference = 'Stop'
$installer = New-Object -ComObject WindowsInstaller.Installer
$database = $installer.GetType().InvokeMember('OpenDatabase','InvokeMethod',$null,$installer,@([IO.Path]::GetFullPath($Path),0))

function Read-MrTable([string]$Query, [string[]]$Columns) {
    $view = $database.GetType().InvokeMember('OpenView','InvokeMethod',$null,$database,@($Query))
    try {
        [void]$view.GetType().InvokeMember('Execute','InvokeMethod',$null,$view,$null)
        $records = @()
        while ($record = $view.GetType().InvokeMember('Fetch','InvokeMethod',$null,$view,$null)) {
            try {
                $values = [ordered]@{}
                for ($i = 0; $i -lt $Columns.Length; $i++) {
                    $values[$Columns[$i]] = $record.GetType().InvokeMember('StringData','GetProperty',$null,$record,@($i+1))
                }
                $records += [pscustomobject]$values
            } finally { [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($record) }
        }
        return $records
    } finally {
        [void]$view.GetType().InvokeMember('Close','InvokeMethod',$null,$view,$null)
        [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($view)
    }
}

try {
    $properties = @(Read-MrTable 'SELECT `Property`, `Value` FROM `Property`' @('Property','Value'))
    $directories = @(Read-MrTable 'SELECT `Directory`, `Directory_Parent`, `DefaultDir` FROM `Directory`' @('Directory','Parent','DefaultDir'))
    $shortcuts = @(Read-MrTable 'SELECT `Shortcut`, `Directory_`, `Name`, `Target`, `Arguments` FROM `Shortcut`' @('Shortcut','Directory','Name','Target','Arguments'))
    $files = @(Read-MrTable 'SELECT `File`, `FileName`, `FileSize` FROM `File`' @('File','FileName','FileSize'))
    $media = @(Read-MrTable 'SELECT `Cabinet` FROM `Media`' @('Cabinet'))
    $binary = @(Read-MrTable 'SELECT `Name` FROM `Binary`' @('Name'))
    $dialogs = @(Read-MrTable 'SELECT `Dialog` FROM `Dialog`' @('Dialog'))
    $licenseControls = @(Read-MrTable "SELECT ``Control``, ``Type``, ``Text`` FROM ``Control`` WHERE ``Dialog_``='LicenseAgreementDlg'" @('Control','Type','Text'))
    $cabRoot = [IO.Path]::GetDirectoryName([IO.Path]::GetFullPath($Report))
    Add-Type -TypeDefinition @'
using System;
using System.IO;
using System.Runtime.InteropServices;
public static class MultiRouteMsiReadOnlyStream {
    [DllImport("msi.dll", CharSet=CharSet.Unicode)] static extern uint MsiOpenDatabaseW(string path, IntPtr mode, out uint handle);
    [DllImport("msi.dll", CharSet=CharSet.Unicode)] static extern uint MsiDatabaseOpenViewW(uint database, string query, out uint view);
    [DllImport("msi.dll")] static extern uint MsiViewExecute(uint view, uint record);
    [DllImport("msi.dll")] static extern uint MsiViewFetch(uint view, out uint record);
    [DllImport("msi.dll")] static extern uint MsiRecordReadStream(uint record, uint field, byte[] data, ref uint count);
    [DllImport("msi.dll")] static extern uint MsiCloseHandle(uint handle);
    static void Check(uint code) { if (code != 0) throw new IOException("Read-only MSI stream error " + code); }
    public static void Save(string path, string stream, string destination) {
        uint database=0, view=0, record=0;
        if (stream.IndexOf('\'') >= 0) throw new IOException("Unexpected MSI stream name");
        try {
            Check(MsiOpenDatabaseW(path, IntPtr.Zero, out database));
            Check(MsiDatabaseOpenViewW(database, "SELECT `Data` FROM `_Streams` WHERE `Name`='"+stream+"'", out view));
            Check(MsiViewExecute(view, 0)); Check(MsiViewFetch(view, out record));
            using (FileStream output = new FileStream(destination, FileMode.Create, FileAccess.Write)) {
                byte[] bytes = new byte[65536]; long total=0;
                for (;;) {
                    uint count = (uint)bytes.Length; Check(MsiRecordReadStream(record, 1, bytes, ref count));
                    if (count == 0) break; total += count;
                    if (total > 1073741824L) throw new IOException("Owned MSI cabinet exceeds size limit");
                    output.Write(bytes, 0, (int)count);
                }
            }
        } finally {
            if (record != 0) MsiCloseHandle(record);
            if (view != 0) MsiCloseHandle(view);
            if (database != 0) MsiCloseHandle(database);
        }
    }
}
'@
    $cabinets = @()
    foreach ($item in $media) {
        if (-not $item.Cabinet.StartsWith('#')) { throw 'Expected an embedded installer cabinet.' }
        $stream = $item.Cabinet.Substring(1)
        if ($stream -notmatch '^[A-Za-z0-9_.-]+$') { throw 'Unexpected installer cabinet name.' }
        $target = Join-Path $cabRoot $stream
        [MultiRouteMsiReadOnlyStream]::Save([IO.Path]::GetFullPath($Path),$stream,$target)
        $cabinets += $stream
    }
    [ordered]@{properties=$properties;directories=$directories;shortcuts=$shortcuts;files=$files;cabinets=$cabinets;binary=$binary;dialogs=$dialogs;license_controls=$licenseControls;read_only=$true} |
        ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $Report -Encoding UTF8
} finally {
    [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($database)
    [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($installer)
}
