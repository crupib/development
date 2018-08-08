@REM Setup DataStax Agent
@echo off

SETLOCAL
cd /d %~dp0\..

if "%~1"=="" (
    echo Usage: [opscenterd address]
    goto :EOF
    )

set OPSC_ADDR=%1
set CONF=conf\address.yaml

if not exist "conf" (
    mkdir conf
    )

echo stomp_interface: %OPSC_ADDR% > %CONF%

ENDLOCAL
