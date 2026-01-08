# Quick Build Guide - PandoraBases

## 🚀 Fastest Way to Build

### If you have IntelliJ IDEA or Eclipse:
1. **Open the project** in your IDE
2. **Right-click on `pom.xml`** → **Maven** → **Reload Project**
3. **Run Maven Goal**: `clean package` (skip tests)
4. **Find JAR**: `target/PandoraBases-1.6.9.5-4.1.9.jar`

### If you have Maven installed:
```bash
mvn clean package -DskipTests
```
JAR will be at: `target/PandoraBases-1.6.9.5-4.1.9.jar`

### If you DON'T have Maven:

**Option 1: Install Maven**
1. Download: https://maven.apache.org/download.cgi
2. Extract and add `bin` folder to your PATH
3. Run: `mvn clean package -DskipTests`

**Option 2: Use Online Build Service**
- Upload project to GitHub
- Use GitHub Actions or similar CI/CD
- Download the built JAR

**Option 3: Use IDE with Maven**
- IntelliJ IDEA (Community Edition is free)
- Eclipse IDE
- Both have built-in Maven support

## 📦 After Building

1. Copy `target/PandoraBases-1.6.9.5-4.1.9.jar` to your server's `plugins` folder
2. Restart your server
3. Configure the plugin in `plugins/PandoraBases/config.yml`

## ✅ Build Requirements

- **Java 8+** (You have Java 25 ✅)
- **Maven 3.6+** (Need to install)
- **Internet connection** (to download dependencies)

## 🔧 Troubleshooting

**"mvn not found"**: Install Maven or use an IDE

**Build errors**: Make sure all source files are present and Java 8+ is installed

**Missing dependencies**: Maven will download them automatically







