@echo off
setlocal
cd /d "%~dp0"

set "JAVAC_CMD="
where javac >nul 2>nul && set "JAVAC_CMD=javac"
if not defined JAVAC_CMD if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" set "JAVAC_CMD=%JAVA_HOME%\bin\javac.exe"
if not defined JAVAC_CMD (
  for /d %%D in ("%ProgramFiles%\Java\jdk*" "%ProgramFiles%\Eclipse Adoptium\jdk*" "%ProgramFiles%\Microsoft\jdk*" "%ProgramFiles%\Amazon Corretto\jdk*" "%ProgramFiles%\Zulu\zulu-*") do (
    if exist "%%D\bin\javac.exe" set "JAVAC_CMD=%%D\bin\javac.exe"
  )
)

set "JAR_CMD="
where jar >nul 2>nul && set "JAR_CMD=jar"
if not defined JAR_CMD if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jar.exe" set "JAR_CMD=%JAVA_HOME%\bin\jar.exe"
if not defined JAR_CMD (
  for /d %%D in ("%ProgramFiles%\Java\jdk*" "%ProgramFiles%\Eclipse Adoptium\jdk*" "%ProgramFiles%\Microsoft\jdk*" "%ProgramFiles%\Amazon Corretto\jdk*" "%ProgramFiles%\Zulu\zulu-*") do (
    if exist "%%D\bin\jar.exe" set "JAR_CMD=%%D\bin\jar.exe"
  )
)

if not defined JAVAC_CMD (
  echo A JDK 17 or newer is required to rebuild: javac not found. Download it from https://adoptium.net
  pause
  exit /b 1
)

if not defined JAR_CMD (
  echo A JDK 17 or newer is required to rebuild: jar not found. Download it from https://adoptium.net
  pause
  exit /b 1
)

if exist build rmdir /s /q build
mkdir build
dir /s /b src\*.java > build\sources.txt
"%JAVAC_CMD%" --release 17 -encoding UTF-8 -d build @build\sources.txt
if errorlevel 1 (
  echo Compilation failed.
  pause
  exit /b 1
)

"%JAR_CMD%" cfe WordSurvive.jar wordsurvive.Main -C build .
if errorlevel 1 (
  echo Packaging jar failed.
  pause
  exit /b 1
)

rmdir /s /q build
echo Built WordSurvive.jar
pause
