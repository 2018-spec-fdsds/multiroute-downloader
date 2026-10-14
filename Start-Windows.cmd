@echo off
setlocal
cd /d "%~dp0"
set "MR_POWERSHELL=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"
if exist "%SystemRoot%\Sysnative\WindowsPowerShell\v1.0\powershell.exe" set "MR_POWERSHELL=%SystemRoot%\Sysnative\WindowsPowerShell\v1.0\powershell.exe"
"%MR_POWERSHELL%" -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0windows\bootstrap.ps1" -Mode Start %*
set "MR_EXIT=%ERRORLEVEL%"
if not "%MR_EXIT%"=="0" if "%~1"=="" pause
exit /b %MR_EXIT%
