# IMMEDIATE FIX FOR BUILD ERRORS

## Critical Steps (Do These Now):

### Step 1: Fix Project Structure in IntelliJ
1. Press `Ctrl+Alt+Shift+S` (or File → Project Structure)
2. In the left panel, click **Modules**
3. Select **MediConnect** module (if you see it)
4. If NO modules appear, click the **"+"** button → **Import Module** → Navigate to `pom.xml` → Select it → Choose "Import project from external model" → Select **Maven** → Finish
5. Once module is selected, go to **Sources** tab
6. You should see source folders listed. If `src/main/java` is NOT marked as "Sources" (blue folder):
   - Click `src/main/java` in the list
   - Click the **"Sources"** button above (or check "Sources" checkbox)
ICE
7. Do the same for `src/main/resources` - mark it as **Resources** (green folder)
8. Click **OK**

### Step 2: Manually Mark Source Folders (If Step 1 didn't work)
1. In the Project Explorer (left panel), find `src/main/java`
2. **Right-click** on `src/main/java`
3. Select **Mark Directory as** → **Sources Root**
4. You should see the folder turn **blue** (means it's recognized as source)
5. **Right-click** on `src/main/resources`
6. Select **Mark Directory as** → **Resources Root**
7. It should turn **green**

### Step 3: Invalidate Caches
1. Go to **File** → **Invalidate Caches...**
2. Check **"Clear file system cache and Local History"**
3. Check **"Clear downloaded shared indexes"**
4. Click **Invalidate and Restart**
5. Wait for IntelliJ to restart

### Step 4: Rebuild
1. After restart, go to **Build** → **Rebuild Project**
2. This should now work!

## If Still Not Working:

### Check Java SDK:
1. Press `Ctrl+Alt+Shift+S litter` → **Project** tab
2. Check **Project SDK** - should be Java 17 or 24 (matching your Java version)
3. Check **Project language level** - should match your SDK

### Re-import as Maven Project:
1. Close IntelliJ
2. Delete `.idea` folder in your project (if you see it)
3. Delete any `*.iml` files in the project
4. Reopen IntelliJ
5. Choose **Open** (not Import)
6. Select your project folder
7. When IntelliJ asks how to import, choose **Import from Maven** or **Maven Project**


