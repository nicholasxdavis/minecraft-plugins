# Build script for NoNerfs
Write-Host "Building NoNerfs..."

# Check if Maven is available
$mavenCmd = Get-Command mvn -ErrorAction SilentlyContinue

if (-not $mavenCmd) {
    Write-Host "Maven not found. Downloading Maven..."
    
    $mavenVersion = "3.9.6"
    $mavenUrl = "https://archive.apache.org/dist/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip"
    $mavenZip = "..\maven-temp\apache-maven-$mavenVersion-bin.zip"
    $mavenDir = "..\maven-temp\apache-maven-$mavenVersion"
    
    # Create temp directory
    New-Item -ItemType Directory -Force -Path "..\maven-temp" | Out-Null
    
    # Download Maven if not exists
    if (-not (Test-Path $mavenZip)) {
        Write-Host "Downloading Maven from $mavenUrl..."
        Invoke-WebRequest -Uri $mavenUrl -OutFile $mavenZip
    }
    
    # Extract Maven if not extracted
    if (-not (Test-Path $mavenDir)) {
        Write-Host "Extracting Maven..."
        Expand-Archive -Path $mavenZip -DestinationPath "..\maven-temp" -Force
    }
    
    $mavenBin = Resolve-Path "$mavenDir\bin\mvn.cmd"
    Write-Host "Building plugin with downloaded Maven..."
    & $mavenBin clean package
    
    # Clean up Maven
    Write-Host "Cleaning up Maven..."
    Remove-Item -Path "..\maven-temp" -Recurse -Force -ErrorAction SilentlyContinue
} else {
    Write-Host "Building plugin with system Maven..."
    mvn clean package
}

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build successful! JAR file created in target directory."
} else {
    Write-Host "Build failed! Check the errors above."
    exit 1
}

