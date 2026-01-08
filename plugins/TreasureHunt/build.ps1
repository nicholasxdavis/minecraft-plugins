# Build script for TreasureHunt plugin
Write-Host "Building TreasureHunt plugin..." -ForegroundColor Green

# Check if Maven is available
$mavenCmd = Get-Command mvn -ErrorAction SilentlyContinue
if (-not $mavenCmd) {
    Write-Host "Maven not found! Please install Maven or use the Maven wrapper." -ForegroundColor Red
    exit 1
}

# Build the project
Set-Location $PSScriptRoot
mvn clean package

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build successful!" -ForegroundColor Green
    
    # Copy to built folder
    $builtDir = Join-Path $PSScriptRoot "..\..\built"
    if (-not (Test-Path $builtDir)) {
        New-Item -ItemType Directory -Path $builtDir | Out-Null
    }
    
    $jarFile = Get-ChildItem -Path "target" -Filter "TreasureHunt-*.jar" | Select-Object -First 1
    if ($jarFile) {
        Copy-Item $jarFile.FullName -Destination $builtDir -Force
        Write-Host "JAR copied to built folder: $($jarFile.Name)" -ForegroundColor Green
    }
} else {
    Write-Host "Build failed!" -ForegroundColor Red
    exit 1
}





