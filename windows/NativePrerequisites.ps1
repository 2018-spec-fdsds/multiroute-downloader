# Called by the native desktop entry point in a hidden child, never by a shortcut.
[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$StateRoot)
$ErrorActionPreference = 'Stop'
try {
    Import-Module (Join-Path $PSScriptRoot 'bootstrap-common.psm1') -Force
    $programFolder = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
    Invoke-MrNativePrerequisites -Root $programFolder -StateRoot $StateRoot
    exit 0
} catch {
    $message = $_.Exception.Message
    if ($message -notlike 'MultiRoute:*') { $message = 'MultiRoute: network setup failed. Reinstall the complete application and retry.' }
    [Console]::Error.WriteLine($message)
    exit 1
}
