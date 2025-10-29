# How to Use Maven in IntelliJ IDEA

## Accessing Maven

### Method 1: Maven Tool Window (Recommended)
1. **Open the Maven tool window:**
   - Go to **View** → **Tool Windows** → **Maven**
   - Or press `Alt + 1` (or `Cmd + 1` on Mac), then select "Maven" tab
   - Or click the **Maven icon** in the right sidebar (looks like a project structure icon)

2. **You'll see:**
   - Your project (`MediConnect`)
   - **Lifecycle** folder with Maven goals:
     - `clean` - Cleans the build directory
     - `validate` - Validates the project
     - `compile` - Compiles source code
     - `test` - Runs tests
     - `package` - Packages the project
     - `install` - Installs to local repository
   - **Plugins** folder
   - **Dependencies** folder

### Method 2: Right-Click on pom.xml
1. Right-click on `pom.xml` in the Project view
2. Select **Maven** → You'll see options like:
   - **Reload Project** - Reloads Maven project configuration
   - **Add Maven Project**
   - **Show Dependencies**
   - **Generate Sources and Update Folders**

### Method 3: Run Configuration
1. Go to **Run** → **Edit Configurations...**
2. Click `+` → **Maven**
3. Configure your Maven command (e.g., `clean compile`)

## Common Actions After Making Changes

### After Adding New Files (like we just did):

1. **Reload Maven Project:**
   - Open Maven tool window (Method 1)
   - Click the **Reload All Maven Projects** button (circular arrows icon) at the top of the Maven window
   - Or: Right-click on `pom.xml` → **Maven** → **Reload Project**

2. **Rebuild Project:**
   - **Build** → **Rebuild Project**
   - Or press `Ctrl + Shift + F9` (Windows/Linux) or `Cmd + Shift + F9` (Mac)

3. **Run Maven Goals:**
   - In Maven tool window, expand **Lifecycle**
   - Double-click any goal to run it (e.g., `clean`, `compile`, `install`)

## Quick Actions

### Reload Project After Changes:
- **Maven tool window** → Click **Reload All Maven Projects** icon (🔄)
- Or: Right-click `pom.xml` → **Maven** → **Reload Project**

### Compile Project:
- **Maven tool window** → **Lifecycle** → Double-click `compile`
- Or: **Build** → **Build Project** (`Ctrl + F9` / `Cmd + F9`)

### Clean Build:
- **Maven tool window** → **Lifecycle** → Double-click `clean`
- Then double-click `compile` or `package`

## After Creating New Appointment Files

Since we just created new appointment-related files, you should:

1. **Reload Maven Project** ( Maven tool window → Reload icon)
2. **Rebuild Project** (Build → Rebuild Project)
3. **Run your Spring Boot application**

This ensures IntelliJ recognizes all the new classes and endpoints.


