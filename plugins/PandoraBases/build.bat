@echo off
echo Building PandoraBases Plugin...
echo.

REM Check if Maven is installed
where mvn >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Maven is not installed or not in PATH.
    echo.
    echo Please install Maven from: https://maven.apache.org/download.cgi
    echo Or use an IDE like IntelliJ IDEA or Eclipse to build the project.
    echo.
    pause
    exit /b 1
)

echo Maven found! Building project...
echo.

REM Clean and build
call mvn clean package -DskipTests

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Build Successful!
    echo ========================================
    echo.
    echo Your plugin JAR is located at:
    echo target\PandoraBases.jar
    echo.
    echo Copy this file to your server's plugins folder.
    echo.
) else (
    echo.
    echo ========================================
    echo Build Failed!
    echo ========================================
    echo.
    echo Please check the error messages above.
    echo.
)

pause







