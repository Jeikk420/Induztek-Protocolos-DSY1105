@echo off
cd /d "%~dp0..\backend"
call mvnw.cmd -pl ms-equipos spring-boot:run
pause
