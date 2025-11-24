# How to Fix IDE Dependency Resolution Issues

If your IDE (Cursor/VS Code) is not detecting Spring Framework and other dependencies, follow these steps:

## Step 1: Build the Project

First, ensure all dependencies are downloaded by building the project:

```bash
./gradlew clean build
```

If the wrapper doesn't work, use:
```bash
gradle wrapper
./gradlew clean build
```

## Step 2: Reload the IDE

1. Press `Cmd+Shift+P` (Mac) or `Ctrl+Shift+P` (Windows/Linux)
2. Type "Java: Clean Java Language Server Workspace"
3. Select it and confirm
4. Then type "Developer: Reload Window" and select it

## Step 3: Sync Gradle Project

1. Press `Cmd+Shift+P` (Mac) or `Ctrl+Shift+P` (Windows/Linux)
2. Type "Java: Build Workspace" or "Gradle: Refresh Gradle Project"
3. Wait for the build to complete

## Step 4: Verify Extensions

Make sure you have these extensions installed:
- **Extension Pack for Java** (vscjava.vscode-java-pack)
- **Gradle for Java** (vscjava.vscode-gradle)
- **Kotlin Language** (fwcd.kotlin)

## Step 5: Check Java Version

Ensure Java 21 is set:
```bash
java -version  # Should show version 21
```

If needed, set JAVA_HOME:
```bash
export JAVA_HOME=/Users/cristian.torres/Library/Java/JavaVirtualMachines/corretto-21.0.6/Contents/Home
```

## Step 6: Force Re-index

If still not working:
1. Close Cursor/VS Code
2. Delete `.vscode/.jdt` folder (if exists)
3. Delete `build` folder
4. Reopen the project
5. Run `./gradlew clean build`
6. Reload window

## Alternative: Use IntelliJ IDEA

For the best Kotlin/Spring Boot experience, consider using IntelliJ IDEA Community Edition, which has native support for Gradle and Kotlin projects.

