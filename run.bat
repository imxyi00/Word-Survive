@echo off
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
  echo Java 17 or newer is required. Download it from https://adoptium.net
  pause
  exit /b 1
)
java -jar WordSurvive.jar %*
if errorlevel 1 pause
