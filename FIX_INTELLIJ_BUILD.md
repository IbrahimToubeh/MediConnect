# Fix IntelliJ Build Failure

## Problem
IntelliJ cannot find packages like `com.MediConnect.EntryRelated.dto`, `com.MediConnect.config`, etc., even though all files exist.

## Solution Steps (Do these in order):

### Step 1: Reload Maven Project
1. Right-click on `pom.xml` in the project tree
2. Select **Maven** → **Reload Project**
3. Wait for Maven to finish downloading dependencies

### Step 2: Mark Source Folders Correctly
1. Right-click on `src/main/java` folder
2. Select **Mark Directory as** → **Sources Root**
3. Right-click on `src/main/resources` folder  
4. Select **Mark Directory as** → **Resources Root**
5. Right-click on `src/test/java` folder (if exists)
6. Select **Mark Directory as** → **Test Sources Root**

### Step 3: Invalidate Caches and Restart
1. Go to **File** → **Invalidate Caches...**
2. Check **"Invalidate and Restart"**
3. Click **"Invalidate and Restart"**
4. Wait for IntelliJ to restart

### Step 4: Rebuild Project
1. Go to **Build** → **Rebuild Project**
2. Wait for compilation to complete

### Step 5: If Still Not Working - Check Project Structure
1. Go to **File** → **Project Structure** (or press `Ctrl+Alt+Shift+S`)
2. Select **Modules** → Select your `MediConnect` module
3. Go to **Sources** tab
4. Verify that `src/main/java` is marked as **Sources**
5. Verify that `src/main/resources` is marked as **Resources**
6. Click **OK**

### Step 6: Check Maven Settings
1. Go to **File** → **Settings** → **Build, Execution, Deployment** → **Build Tools** → **Maven**
2. Verify:
   - **Maven home path** is set correctly
   - **User settings file** points to your `settings.xml` (if you have one)
   - **Local repository** is set correctly
3. Click **OK**

## Alternative: Use Maven from Command Line
If IntelliJ continues to have issues, you can compile from command line:

```bash
cd MediConnect
mvn clean compile
```

Then reload the project in IntelliJ.

## Verification
After following these steps, try building again. The errors should disappear once IntelliJ recognizes the source folders correctly.

