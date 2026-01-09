# Migrating Now in Android from Koin Annotations (KSP) to Koin Compiler Plugin

## Introduction

This guide walks through migrating the Now in Android app from Koin Annotations with KSP to the native Koin Compiler Plugin. The compiler plugin replaces KSP-based annotation processing with native Kotlin compiler integration, resulting in faster builds and tighter compiler integration.

**Current State**: Koin Annotations 2.3 with KSP
**Target State**: Koin Compiler Plugin 0.1.28

---

## Why Migrate to the Compiler Plugin?

### Performance Benefits
- **Faster builds**: No separate KSP processing step for Koin
- **Native compiler integration**: FIR + IR phases instead of KSP symbol processing
- **Reduced overhead**: Single compilation pass for dependency injection

### Same Annotations, Different Engine
The compiler plugin supports the same annotations you're already using:
- `@Module`, `@ComponentScan`, `@Configuration`
- `@Singleton`, `@Factory`, `@Scoped`
- `@KoinViewModel`, `@KoinWorker`
- JSR-330: `javax.inject.Singleton`, `javax.inject.Inject`, `javax.inject.Named`

### Simpler DSL
The compiler plugin also enables a cleaner DSL syntax:
```kotlin
// KSP style
single { MyService(get(), get()) }
singleOf(::MyService)

// Compiler plugin style
single<MyService>()  // Constructor resolved at compile-time
```

---

## Prerequisites

| Dependency | Version |
|------------|---------|
| Kotlin | 2.3.20-Beta1 |
| KSP (Room only) | 2.3.20-Beta1-1.0.31 |
| Room | 2.8.4 |
| Koin Compiler Plugin | 0.1.28 |

---

## Migration Steps

### Step 1: Update Version Catalog

Edit `gradle/libs.versions.toml`:

```diff
[versions]
- kotlin = "2.2.20"
+ kotlin = "2.3.20-Beta1"

- ksp = "2.3.2"
+ ksp = "2.3.20-Beta1-1.0.31"  # Keep for Room only

- room = "2.7.2"
+ room = "2.8.4"

- koin-annotations = "2.3.1"
+ koinCompilerPlugin = "0.1.28"

[libraries]
# Remove these lines:
- koin-annotations = {module = "io.insert-koin:koin-annotations", version.ref = "koin-annotations"}
- koin-ksp-compiler = {module = "io.insert-koin:koin-ksp-compiler", version.ref = "koin-annotations"}

# Keep these (unchanged):
koin-core = {group = "io.insert-koin", name = "koin-core", version.ref = "koin"}
koin-android = {group = "io.insert-koin", name = "koin-android", version.ref = "koin"}
koin-androidx-worker = {group = "io.insert-koin", name = "koin-androidx-workmanager", version.ref = "koin"}
koin-compose-viewmodel = {group = "io.insert-koin", name = "koin-compose-viewmodel", version.ref = "koin"}

[plugins]
# Keep KSP for Room:
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }

# Add Koin Compiler Plugin:
+ koin-compiler = { id = "io.insert-koin.compiler.plugin", version.ref = "koinCompilerPlugin" }
```

**Current → Target versions:**

| Dependency | Current | Target |
|------------|---------|--------|
| Kotlin | 2.2.20 | 2.3.20-Beta1 |
| KSP | 2.3.2 | 2.3.20-Beta1-1.0.31 |
| Room | 2.7.2 | 2.8.4 |
| Koin | 4.2.0-beta2 | 4.2.0-beta2 (unchanged) |
| Koin Annotations | 2.3.1 (KSP) | 0.1.28 (Compiler Plugin) |

### Step 2: Update Root build.gradle.kts

```kotlin
plugins {
    // ... existing plugins

    // Keep KSP for Room
    alias(libs.plugins.ksp) apply false

    // Add Koin Compiler Plugin
    alias(libs.plugins.koin.compiler) apply false
}
```

### Step 3: Create Koin Convention Plugin (Recommended)

Create a new convention plugin to centralize Koin setup. This makes the migration cleaner and future updates easier.

**Create** `build-logic/convention/src/main/kotlin/KoinConventionPlugin.kt`:

```kotlin
import com.google.samples.apps.nowinandroid.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class KoinConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // Apply Koin Compiler Plugin
            pluginManager.apply("io.insert-koin.compiler.plugin")

            // Note: koin-annotations is auto-injected by the compiler plugin
            // You only need to add koin-core and other runtime libraries
        }
    }
}
```

**Register** the plugin in `build-logic/convention/build.gradle.kts`:

```kotlin
gradlePlugin {
    plugins {
        // ... existing plugins ...
        register("koin") {
            id = "nowinandroid.koin"
            implementationClass = "KoinConventionPlugin"
        }
    }
}
```

**Add** to `gradle/libs.versions.toml` in the plugins section (already done in Step 1):
```toml
[plugins]
koin-compiler = { id = "io.insert-koin.compiler.plugin", version.ref = "koinCompilerPlugin" }
```

**Add** to `build-logic/convention/build.gradle.kts` dependencies:
```kotlin
dependencies {
    // ... existing dependencies ...
    implementation(libs.plugins.koin.compiler.get().pluginId)
}
```

### Step 4: Update Module build.gradle.kts Files

Each module currently has `ksp(libs.koin.ksp.compiler)`. Replace with the Koin convention plugin.

**Modules to update:**

| Module | Current | After Migration |
|--------|---------|-----------------|
| `app` | `ksp(libs.koin.ksp.compiler)` | `id("nowinandroid.koin")` |
| `core:common` | `ksp(libs.koin.ksp.compiler)` | `id("nowinandroid.koin")` |
| `core:data` | `ksp(libs.koin.ksp.compiler)` | `id("nowinandroid.koin")` |
| `core:database` | `ksp(libs.koin.ksp.compiler)` | `id("nowinandroid.koin")` + keep Room KSP |
| `core:datastore` | `ksp(libs.koin.ksp.compiler)` | `id("nowinandroid.koin")` |
| `core:domain` | `ksp(libs.koin.ksp.compiler)` | `id("nowinandroid.koin")` |
| `core:network` | `ksp(libs.koin.ksp.compiler)` | `id("nowinandroid.koin")` |
| `core:notifications` | `ksp(libs.koin.ksp.compiler)` | `id("nowinandroid.koin")` |
| `core:analytics` | `ksp(libs.koin.ksp.compiler)` | `id("nowinandroid.koin")` |
| `feature:bookmarks` | `ksp(libs.koin.ksp.compiler)` | `id("nowinandroid.koin")` |
| `feature:foryou` | `ksp(libs.koin.ksp.compiler)` | `id("nowinandroid.koin")` |
| `feature:interests` | `ksp(libs.koin.ksp.compiler)` | `id("nowinandroid.koin")` |
| `feature:search` | `ksp(libs.koin.ksp.compiler)` | `id("nowinandroid.koin")` |
| `feature:settings` | `ksp(libs.koin.ksp.compiler)` | `id("nowinandroid.koin")` |
| `feature:topic` | `ksp(libs.koin.ksp.compiler)` | `id("nowinandroid.koin")` |
| `sync:work` | `ksp(libs.koin.ksp.compiler)` | `id("nowinandroid.koin")` |

**Example: core/data/build.gradle.kts**

Before:
```kotlin
plugins {
    alias(libs.plugins.nowinandroid.android.library)
    alias(libs.plugins.nowinandroid.android.library.jacoco)
}

dependencies {
    // ...
    ksp(libs.koin.ksp.compiler)  // Remove this
}
```

After:
```kotlin
plugins {
    alias(libs.plugins.nowinandroid.android.library)
    alias(libs.plugins.nowinandroid.android.library.jacoco)
    id("nowinandroid.koin")  // Add this
}

dependencies {
    // ... (remove ksp line)
}
```

**Example: core/database/build.gradle.kts** (has Room)

Before:
```kotlin
plugins {
    alias(libs.plugins.nowinandroid.android.library)
    alias(libs.plugins.nowinandroid.android.room)
}

dependencies {
    ksp(libs.koin.ksp.compiler)  // Remove this
    ksp(libs.room.compiler)      // Keep this for Room
}
```

After:
```kotlin
plugins {
    alias(libs.plugins.nowinandroid.android.library)
    alias(libs.plugins.nowinandroid.android.room)
    id("nowinandroid.koin")  // Add this
}

dependencies {
    ksp(libs.room.compiler)  // Keep for Room only
}
```

### Step 5: Code Changes (Minimal)

#### No Changes Required For:

**JSR-330 annotations** - Already supported:
```kotlin
// These work exactly the same
import javax.inject.Inject
import javax.inject.Singleton
import javax.inject.Named

@Singleton
class OfflineFirstUserDataRepository @Inject constructor(
    private val niaPreferencesDataSource: NiaPreferencesDataSource,
    @Named("io") private val ioDispatcher: CoroutineDispatcher
) : UserDataRepository
```

**Koin annotations** - Already supported:
```kotlin
@Module
@ComponentScan("com.google.samples.apps.nowinandroid.core.data")
@Configuration
class DataModule
```

**ViewModel annotations** - Already supported:
```kotlin
@KoinViewModel
class ForYouViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,
    private val getFollowableTopics: GetFollowableTopicsUseCase
) : ViewModel()
```

#### Remove (If Present):

```kotlin
// Remove @Monitor annotations (not supported in compiler plugin)
// @Monitor  <-- Remove this
class SomeService
```

### Step 6: Sync and Build

```bash
# Clean build to ensure fresh compilation
./gradlew clean

# Build the project
./gradlew assembleDebug

# Run tests to verify DI graph
./gradlew testDebugUnitTest
```

---

### Step 6: Update KSP Generated Import

The KSP-based Koin Annotations generates code in `org.koin.ksp.generated` package. The compiler plugin generates in `org.koin.plugin.generated`.

**In `app/src/main/kotlin/.../NiaApplication.kt`:**

Before:
```kotlin
import org.koin.ksp.generated.*
```

After:
```kotlin
// The compiler plugin auto-discovers @Configuration modules
// No explicit import needed if using @Configuration
// Or use: import org.koin.plugin.generated.* if needed
```

---

## Module-by-Module Checklist

| Module | Uses Room? | KSP Needed? | Migration Action |
|--------|------------|-------------|------------------|
| `app` | No | No | Add `id("nowinandroid.koin")`, remove `ksp(libs.koin.ksp.compiler)` |
| `core:common` | No | No | Add `id("nowinandroid.koin")`, remove `ksp(libs.koin.ksp.compiler)` |
| `core:data` | No | No | Add `id("nowinandroid.koin")`, remove `ksp(libs.koin.ksp.compiler)` |
| `core:database` | Yes | Yes | Add `id("nowinandroid.koin")`, keep `ksp(libs.room.compiler)` only |
| `core:datastore` | No | No | Add `id("nowinandroid.koin")`, remove `ksp(libs.koin.ksp.compiler)` |
| `core:domain` | No | No | Add `id("nowinandroid.koin")`, remove `ksp(libs.koin.ksp.compiler)` |
| `core:network` | No | No | Add `id("nowinandroid.koin")`, remove `ksp(libs.koin.ksp.compiler)` |
| `core:notifications` | No | No | Add `id("nowinandroid.koin")`, remove `ksp(libs.koin.ksp.compiler)` |
| `core:analytics` | No | No | Add `id("nowinandroid.koin")`, remove `ksp(libs.koin.ksp.compiler)` |
| `feature:bookmarks` | No | No | Add `id("nowinandroid.koin")`, remove `ksp(libs.koin.ksp.compiler)` |
| `feature:foryou` | No | No | Add `id("nowinandroid.koin")`, remove `ksp(libs.koin.ksp.compiler)` |
| `feature:interests` | No | No | Add `id("nowinandroid.koin")`, remove `ksp(libs.koin.ksp.compiler)` |
| `feature:search` | No | No | Add `id("nowinandroid.koin")`, remove `ksp(libs.koin.ksp.compiler)` |
| `feature:settings` | No | No | Add `id("nowinandroid.koin")`, remove `ksp(libs.koin.ksp.compiler)` |
| `feature:topic` | No | No | Add `id("nowinandroid.koin")`, remove `ksp(libs.koin.ksp.compiler)` |
| `sync:work` | No | No | Add `id("nowinandroid.koin")`, remove `ksp(libs.koin.ksp.compiler)` |

---

## Troubleshooting

### Build Error: "Unresolved reference: koin"

The compiler plugin auto-injects `koin-annotations` dependency. If you see this error:
1. Ensure the plugin is applied: `id("io.insert-koin.compiler.plugin")`
2. Sync Gradle files
3. Invalidate caches and restart (Android Studio)

### Runtime Error: "No definition found for class X"

This usually means a class wasn't scanned. Check:
1. The class has a definition annotation (`@Singleton`, `@Factory`, etc.)
2. The package is included in `@ComponentScan`
3. The module has `@Configuration` for auto-discovery

### KSP Errors After Migration

If you see KSP errors related to Koin:
1. Remove `ksp(libs.koin.annotations.ksp)` from all modules
2. Ensure you're not applying KSP plugin unnecessarily
3. KSP should only be used for Room

### Incremental Compilation Issues

If you experience issues with incremental compilation:
```properties
# In gradle.properties (temporary workaround)
kotlin.incremental=false
kotlin.incremental.multiplatform=false
```

---

## Verification Checklist

After migration, verify:

- [ ] App builds successfully
- [ ] All unit tests pass
- [ ] All UI tests pass
- [ ] App launches without DI crashes
- [ ] All features work correctly
- [ ] Offline mode works (verifies Repository injection)
- [ ] Sync works (verifies WorkManager/Worker injection)
- [ ] Settings persist (verifies DataStore injection)

---

## Benefits After Migration

1. **Faster Builds**: No separate KSP step for Koin processing
2. **Simpler Dependencies**: No `koin-annotations-ksp` processor to manage
3. **Native Integration**: Direct Kotlin compiler integration via FIR + IR
4. **Same Annotations**: All existing annotations work unchanged
5. **JSR-330 Support**: Full compatibility with `javax.inject.*` and `jakarta.inject.*`
6. **Future-Proof**: Better positioned for Kotlin compiler evolution

---

## Rollback Plan

If issues arise, rollback by:

1. Revert `libs.versions.toml` changes
2. Revert convention plugin changes
3. Re-add KSP dependencies to modules
4. Sync and rebuild

The code itself doesn't change, so rollback is straightforward.

---

## References

- [Koin Compiler Plugin Documentation](https://insert-koin.io/docs/reference/koin-compiler-plugin/)
- [Kotlin Compiler Plugin Basics](../compiler-plugin-template/docs/COMPILER_BASICS.md)
- [Original Hilt to Koin Migration](./MIGRATION_GUIDE.md)