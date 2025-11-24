# Sync IDE to Detect Dependencies - Step by Step

Your project builds and runs successfully, but the IDE isn't detecting Spring Framework dependencies. Follow these steps **in order**:

## Step 1: Clean Java Language Server Workspace

1. Press `Cmd+Shift+P` (Mac) or `Ctrl+Shift+P` (Windows/Linux)
2. Type: `Java: Clean Java Language Server Workspace`
3. Press Enter
4. **Click "Restart and delete"** when prompted
5. **Wait for it to complete** (check status bar at bottom)

## Step 2: Reload Window

1. Press `Cmd+Shift+P` again
2. Type: `Developer: Reload Window`
3. Press Enter
4. **Wait for the window to reload**

## Step 3: Build Workspace

1. Press `Cmd+Shift+P` again
2. Type: `Java: Build Workspace`
3. Press Enter
4. **Wait 1-2 minutes** for indexing (watch status bar)

## Step 4: Verify Gradle Sync

1. Open the Output panel: `View → Output` (or `Cmd+Shift+U`)
2. Select "Java" from the dropdown
3. Look for messages like:
   - "Building workspace..."
   - "Gradle sync completed"
   - Any error messages

## Step 5: Check Extensions

Make sure you have these installed:
- **Extension Pack for Java** (includes everything)
- **Gradle for Java**
- **Kotlin Language**

To check: `Cmd+Shift+X` → Search for "Extension Pack for Java"

## Step 6: Force Gradle Refresh (If Still Not Working)

1. Press `Cmd+Shift+P`
2. Type: `Gradle: Refresh Gradle Project`
3. Press Enter
4. Wait for sync to complete

## Alternative: Use Command Line

If the IDE commands don't work, you can also:

```bash
# In terminal, from project root:
./gradlew clean build

# Then in IDE:
# 1. Clean Java Language Server Workspace
# 2. Reload Window
# 3. Build Workspace
```

## Expected Result

After these steps, you should see:
- ✅ No red underlines on Spring imports
- ✅ Hover shows documentation
- ✅ F12 navigates to definitions
- ✅ Autocomplete works

## If It Still Doesn't Work

1. **Check Java Extension Output:**
   - View → Output
   - Select "Java"
   - Look for errors

2. **Check Gradle Extension Output:**
   - View → Output
   - Select "Gradle"
   - Look for sync errors

3. **Try deleting IDE cache:**
   ```bash
   rm -rf .vscode/.jdt
   rm -rf .idea
   ```
   Then repeat Steps 1-3

4. **Verify Java Home:**
   - Open Settings: `Cmd+,`
   - Search: `java.home`
   - Should be: `/Users/cristian.torres/Library/Java/JavaVirtualMachines/corretto-21.0.6/Contents/Home`

## Quick Checklist

- [ ] Cleaned Java Language Server Workspace
- [ ] Reloaded Window
- [ ] Built Workspace
- [ ] Checked Output panel for errors
- [ ] Verified extensions are installed
- [ ] Java 21 is set correctly

After completing all steps, the IDE should recognize all Spring Framework dependencies!



