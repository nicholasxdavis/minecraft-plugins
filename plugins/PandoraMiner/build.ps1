# Build script for PandoraMiner
Write-Host "Building PandoraMiner..." -ForegroundColor Cyan

# Check if Maven is available
$mavenCmd = Get-Command mvn -ErrorAction SilentlyContinue

if (-not $mavenCmd) {
    Write-Host "Maven not found. Downloading Maven..." -ForegroundColor Yellow
    
    $mavenVersion = "3.9.6"
    $mavenUrl = "https://archive.apache.org/dist/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip"
    $mavenZip = "maven-temp\apache-maven-$mavenVersion-bin.zip"
    $mavenDir = "maven-temp\apache-maven-$mavenVersion"
    
    # Create temp directory
    New-Item -ItemType Directory -Force -Path "maven-temp" | Out-Null
    
    # Download Maven if not exists
    if (-not (Test-Path $mavenZip)) {
        Write-Host "Downloading Maven from $mavenUrl..." -ForegroundColor Yellow
        Invoke-WebRequest -Uri $mavenUrl -OutFile $mavenZip
    }
    
    # Extract Maven if not extracted
    if (-not (Test-Path $mavenDir)) {
        Write-Host "Extracting Maven..." -ForegroundColor Yellow
        Expand-Archive -Path $mavenZip -DestinationPath "maven-temp" -Force
    }
    
    $mavenBin = Resolve-Path "$mavenDir\bin\mvn.cmd"
    Write-Host "Building plugin with downloaded Maven..." -ForegroundColor Green
    & $mavenBin clean package
    
    # Clean up Maven
    Write-Host "Cleaning up Maven..." -ForegroundColor Yellow
    Remove-Item -Path "maven-temp" -Recurse -Force -ErrorAction SilentlyContinue
} else {
    Write-Host "Building plugin with system Maven..." -ForegroundColor Green
    mvn clean package
}

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build successful! JAR file is in the target directory." -ForegroundColor Green
    
    # Show JAR file location
    $jarFile = Get-ChildItem -Path "target" -Filter "PandoraMiner-*.jar" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    
    if ($jarFile) {
        Write-Host "JAR file: $($jarFile.FullName)" -ForegroundColor Cyan
    } else {
        Write-Host "Error: JAR file not found in target directory!" -ForegroundColor Red
    }
} else {
    Write-Host "Build failed! Check the errors above." -ForegroundColor Red
    exit 1
}


