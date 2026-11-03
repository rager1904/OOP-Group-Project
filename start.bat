@echo off
cd /d "%~dp0"
set "TRACKER_JAVA=java"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "TRACKER_JAVA=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_HOME for /d %%J in ("%ProgramFiles%\Java\jdk*") do if exist "%%J\bin\java.exe" set "TRACKER_JAVA=%%J\bin\java.exe"
"%TRACKER_JAVA%" -jar target\market-stock-tracker-1.0.0.jar
pause
