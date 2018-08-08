@echo off
SETLOCAL
cd /d %~dp0\..

set JAR=datastax-agent*.jar

set JVM_OPTS=
call :jvmOptsAppend "-Djavax.net.ssl.trustStore=bin/ssl/agentKeyStore"
call :jvmOptsAppend "-Djavax.net.ssl.keyStore=bin/ssl/agentKeyStore"
call :jvmOptsAppend "-Djavax.net.ssl.keyStorePassword=opscenter"
call :jvmOptsAppend "-Dlog4j.configuration=file:conf/log4j.properties"
call :jvmOptsAppend "-DXmx60M"
call :jvmOptsAppend "-DXms60M"
goto run

:jvmOptsAppend
set JVM_OPTS=%JVM_OPTS% %1
goto :eof

:run
start java %JVM_OPTS% -jar %JAR% conf\address.yaml

ENDLOCAL
