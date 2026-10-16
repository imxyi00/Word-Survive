@echo off
cd /d "%~dp0"
where javac >nul 2>nul
if errorlevel 1 (
  echo A JDK 17 or newer is required to rebuild. Download it from https://adoptium.net
  pause
  exit /b 1
)
if exist build rmdir /s /q build
mkdir build
dir /s /b src\*.java > build\sources.txt
javac --release 17 -encoding UTF-8 -d build @build\sources.txt
if errorlevel 1 (
  pause
  exit /b 1
)
jar cfe WordSurvive.jar wordsurvive.Main -C build .
rmdir /s /q build
echo Built WordSurvive.jar
pause
