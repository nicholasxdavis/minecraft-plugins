# PandoraBases Build Script for PowerShell
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  PandoraBases Plugin Builder" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if Maven is installed
$mvnPath = Get-Command mvn -ErrorAction SilentlyContinue

if (-not $mvnPath) {
    Write-Host "Maven is not installed or not in PATH." -ForegroundColor Red
    Write-Host ""
    Write-Host "Please install Maven:" -ForegroundColor Yellow
    Write-Host "1. Download from: https://maven.apache.org/download.cgi" -ForegroundColor White
    Write-Host "2. Extract to a folder (e.g., C:\Program Files\Apache\maven)" -ForegroundColor White
    Write-Host "3. Add the 'bin' folder to your PATH environment variable" -ForegroundColor White
    Write-Host ""
    Write-Host "OR use an IDE like IntelliJ IDEA or Eclipse to build." -ForegroundColor Yellow
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "Maven found! Building project..." -ForegroundColor Green
Write-Host ""

# Clean and build
& mvn clean package -DskipTests

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "  Build Successful!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Your plugin JAR is located at:" -ForegroundColor Cyan
    Write-Host "  target\PandoraBases-1.6.9.5-4.1.9.jar" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Copy this file to your server's plugins folder." -ForegroundColor White
    Write-Host ""
    
    # Check if JAR exists
    $jarPath = "target\PandoraBases-1.6.9.5-4.1.9.jar"
    if (Test-Path $jarPath) {
        $jarInfo = Get-Item $jarPath
        Write-Host "JAR Size: $([math]::Round($jarInfo.Length / 1MB, 2)) MB" -ForegroundColor Green
        Write-Host "Location: $($jarInfo.FullName)" -ForegroundColor Green
    }
} else {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "  Build Failed!" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please check the error messages above." -ForegroundColor Yellow
}

Write-Host ""
Read-Host "Press Enter to exit"







