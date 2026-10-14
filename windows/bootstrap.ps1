# Requires Windows PowerShell 5.1. Policy applies to this launcher process only.
[CmdletBinding(PositionalBinding = $false)]
param(
    [ValidateSet('Start','Deploy')][string]$Mode = 'Start',
    [Parameter(ValueFromRemainingArguments = $true)][string[]]$ApplicationArguments = @()
)
$ErrorActionPreference = 'Stop'
try {
    Import-Module (Join-Path $PSScriptRoot 'bootstrap-common.psm1') -Force
    $programFolder = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
    $result = Invoke-MrBootstrap -Root $programFolder -Mode $Mode -ApplicationArguments $ApplicationArguments
    exit $result
} catch {
    # Raw process/network exceptions may contain private URLs or environment values.
    $message = $_.Exception.Message
    if ($message -notlike 'MultiRoute:*') { $message = 'MultiRoute: setup failed. Re-extract the complete distribution and retry; check docs/en.md.' }
    [Console]::Error.WriteLine($message)
    exit 1
}
