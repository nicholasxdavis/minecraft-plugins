# Building PandoraBases Plugin

## Quick Build Instructions

### Option 1: Using Maven (Command Line)

1. **Install Maven** (if not already installed):
   - Download from: https://maven.apache.org/download.cgi
   - Extract and add to your PATH

2. **Build the plugin**:
   ```bash
   mvn clean package -DskipTests
   ```

3. **Find your JAR**:
   - The built JAR will be at: `target/PandoraBases.jar`
   - Copy this file to your server's `plugins` folder

### Option 2: Using Build Script (Windows)

1. **Double-click** `build.bat` in the project root
2. The script will automatically build the plugin
3. Find the JAR at: `target\PandoraBases.jar`

### Option 3: Using IntelliJ IDEA

1. Open the project in IntelliJ IDEA
2. Open the Maven tool window (View → Tool Windows → Maven)
3. Expand `PandoraBases` → `Lifecycle`
4. Double-click `clean` then `package`
5. Find the JAR at: `target/PandoraBases.jar`

### Option 4: Using Eclipse

1. Right-click on the project
2. Select `Run As` → `Maven build...`
3. Enter goals: `clean package`
4. Click `Run`
5. Find the JAR at: `target/PandoraBases.jar`

## After Building

1. Copy `target/PandoraBases.jar` to your server's `plugins` folder
2. Restart your server
3. The plugin will generate configuration files on first run

## Requirements

- Java 8 or higher
- Maven 3.6+ (for command line builds)
- All dependencies will be downloaded automatically by Maven

## Troubleshooting

**Build fails with "mvn not found"**:
- Install Maven and add it to your PATH
- Or use an IDE with built-in Maven support

**Build fails with compilation errors**:
- Make sure you have Java 8+ installed
- Check that all source files are present
- Try cleaning: `mvn clean` then `mvn package`

**JAR file not found**:
- Check the `target` directory
- Make sure the build completed successfully
- Look for error messages in the build output







