# Build script for PandoraWorldLock
Write-Host "Building PandoraWorldLock..." -ForegroundColor Cyan

# Check if Maven is available
$mavenCmd = Get-Command mvn -ErrorAction SilentlyContinue

if (-not $mavenCmd) {
    Write-Host "Maven not found. Downloading Maven temporarily..." -ForegroundColor Yellow
    
    $mavenVersion = "3.9.6"
    $mavenUrl = "https://archive.apache.org/dist/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip"
    $mavenZip = "maven-temp\apache-maven-$mavenVersion-bin.zip"
    $mavenDir = "maven-temp\apache-maven-$mavenVersion"
    
    # Create temp directory
    New-Item -ItemType Directory -Force -Path "maven-temp" | Out-Null
    
    # Download Maven if not exists
    if (-not (Test-Path $mavenZip)) {
        Write-Host "Downloading Maven from $mavenUrl..." -ForegroundColor Yellow
        try {
            Invoke-WebRequest -Uri $mavenUrl -OutFile $mavenZip -UseBasicParsing
        } catch {
            Write-Host "Failed to download Maven. Please install Maven manually." -ForegroundColor Red
            Write-Host "Download from: https://maven.apache.org/download.cgi" -ForegroundColor Yellow
            exit 1
        }
    }
    
    # Extract Maven if not extracted
    if (-not (Test-Path $mavenDir)) {
        Write-Host "Extracting Maven..." -ForegroundColor Yellow
        Expand-Archive -Path $mavenZip -DestinationPath "maven-temp" -Force
    }
    
    $mavenBin = Resolve-Path "$mavenDir\bin\mvn.cmd"
    Write-Host "Building plugin with downloaded Maven..." -ForegroundColor Green
    & $mavenBin clean package -DskipTests
    
    $buildSuccess = $LASTEXITCODE -eq 0
    
    # Clean up Maven
    Write-Host "Cleaning up temporary Maven..." -ForegroundColor Yellow
    Remove-Item -Path "maven-temp" -Recurse -Force -ErrorAction SilentlyContinue
    
    if (-not $buildSuccess) {
        Write-Host ""
        Write-Host "Build failed! Check the errors above." -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "Building plugin with system Maven..." -ForegroundColor Green
    mvn clean package -DskipTests
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "Build failed! Check the errors above." -ForegroundColor Red
        exit 1
    }
}

# Show build result
Write-Host ""
Write-Host "Build successful! JAR file location:" -ForegroundColor Green
$jarPath = Get-ChildItem -Path "target" -Filter "*.jar" -Exclude "*-sources.jar","*-javadoc.jar" | Select-Object -First 1
if ($jarPath) {
    Write-Host "  $($jarPath.FullName)" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Copy this JAR to your server's plugins folder." -ForegroundColor Yellow
} else {
    Write-Host "  JAR file not found in target directory" -ForegroundColor Red
}

