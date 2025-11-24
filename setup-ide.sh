#!/bin/bash

set -e

echo "🚀 Setting up IDE for full dependency resolution and documentation..."
echo ""

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo "1. ✅ Ensuring Gradle wrapper is valid..."
if [ ! -f "gradle/wrapper/gradle-wrapper.jar" ] || [ ! -s "gradle/wrapper/gradle-wrapper.jar" ]; then
    echo "   Downloading Gradle wrapper JAR..."
    curl -L -o gradle/wrapper/gradle-wrapper.jar \
        https://raw.githubusercontent.com/gradle/gradle/v8.11.1/gradle/wrapper/gradle-wrapper.jar
fi

echo "2. ✅ Making gradlew executable..."
chmod +x gradlew

echo "3. ✅ Verifying Java 21..."
JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
if [ "$JAVA_VERSION" != "21" ]; then
    echo "   ⚠️  Warning: Java version is $JAVA_VERSION, expected 21"
    echo "   Please ensure Java 21 is installed and set as default"
else
    echo "   ✓ Java 21 detected"
fi

echo ""
echo "4. 🔨 Building project to download dependencies..."
echo "   This may take a few minutes on first run..."
./gradlew clean build --refresh-dependencies -x test || {
    echo "   ⚠️  Build had some issues, but continuing..."
}

echo ""
echo "5. ✅ Generating IDE metadata..."
./gradlew idea 2>/dev/null || echo "   (idea task not available, skipping)"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ Setup complete!"
echo ""
echo "📋 Next steps in Cursor/VS Code:"
echo ""
echo "   1. Press Cmd+Shift+P (Mac) or Ctrl+Shift+P (Windows/Linux)"
echo "   2. Type: 'Java: Clean Java Language Server Workspace'"
echo "   3. Press Enter and confirm"
echo "   4. Wait for cleanup to complete"
echo "   5. Then type: 'Developer: Reload Window'"
echo "   6. Press Enter"
echo ""
echo "   7. After reload, type: 'Java: Build Workspace'"
echo "   8. Wait for indexing (1-2 minutes)"
echo ""
echo "📚 You should now have:"
echo "   ✓ Full autocomplete for Spring Framework"
echo "   ✓ Documentation on hover"
echo "   ✓ F12 navigation to source code"
echo "   ✓ Import suggestions"
echo ""
echo "💡 If issues persist, see IDE_SETUP.md for detailed troubleshooting"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

