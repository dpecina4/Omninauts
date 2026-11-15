@echo off
setlocal
set "GRADLE_VERSION=9.3.1"
set "GRADLE_USER_HOME=%GRADLE_USER_HOME%"
if "%GRADLE_USER_HOME%"=="" set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
where gradle >nul 2>nul
if not errorlevel 1 (
  for /f "tokens=2" %%V in ('gradle --version ^| findstr /b Gradle') do set "INSTALLED_VERSION=%%V"
  if "%INSTALLED_VERSION%"=="%GRADLE_VERSION%" (
    gradle %*
    exit /b %ERRORLEVEL%
  )
)
set "GRADLE_ROOT=%GRADLE_USER_HOME%\cavernauts-bootstrap\gradle-%GRADLE_VERSION%"
set "GRADLE_BIN=%GRADLE_ROOT%\bin\gradle.bat"
if not exist "%GRADLE_BIN%" (
  set "ZIP_PATH=%GRADLE_USER_HOME%\cavernauts-bootstrap\gradle-%GRADLE_VERSION%-bin.zip"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $d='%GRADLE_USER_HOME%\cavernauts-bootstrap'; New-Item -ItemType Directory -Force -Path $d | Out-Null; $z=Join-Path $d 'gradle-%GRADLE_VERSION%-bin.zip'; Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile $z; $h=(Get-FileHash $z -Algorithm SHA256).Hash.ToLower(); if ($h -ne 'b266d5ff6b90eada6dc3b20cb090e3731302e553a27c5d3e4df1f0d76beaff06') { throw 'Gradle distribution checksum verification failed.' }; Expand-Archive -Force $z $d"
  if errorlevel 1 exit /b 1
)
call "%GRADLE_BIN%" %*
exit /b %ERRORLEVEL%
