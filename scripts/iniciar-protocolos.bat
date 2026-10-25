@echo off
cd /d "%~dp0..\backend"
call mvnw.cmd -pl ms-protocolos spring-boot:run
pause
