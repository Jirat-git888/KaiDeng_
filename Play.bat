@echo off
cd /d "%~dp0"
set "KAIDENG_JAVA=java"
if exist "%JAVA_HOME%\bin\java.exe" set "KAIDENG_JAVA=%JAVA_HOME%\bin\java.exe"
if exist "%ProgramFiles%\Java\jdk-21\bin\java.exe" set "KAIDENG_JAVA=%ProgramFiles%\Java\jdk-21\bin\java.exe"
"%KAIDENG_JAVA%" -jar KaiDeng.jar
if errorlevel 1 pause
