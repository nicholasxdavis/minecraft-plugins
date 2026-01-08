# Build script for Pandora Banknotes
# This script will download Vault API and build the project

$ErrorActionPreference = "Stop"

Write-Host "=== Pandora Banknotes Build Script ===" -ForegroundColor Cyan

# Check for Maven
$mavenHome = "$env:TEMP\maven-build\apache-maven-3.9.6"
if (-not (Test-Path "$mavenHome\bin\mvn.cmd")) {
    Write-Host "Maven not found. Downloading..." -ForegroundColor Yellow
    $mavenVersion = "3.9.6"
    $mavenUrl = "https://archive.apache.org/dist/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip"
    $tempDir = "$env:TEMP\maven-build"
    $zipFile = "$tempDir\maven.zip"
    
    New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
    Invoke-WebRequest -Uri $mavenUrl -OutFile $zipFile
    Expand-Archive -Path $zipFile -DestinationPath $tempDir -Force
}

# Download and install Vault API
$vaultDir = "$env:USERPROFILE\.m2\repository\net\milkbowl\vault\VaultAPI\1.5"
$vaultFile = "$vaultDir\VaultAPI-1.5.jar"

if (-not (Test-Path $vaultFile)) {
    Write-Host "Downloading Vault API..." -ForegroundColor Yellow
    New-Item -ItemType Directory -Path $vaultDir -Force | Out-Null
    
    # Try multiple sources
    $sources = @(
        "https://github.com/MilkBowl/Vault/releases/download/1.7.3/Vault.jar",
        "https://www.spigotmc.org/resources/vault.34315/download?version=279169"
    )
    
    $downloaded = $false
    foreach ($url in $sources) {
        try {
            Write-Host "Trying: $url" -ForegroundColor Gray
            Invoke-WebRequest -Uri $url -OutFile $vaultFile -UseBasicParsing -ErrorAction Stop
            if (Test-Path $vaultFile -and (Get-Item $vaultFile).Length -gt 0) {
                Write-Host "Vault downloaded successfully!" -ForegroundColor Green
                $downloaded = $true
                break
            }
        } catch {
            Write-Host "Failed: $_" -ForegroundColor Red
        }
    }
    
    if (-not $downloaded) {
        Write-Host "`nERROR: Could not download Vault API automatically." -ForegroundColor Red
        Write-Host "Please manually download Vault.jar from:" -ForegroundColor Yellow
        Write-Host "  https://www.spigotmc.org/resources/vault.34315/" -ForegroundColor Yellow
        Write-Host "Then run:" -ForegroundColor Yellow
        Write-Host "  & '$mavenHome\bin\mvn.cmd' install:install-file -Dfile=path\to\Vault.jar -DgroupId=net.milkbowl.vault -DartifactId=VaultAPI -Dversion=1.5 -Dpackaging=jar" -ForegroundColor Yellow
        exit 1
    }
    
    # Install to Maven repository
    Write-Host "Installing Vault to Maven repository..." -ForegroundColor Yellow
    & "$mavenHome\bin\mvn.cmd" install:install-file -Dfile=$vaultFile -DgroupId=net.milkbowl.vault -DartifactId=VaultAPI -Dversion=1.5 -Dpackaging=jar
}

# Build the project
Write-Host "`nBuilding Pandora Banknotes..." -ForegroundColor Cyan
& "$mavenHome\bin\mvn.cmd" clean package

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n=== BUILD SUCCESSFUL ===" -ForegroundColor Green
    $jarFile = Get-ChildItem -Path "target" -Filter "*.jar" -Exclude "*sources.jar" | Select-Object -First 1
    if ($jarFile) {
        Write-Host "JAR file created: $($jarFile.FullName)" -ForegroundColor Green
        Write-Host "Size: $([math]::Round($jarFile.Length / 1KB, 2)) KB" -ForegroundColor Gray
    }
} else {
    Write-Host "`n=== BUILD FAILED ===" -ForegroundColor Red
    exit 1
}

