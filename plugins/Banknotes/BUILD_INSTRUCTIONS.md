# Building Pandora Banknotes

## Prerequisites
- Java JDK (version 7 or higher)
- Maven (or use the build script which downloads it automatically)

## Manual Build Steps

### 1. Download Vault API
Since Vault API is required for compilation, you need to download it first:

1. Download Vault.jar from: https://www.spigotmc.org/resources/vault.34315/
2. Install it to your local Maven repository:

```powershell
mvn install:install-file -Dfile=path\to\Vault.jar -DgroupId=net.milkbowl.vault -DartifactId=VaultAPI -Dversion=1.5 -Dpackaging=jar
```

### 2. Build the Project
Once Vault is installed, build the project:

```powershell
mvn clean package
```

The JAR file will be created in the `target` directory.

## Using the Build Script

Alternatively, you can use the provided build script:

1. First, manually download Vault.jar from SpigotMC
2. Place it in the project directory
3. Run: `powershell -ExecutionPolicy Bypass -File build.ps1`

## Note
Vault is a "provided" dependency, meaning it must be available on your Minecraft server at runtime. The plugin JAR does not include Vault - it expects Vault to be installed as a separate plugin on your server.

