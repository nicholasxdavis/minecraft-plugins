$scriptRoot = $PSScriptRoot
$builtFolder = Join-Path $scriptRoot "built"

if (-not (Test-Path $builtFolder)) {
    New-Item -ItemType Directory -Path $builtFolder | Out-Null
}

Get-ChildItem -Path $builtFolder -Filter "*.jar" -ErrorAction SilentlyContinue | Remove-Item -Force

$plugins = @("BaseSystem", "ChatManager", "CraftAccess", "EssentialsGUI", "EssentialsPerm", "FarmShop", "HomeBuffs", "Hook", "LevelEnchant", "LevelPlugin", "MoreCaves", "MoreEnd", "MoreMobs", "MoreNether", "MoreWeather", "NametagFormatter", "PerkShop", "PetPlugin", "SellGUI", "TreasureHunt")

$successCount = 0
$failCount = 0

foreach ($plugin in $plugins) {
    $pluginPath = Join-Path $scriptRoot "plugins\$plugin"
    if (-not (Test-Path (Join-Path $pluginPath "pom.xml"))) { continue }
    
    Write-Host "Building $plugin..." -ForegroundColor Cyan
    Push-Location $pluginPath
    
    try {
        .\build.ps1 *>&1 | Out-Null
        
        Start-Sleep -Milliseconds 500
        
        $targetPath = Join-Path $pluginPath "target"
        if (Test-Path $targetPath) {
            $jarFile = Get-ChildItem -Path $targetPath -Filter "*.jar" -Exclude "original-*.jar" -ErrorAction SilentlyContinue | Where-Object { $_.Name -like "$plugin-*" -or $_.Name -eq "$plugin.jar" } | Select-Object -First 1
            
            if ($jarFile) {
                Copy-Item $jarFile.FullName -Destination $builtFolder -Force
                Write-Host "$plugin - SUCCESS" -ForegroundColor Green
                $successCount++
            } else {
                Write-Host "$plugin - FAILED (No JAR found in target)" -ForegroundColor Red
                $failCount++
            }
        } else {
            Write-Host "$plugin - FAILED (target directory not found)" -ForegroundColor Red
            $failCount++
        }
    } catch {
        Write-Host "$plugin - FAILED: $_" -ForegroundColor Red
        $failCount++
    } finally {
        Pop-Location
    }
}

Write-Host ""
Write-Host "Built: $successCount | Failed: $failCount" -ForegroundColor $(if ($failCount -eq 0) { "Green" } else { "Yellow" })
Write-Host "JARs in: $builtFolder" -ForegroundColor Green





