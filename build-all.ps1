# Master build script for all plugins
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Building All Plugins" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$scriptRoot = $PSScriptRoot
$builtFolder = Join-Path $scriptRoot "built"

if (-not (Test-Path $builtFolder)) {
    New-Item -ItemType Directory -Path $builtFolder | Out-Null
    Write-Host "Created built folder: $builtFolder" -ForegroundColor Green
}

Write-Host "Cleaning built folder..." -ForegroundColor Yellow
Get-ChildItem -Path $builtFolder -Filter "*.jar" -ErrorAction SilentlyContinue | Remove-Item -Force

# Check for Maven and download if needed
$mavenCmd = Get-Command mvn -ErrorAction SilentlyContinue
$mvnPath = $null

if (-not $mavenCmd) {
    Write-Host "Maven not found in PATH. Downloading Maven..." -ForegroundColor Yellow
    
    $mavenVersion = "3.9.6"
    $mavenUrl = "https://archive.apache.org/dist/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip"
    $mavenZip = Join-Path $scriptRoot "maven-temp.zip"
    $mavenDir = Join-Path $scriptRoot "maven-temp"
    
    try {
        if (-not (Test-Path (Join-Path $mavenDir "apache-maven-$mavenVersion\bin\mvn.cmd"))) {
            Write-Host "Downloading Maven from $mavenUrl..." -ForegroundColor Yellow
            Invoke-WebRequest -Uri $mavenUrl -OutFile $mavenZip -UseBasicParsing
            
            Write-Host "Extracting Maven..." -ForegroundColor Yellow
            Expand-Archive -Path $mavenZip -DestinationPath $mavenDir -Force
            Remove-Item $mavenZip -ErrorAction SilentlyContinue
        }
        
        $mvnPath = Join-Path (Resolve-Path $mavenDir) "apache-maven-$mavenVersion\bin\mvn.cmd"
        Write-Host "Using downloaded Maven: $mvnPath" -ForegroundColor Green
    } catch {
        Write-Host "Failed to download Maven. Please install Maven manually." -ForegroundColor Red
        exit 1
    }
} else {
    $mvnPath = "mvn"
    Write-Host "Using system Maven" -ForegroundColor Green
}

$plugins = @(
    "BaseSystem",
    "ChatManager",
    "CraftAccess",
    "EssentialsGUI",
    "EssentialsPerm",
    "FarmShop",
    "HomeBuffs",
    "Hook",
    "LevelEnchant",
    "LevelPlugin",
    "MoreCaves",
    "MoreEnd",
    "MoreMobs",
    "MoreNether",
    "MoreWeather",
    "NametagFormatter",
    "PerkShop",
    "PetPlugin",
    "SellGUI",
    "TreasureHunt",
    "Welcome"
)

$successCount = 0
$failCount = 0
$failedPlugins = @()

foreach ($plugin in $plugins) {
    $pluginPath = Join-Path $scriptRoot "plugins\$plugin"
    
    if (-not (Test-Path $pluginPath)) {
        Write-Host "[$plugin] Plugin directory not found, skipping..." -ForegroundColor Yellow
        continue
    }
    
    $pomPath = Join-Path $pluginPath "pom.xml"
    if (-not (Test-Path $pomPath)) {
        Write-Host "[$plugin] pom.xml not found, skipping..." -ForegroundColor Yellow
        continue
    }
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Building: $plugin" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    
    Push-Location $pluginPath
    
    try {
        & $mvnPath clean package
        
        if ($LASTEXITCODE -eq 0) {
            Start-Sleep -Milliseconds 1000
            $targetPath = Join-Path $pluginPath "target"
            
            if (Test-Path $targetPath) {
                $allJars = Get-ChildItem -Path $targetPath -Filter "*.jar" -ErrorAction SilentlyContinue
                # First try to find shaded JAR
                $jarFile = $allJars | Where-Object { $_.Name -like "*-shaded.jar" -or $_.Name -like "*shaded.jar" } | Select-Object -First 1
                
                # If no shaded JAR, try standard pattern
                if (-not $jarFile) {
                    $jarFile = $allJars | Where-Object { $_.Name -notlike "original-*" -and ($_.Name -like "$plugin-*" -or $_.Name -eq "$plugin.jar") } | Select-Object -First 1
                }
                
                # If still no JAR found but JARs exist, try to find the largest one (likely the main JAR)
                if (-not $jarFile -and $allJars.Count -gt 0) {
                    $jarFile = $allJars | Where-Object { $_.Name -notlike "original-*" } | Sort-Object Length -Descending | Select-Object -First 1
                }
                
                if ($jarFile) {
                    $destPath = Join-Path $builtFolder $jarFile.Name
                    Copy-Item $jarFile.FullName -Destination $destPath -Force
                    Write-Host "[$plugin] Build successful! Copied to built folder." -ForegroundColor Green
                    $successCount++
                } else {
                    Write-Host "[$plugin] JAR file not found in target directory!" -ForegroundColor Red
                    if (Test-Path $targetPath) {
                        $existing = Get-ChildItem -Path $targetPath -Filter "*.jar" -ErrorAction SilentlyContinue
                        if ($existing) {
                            Write-Host "[$plugin] Found JARs but none matched pattern: $($existing.Name -join ', ')" -ForegroundColor Yellow
                        }
                    }
                    $failCount++
                    $failedPlugins += $plugin
                }
            } else {
                Write-Host "[$plugin] Target directory not found!" -ForegroundColor Red
                $failCount++
                $failedPlugins += $plugin
            }
        } else {
            Write-Host "[$plugin] Build failed!" -ForegroundColor Red
            $failCount++
            $failedPlugins += $plugin
        }
    } catch {
        Write-Host "[$plugin] Build error: $_" -ForegroundColor Red
        $failCount++
        $failedPlugins += $plugin
    } finally {
        Pop-Location
    }
}

Set-Location $scriptRoot

# Clean up downloaded Maven if we downloaded it
if ($null -ne $mvnPath -and $mvnPath -ne "mvn" -and (Test-Path (Join-Path $scriptRoot "maven-temp"))) {
    Write-Host ""
    Write-Host "Cleaning up downloaded Maven..." -ForegroundColor Yellow
    Remove-Item -Recurse -Force (Join-Path $scriptRoot "maven-temp") -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Build Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Successful: $successCount" -ForegroundColor Green
Write-Host "Failed: $failCount" -ForegroundColor $(if ($failCount -eq 0) { "Green" } else { "Red" })

if ($failedPlugins.Count -gt 0) {
    Write-Host ""
    Write-Host "Failed plugins:" -ForegroundColor Red
    foreach ($plugin in $failedPlugins) {
        Write-Host "  - $plugin" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Built JARs are in: $builtFolder" -ForegroundColor Green
Write-Host ""
