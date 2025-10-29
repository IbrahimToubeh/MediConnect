# Quick Fix for ApplicationContext Startup Error

## Get Full Error Message

**In IntelliJ Console, look for the complete error stack trace**, especially:
- Lines starting with `org.springframework.context.ApplicationContextException`
- Lines starting with `Caused by:`
- Any `BeanCreationException` or `UnsatisfiedDependencyException`

## Common Issues & Quick Fixes

### Issue 1: Redis Connection Failed
If Redis is not running, the app will fail to start.

**Quick Fix - Disable Redis temporarily:**
1. Open `application.properties`
2. Change line 30:
   ```properties
   # spring.cache.type=redis
   spring.cache.type=simple
   ```
3. Comment out Redis connection:
   ```properties
   # spring.data.redis.host=localhost
   # spring.data.redis.port=6379
   ```

### Issue 2: Database Connection Failed
Verify PostgreSQL is running:
```powershell
Get-Service postgresql*
```

### Issue 3: Port Already in Use
If port 8080 is already in use:
```powershell
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

## Share Full Error

Copy the **complete error stack trace** from IntelliJ console (from "Error starting ApplicationContext" down to the bottom) so I can provide a targeted fix.

