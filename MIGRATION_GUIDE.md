# Migrating Now in Android from Hilt to Koin Annotations 2.3

## Introduction

This article walks through the complete migration of Google's Now in Android app from Dagger Hilt to Koin Annotations 2.3. If you're not familiar with it, Now in Android is Google's official sample app showcasing modern Android development patterns. It's a real production-quality news app with 30 Gradle modules, offline-first architecture, WorkManager sync, and a full Jetpack Compose UI with Material 3.

What makes this migration interesting is the scale. We're talking about ~40 dependency injection components spread across multiple layers: ViewModels, repositories, use cases, DAOs, network clients, and various utilities. The app uses custom qualifiers, scoped components, and complex dependency graphs. In other words, it's not a toy example.

The entire migration took about 2 hours. Most of that time was spent on setup and verification rather than actual code changes. Let me show you how we did it.

---

## Why This Migration Matters

Koin Annotations 2.2 was a major release that fundamentally changed how Koin handles enterprise-scale applications. Version 2.3 extends this with improved KSP compatibility. Let me break down the key features that made this migration possible:

### JSR-330 Compatibility

The game-changer is full JSR-330 standard support. Your existing `@Inject` constructors, `@Singleton` annotations, and custom `@Qualifier` annotations work without changes. This isn't just convenience - it means your business logic stays framework-agnostic. The domain layer doesn't care whether you're using Hilt, Koin, or any other JSR-330 compatible framework.

Coming from Hilt, this is huge. You're not rewriting your dependency graph. The repositories, use cases, and data sources that took months to build? They keep working.

### Smart Configurations with @Configuration

The `@Configuration` annotation marks modules for auto-discovery. Instead of manually listing every module in your `startKoin` block, KSP scans your project at compile time and generates the module list automatically.

This becomes critical in multi-module projects. Now in Android has 30 Gradle modules, and manually tracking which modules to load would be error-prone. With `@Configuration`, each module declares itself, and Koin finds it.

### ComponentScan for Package-Based Discovery

`@ComponentScan` tells Koin to scan entire packages for annotated classes. Instead of writing provider functions for every repository, use case, and ViewModel, you annotate the package once:

```kotlin
@Module
@ComponentScan("com.google.samples.apps.nowinandroid.feature")
class FeaturesModule
```

This automatically discovers all `@KoinViewModel` classes in the feature package. Adding a new feature? Just annotate the ViewModel. No module updates needed.

### Scope Archetypes

Scope archetypes like `@ActivityScope`, `@FragmentScope`, and `@ViewModelScope` simplify lifecycle management. Instead of manually creating and destroying scoped instances, you mark the provider function with an archetype:

```kotlin
@ActivityScope
fun jankStats(activity: ComponentActivity): JankStats
```

Koin ties the instance to the Activity lifecycle automatically. When the Activity dies, the instance is cleaned up. No leaks, no manual management.

### KSP Generation & Compile Safety

Koin Annotations uses KSP to consolidate annotations metadata and generate Koin DSL code at compile time. This gives you compile-time safety - missing dependencies fail the build with clear error messages, not at runtime.

It's worth noting that Koin itself has never used reflection. The annotations provide a faster, more scalable workflow on top of Koin's existing DSL. You get the developer experience of annotations with the performance of generated code.

### Version 2.3 Update

Koin Annotations 2.3.0 works with KSP 2.3.1, bringing compatibility with the latest Kotlin compiler and KSP versions. The core features from 2.2 remain the foundation for enterprise migration scenarios like this one.

---

## The Migration Strategy

We followed a top-down approach, which might seem counterintuitive if you're used to bottom-up migrations. Here's the order:

```
1. Application Entry Point (@KoinApplication)
2. Activity Components (JankStats)
3. Core Infrastructure (Dispatchers)
4. ViewModels
5. Use Cases
6. Repositories
7. Data Foundation (Database, Network)
```

Why top-down? Because you need the Koin container running before you can migrate anything else. And once it's running, you get immediate value by migrating user-facing features like ViewModels first. You can test the app at each step, which makes debugging much easier.

### The Dagger Bridge: Progressive Migration

Here's the important part: we didn't migrate everything at once. Koin 4.1.2 introduced the `koin-dagger-bridge` module that lets Koin and Dagger coexist in the same app. This means you can migrate components gradually while the app continues to work.

The bridge provides a `scope.dagger<T>()` extension function that retrieves Dagger's `@EntryPoint` instances. Here's how it works:

Step 1: Define a Dagger EntryPoint

```kotlin
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DataModuleBridge {
   fun newsResourceDao(): NewsResourceDao
   fun topicDao(): TopicDao
   fun niaPreferencesDataSource(): NiaPreferencesDataSource
}
```

This is standard Dagger - an `@EntryPoint` interface that exposes components from Hilt's `SingletonComponent`.

Step 2: Access from Koin

```kotlin
@Module
@Configuration
class DataKoinModule {
   @Factory
   fun newsResourceDao(scope: Scope): NewsResourceDao =
      scope.dagger<DataModuleBridge>().newsResourceDao()
}
```

The `scope.dagger<T>()` function calls Dagger's `EntryPoints.get()` under the hood, retrieving the Hilt-managed instance and making it available to Koin's dependency graph.

Why `@Factory` instead of `@Single`?

Because Dagger already manages the singleton lifecycle. If you use `@Single` in Koin, you'd cache Dagger's instance in Koin's container, creating dual ownership. `@Factory` means "retrieve from Dagger on each request" without Koin trying to manage the lifecycle. Dagger remains the source of truth.

This pattern let us migrate ViewModels to Koin while their repository dependencies were still in Dagger. Once the ViewModels worked, we migrated the repositories. Then the DAOs. Layer by layer, replacing bridge functions with native Koin components.

The final commit removed the bridge entirely. But during migration, it was essential for keeping the app functional.

---

## Step 1: Bootstrap the Koin Container

Everything starts with setting up the application class. This is where you initialize Koin and tell it where to find your modules.

The Hilt version:
```kotlin
@HiltAndroidApp
class NiaApplication : Application(), ImageLoaderFactory {
   override fun onCreate() {
      super.onCreate()
      // Hilt handles everything automatically
   }
}
```

After migration:
```kotlin
@KoinApplication
class NiaApplication : Application(), ImageLoaderFactory {

   private val imageLoader: ImageLoader by inject()
   private val profileVerifierLogger: ProfileVerifierLogger by inject()

   override fun onCreate() {
      startKoin {
         androidContext(this@NiaApplication)
         workManagerFactory()
      }

      super.onCreate()

      Sync.initialize(context = this)
      profileVerifierLogger()
   }

   override fun newImageLoader(): ImageLoader = imageLoader
}
```

A few things to note here:

1. `@KoinApplication` replaces `@HiltAndroidApp`. This annotation triggers KSP to scan for all `@Configuration` modules in your project.

2. `startKoin` is called in the first lines of `onCreate()`. If you're doing a progressive migration with both Hilt and Koin, call it before Hilt initialization to help with colocation.

3. `workManagerFactory()` integrates Koin with WorkManager, so your Workers can use dependency injection.

4. Dependencies like `imageLoader` are injected using `by inject()`. This is field injection - the instance is created lazily when you first access the property.

At this point, the app won't build yet because we haven't created any Koin modules. But the foundation is there.

Git commit: `a2cf7448 - Koin setup with annotations + @KoinApplication entry point`

---

## Step 2: Activity Scopes and JankStats

Now in Android uses JankStats from the Metrics library to track UI performance. The interesting part is that JankStats needs to be scoped to the Activity lifecycle - it should be created when the Activity starts and destroyed when it's finished.

Hilt handles this with custom scopes. Koin Annotations 2.3 has a simpler approach: scope archetypes. For Activities, you use `@ActivityScope`.

The module:
```kotlin
@Module
@Configuration
class JankStatsKoinModule {

   @ActivityScope
   fun jankStats(activity: ComponentActivity): JankStats =
      JankStats.createAndTrack(activity.window, providesOnFrameListener())
}

fun providesOnFrameListener(): OnFrameListener = OnFrameListener { frameData ->
   if (frameData.isJank) {
      Log.v("NiA Jank", frameData.toString())
      KotzillaSDK.log("NiA Jank - $frameData")
   }
}
```

The `@ActivityScope` annotation tells Koin to create one instance per Activity and tie its lifecycle to that Activity. When the Activity is destroyed, Koin automatically cleans up the JankStats instance.

Using it in MainActivity:
```kotlin
class MainActivity : ComponentActivity(), AndroidScopeComponent {

   override val scope: Scope by activityScope()

   private val lazyStats: JankStats by inject()
   private val networkMonitor: NetworkMonitor by inject()
   private val timeZoneMonitor: TimeZoneMonitor by inject()
   private val analyticsHelper: AnalyticsHelper by inject()
   private val viewModel: MainActivityViewModel by viewModel()

   override fun onResume() {
      super.onResume()
      lazyStats.isTrackingEnabled = true
   }

   override fun onPause() {
      super.onPause()
      lazyStats.isTrackingEnabled = false
   }
}
```

Two things make this work:

1. `AndroidScopeComponent` interface marks the Activity as scope-aware.
2. `override val scope: Scope by activityScope()` creates and manages the scope.

The nice thing about this pattern is there's no manual lifecycle management. You don't need to worry about leaking the JankStats instance or cleaning it up manually. Koin handles it.

Git commit: `ef16e37d - JankStatsKoinModule (in config) for jankStats definition in @ActivityScope`

---

## Step 3: Core Infrastructure (Dispatchers)

Before migrating the main application components, we need the foundational pieces that everything depends on. In Now in Android, that's coroutine dispatchers.

The app uses a custom `@Qualifier` annotation to distinguish between IO and Default dispatchers:

```kotlin
@Qualifier
@Retention(RUNTIME)
annotation class Dispatcher(val niaDispatcher: NiaDispatchers)

enum class NiaDispatchers {
   Default,
   IO,
}
```

This annotation doesn't change at all between Hilt and Koin. JSR-330 qualifiers work identically in both frameworks.

The Koin module:
```kotlin
@Module
@Configuration
object DispatchersKoinModule {

   @Singleton
   @Dispatcher(IO)
   fun providesIODispatcher(): CoroutineDispatcher = Dispatchers.IO

   @Singleton
   @Dispatcher(NiaDispatchers.Default)
   fun providesDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
```

What changed from Hilt:
- Removed `@InstallIn(SingletonComponent::class)` - not needed anymore
- Removed `@Provides` - Koin infers this from the function
- Added `@Configuration` so Koin auto-discovers this module

The `@Singleton` annotation and the custom `@Dispatcher` qualifier stay exactly the same. This is the power of JSR-330 compatibility - your domain code doesn't care about the DI framework.

Coroutine Scopes:

```kotlin
@Module
@Configuration
object CoroutineScopesKoinModule {

   @Singleton
   fun providesCoroutineScope(
      @Dispatcher(NiaDispatchers.Default) dispatcher: CoroutineDispatcher,
   ): CoroutineScope = SupervisorJob() + dispatcher
}
```

Notice how the `@Dispatcher(NiaDispatchers.Default)` parameter works seamlessly. Koin resolves the qualified dependency just like Hilt did.

### Bidirectional Bridge: Dagger Consuming Koin Dependencies

During progressive migration, you might need Dagger components to consume dependencies that have already been migrated to Koin. The bridge works in both directions.

For Dagger to access Koin components, create a Hilt module that retrieves instances from the Koin container:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DispatchersHiltModule {

    @Provides
    fun providesCoroutineScope(): CoroutineScope = KoinPlatform.getKoin().get()
}
```

This pattern uses `KoinPlatform.getKoin()` to access the Koin container and retrieve instances. Now Dagger-managed components can depend on the CoroutineScope that is defined in Koin.

Why this matters during migration:
- You can migrate infrastructure first (like Dispatchers) to Koin
- Existing Hilt components continue working, pulling from Koin
- No need to keep duplicate definitions in both frameworks
- Clean migration path: Koin becomes the source of truth layer by layer

The key is that the bridge is temporary. Once you've migrated all dependent components, you remove these bridge modules entirely.

Git commit: `9e0b5711 - Bridge Core Coroutines/Scopes/Dispatchers`

---

## Step 4: ViewModels (Top-Down Migration)

This is where the migration gets interesting. We migrated all 8 ViewModels before touching repositories or use cases. Why? Because ViewModels are the entry point to your features. If they work, you know dependency resolution is working correctly.

At this stage, we're still bridging all ViewModel dependencies (repositories, use cases) from Dagger. The ViewModels themselves move to Koin, but their dependencies remain in Dagger temporarily.

The change is minimal. Replace `@HiltViewModel` with `@KoinViewModel` and remove `@Inject` from the constructor:

Before (Hilt):
```kotlin
@HiltViewModel
class BookmarksViewModel @Inject constructor(
   private val userDataRepository: UserDataRepository,
   userNewsResourceRepository: UserNewsResourceRepository,
) : ViewModel() {
   // implementation
}
```

After (Koin):
```kotlin
@KoinViewModel
class BookmarksViewModel(
   private val userDataRepository: UserDataRepository,
   userNewsResourceRepository: UserNewsResourceRepository,
) : ViewModel() {
   // implementation unchanged
}
```

That's it. The implementation stays the same. All dependencies are resolved automatically.

SearchViewModel is the most complex ViewModel in the app. It has 7 dependencies including `SavedStateHandle`:

```kotlin
@KoinViewModel
class SearchViewModel(
   getSearchContentsUseCase: GetSearchContentsUseCase,
   recentSearchQueriesUseCase: GetRecentSearchQueriesUseCase,
   private val searchContentsRepository: SearchContentsRepository,
   private val recentSearchRepository: RecentSearchRepository,
   private val userDataRepository: UserDataRepository,
   private val savedStateHandle: SavedStateHandle,
   private val analyticsHelper: AnalyticsHelper,
) : ViewModel() {

   val searchQuery = savedStateHandle.getStateFlow(
      key = SEARCH_QUERY,
      initialValue = ""
   )
   // ... rest of implementation
}
```

`SavedStateHandle` is injected automatically by Koin, just like it was with Hilt. No special configuration needed.

How ViewModels are discovered:

Instead of manually declaring each ViewModel, we use `@ComponentScan`:

```kotlin
@Module(includes = [FeaturesModule::class, DomainModule::class])
@ComponentScan("com.google.samples.apps.nowinandroid.util", "com.google.samples.apps.nowinandroid.ui")
@Configuration
class AppModule {
   @KoinViewModel
   fun mainActivityViewModel(userDataRepository: UserDataRepository) =
      MainActivityViewModel(userDataRepository)
}

@Module
@ComponentScan("com.google.samples.apps.nowinandroid.feature")
class FeaturesModule
```

`FeaturesModule` scans the entire feature package and automatically registers all classes annotated with `@KoinViewModel`. This scales much better than manually listing each ViewModel.

All 8 ViewModels migrated:
- BookmarksViewModel
- InterestsViewModel
- SearchViewModel
- SettingsViewModel
- TopicViewModel
- ForYouViewModel
- MainActivityViewModel
- Interests2PaneViewModel

At this point, the app builds and the UI works. ViewModels are created successfully, even though their dependencies (repositories, use cases) are still using `@Inject` constructors. This is JSR-330 compatibility in action.

Git commit: `b7d9f4a9 - Migrate all ViewModel to Koin`

---

## Step 5: Domain Layer (Use Cases)

Use cases in Now in Android follow a simple pattern: they take repository interfaces in the constructor and expose one or more operations. They all use `@Inject` constructor injection.

The good news: these don't need to change at all. Koin's JSR-330 support means existing `@Inject` constructors work without modification.

GetFollowableTopicsUseCase:
```kotlin
class GetFollowableTopicsUseCase @Inject constructor(
   private val topicsRepository: TopicsRepository,
   private val userDataRepository: UserDataRepository,
) {
   operator fun invoke(sortBy: TopicSortField = NONE): Flow<List<FollowableTopic>> =
      combine(
         userDataRepository.userData,
         topicsRepository.getTopics(),
      ) { userData, topics ->
         topics.map { topic ->
            FollowableTopic(
               topic = topic,
               isFollowed = topic.id in userData.followedTopics,
            )
         }.let { followedTopics ->
            when (sortBy) {
               NAME -> followedTopics.sortedBy { it.topic.name }
               else -> followedTopics
            }
         }
      }
}
```

No changes to the class itself. We just need to tell Koin where to find it:

```kotlin
@Module
@ComponentScan("com.google.samples.apps.nowinandroid.core.domain")
class DomainModule
```

This scans the domain package and registers all three use cases:
- GetFollowableTopicsUseCase
- GetSearchContentsUseCase
- GetRecentSearchQueriesUseCase

Git commit: `0f266ea5 - Scan/migrate UseCase injection into Koin`

---

## Step 6: Data Layer (Repositories)

Repositories in Now in Android use the `@Singleton` annotation and constructor injection. Like use cases, they don't need code changes - just discovery configuration.

OfflineFirstUserDataRepository:
```kotlin
@Singleton
internal class OfflineFirstUserDataRepository(
   private val niaPreferencesDataSource: NiaPreferencesDataSource,
   private val analyticsHelper: AnalyticsHelper,
) : UserDataRepository {

   override val userData: Flow<UserData> = niaPreferencesDataSource.userData

   override suspend fun setTopicIdFollowed(followedTopicId: String, followed: Boolean) {
      niaPreferencesDataSource.setTopicIdFollowed(followedTopicId, followed)
      analyticsHelper.logTopicFollowToggled(followedTopicId, followed)
   }

   // ... other methods
}
```

The `@Singleton` annotation stays. The constructor injection stays. Only the module configuration changes:

```kotlin
@Module
@Configuration
@ComponentScan("com.google.samples.apps.nowinandroid.core.data")
class DataKoinModule
```

Using custom qualifiers in repositories:

Some repositories need the IO dispatcher for database operations:

```kotlin
@Singleton
internal class DefaultSearchContentsRepository(
   private val newsResourceDao: NewsResourceDao,
   private val newsResourceFtsDao: NewsResourceFtsDao,
   private val topicDao: TopicDao,
   private val topicFtsDao: TopicFtsDao,
   @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : SearchContentsRepository {
   // implementation
}
```

The `@Dispatcher(IO)` qualifier works exactly as it did with Hilt. Koin resolves it to the IO dispatcher we defined earlier.

Git commit: `122cb2b1 - Move all repositories - update bridges`

---

## Step 7: Data Foundation (Database, Network, DataStore)

The lowest layer includes Room database, Retrofit network client, and DataStore. These components require provider functions because they involve builder patterns or factory methods.

Database module:
```kotlin
@Module
@Configuration
class DatabaseKoinModule {

   @Single
   fun providesNiaDatabase(context: Context): NiaDatabase =
      Room.databaseBuilder(
         context,
         NiaDatabase::class.java,
         "nia-database",
      ).build()
}
```

DAOs require a different approach because they're extracted from the database instance:

```kotlin
@Module(includes = [DatabaseKoinModule::class])
@Configuration
class DaosKoinModule {

   @Single
   fun providesTopicsDao(database: NiaDatabase): TopicDao =
      database.topicDao()

   @Single
   fun providesNewsResourceDao(database: NiaDatabase): NewsResourceDao =
      database.newsResourceDao()

   @Single
   fun providesTopicFtsDao(database: NiaDatabase): TopicFtsDao =
      database.topicFtsDao()

   @Single
   fun providesNewsResourceFtsDao(database: NiaDatabase): NewsResourceFtsDao =
      database.newsResourceFtsDao()

   @Single
   fun providesRecentSearchQueryDao(database: NiaDatabase): RecentSearchQueryDao =
      database.recentSearchQueryDao()
}
```

Each DAO is a function that takes the database and returns the DAO. Koin manages the lifecycle - these are all singletons tied to the database instance.

Network and DataStore use `@ComponentScan` for simpler components:

```kotlin
@Module
@Configuration
@ComponentScan("com.google.samples.apps.nowinandroid.core.network")
class NetworkKoinModule

@Module
@Configuration
@ComponentScan("com.google.samples.apps.nowinandroid.core.datastore")
class DataStoreKoinModule
```

This discovers network data sources, Retrofit interfaces, and DataStore configurations automatically.

---

## The Complete Module Structure

After migration, Now in Android has 8 configuration modules that Koin discovers automatically:

1. AppModule - Application-level components and ViewModels
2. JankStatsKoinModule - Activity-scoped performance monitoring
3. DispatchersKoinModule - Coroutine dispatchers with custom qualifiers
4. CoroutineScopesKoinModule - Application-scoped coroutines
5. DataKoinModule - Repositories and data sources
6. DatabaseKoinModule & DaosKoinModule - Room database and DAOs
7. NetworkKoinModule - Retrofit and network components
8. DataStoreKoinModule - Preferences and settings

The key is `@Configuration` on top-level modules. This tells KSP to include them in the generated module list. Koin loads all of them automatically when `startKoin` is called.

---

## What Actually Changed

Let's be specific about what code changed during migration:

Removed:
- `@HiltAndroidApp` from Application class
- `@InstallIn(SingletonComponent::class)` from all modules
- `@Provides` annotation from provider functions (though `@Single` is used for DAOs)
- `@HiltViewModel` from ViewModels
- `@Inject` from ViewModel constructors

Added:
- `@KoinApplication` to Application class
- `startKoin { }` configuration block
- `@Configuration` on 8 top-level modules
- `@ComponentScan` to enable auto-discovery
- `@KoinViewModel` on ViewModels
- `@ActivityScope` for JankStats

Unchanged:
- All `@Inject` constructors in use cases and repositories
- All `@Singleton` annotations
- All custom `@Qualifier` annotations
- ViewModel implementations
- Repository implementations
- Use case implementations
- Actually, pretty much all business logic

The migration touched mostly configuration code. The business logic layer - where most of your code lives - stayed the same.

---

## Build Configuration

You need to add Koin dependencies to each module that uses dependency injection. In `gradle/libs.versions.toml`:

```toml
[versions]
koin = "4.1.1"
koin-annotations = "2.3.0"

[libraries]
koin-core = {group = "io.insert-koin", name = "koin-core", version.ref = "koin"}
koin-android = {group = "io.insert-koin", name = "koin-android", version.ref = "koin"}
koin-androidx-worker = {group = "io.insert-koin", name = "koin-androidx-workmanager", version.ref = "koin"}
koin-compose-viewmodel = {group = "io.insert-koin", name = "koin-compose-viewmodel", version.ref = "koin"}
koin-annotations = {module = "io.insert-koin:koin-annotations", version.ref = "koin-annotations"}
koin-ksp-compiler = {module = "io.insert-koin:koin-ksp-compiler", version.ref = "koin-annotations"}
javax-inject = { module = "javax.inject:javax.inject", version = "1" }
```

In each module's `build.gradle.kts`:

```kotlin
plugins {
   // ... other plugins
   alias(libs.plugins.ksp)
}

dependencies {
   implementation(libs.koin.android)
   implementation(libs.koin.compose.viewmodel)
   implementation(libs.koin.annotations)
   ksp(libs.koin.ksp.compiler)

   // Keep JSR-330 for @Inject, @Singleton, @Qualifier
   implementation(libs.javax.inject)

   // For progressive migration (remove after migration complete)
   implementation("io.insert-koin:koin-androidx-dagger:$koin_version")
}
```

The JSR-330 dependency is important - that's what provides `@Inject`, `@Singleton`, and `@Qualifier` annotations.

The `koin-androidx-dagger` dependency provides the Dagger bridge. This is only needed during migration. Once you've fully migrated to Koin, you can remove both this dependency and the Hilt/Dagger dependencies.

---

## Common Patterns

### Pattern 1: Custom Qualifiers

Custom qualifiers are one of the most powerful features that "just work" between Hilt and Koin.

Definition (unchanged):
```kotlin
@Qualifier
@Retention(RUNTIME)
annotation class Dispatcher(val niaDispatcher: NiaDispatchers)

enum class NiaDispatchers {
   Default,
   IO,
}
```

Provider:
```kotlin
@Singleton
@Dispatcher(IO)
fun providesIODispatcher(): CoroutineDispatcher = Dispatchers.IO
```

Consumer:
```kotlin
class TimeZoneBroadcastMonitor(
   @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
   private val context: Application,
) : TimeZoneMonitor
```

### Pattern 2: ViewModels with SavedStateHandle

Koin automatically injects `SavedStateHandle` when a ViewModel requests it:

```kotlin
@KoinViewModel
class SearchViewModel(
   // ... other dependencies
   private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

   val searchQuery = savedStateHandle.getStateFlow(
      key = SEARCH_QUERY,
      initialValue = ""
   )
}
```

No special configuration needed. It just works.

### Pattern 3: Provider Functions for Complex Construction

When components need builder patterns or factory methods, use provider functions:

```kotlin
@Module
@Configuration
class DatabaseKoinModule {

   @Single
   fun providesNiaDatabase(context: Context): NiaDatabase =
      Room.databaseBuilder(
         context,
         NiaDatabase::class.java,
         "nia-database",
      ).build()
}
```

### Pattern 4: Module Composition

Modules can include other modules to express dependencies:

```kotlin
@Module(includes = [DatabaseKoinModule::class])
@Configuration
class DaosKoinModule {
   // DAOs depend on the database from DatabaseKoinModule
}
```

---

## Progressive Migration with the Dagger Bridge

One of the most valuable patterns in this migration was the ability to migrate incrementally. Instead of a "big bang" rewrite, we used the `koin-dagger-bridge` module to run both frameworks simultaneously.

### How the Bridge Works

The bridge is a simple Koin module that provides two extension functions:

```kotlin
// From koin-dagger-bridge module
inline fun <reified T> Scope.dagger() : T {
   return EntryPoints.get(androidContext().applicationContext, T::class.java)
}
```

This wraps Dagger's `EntryPoints.get()` API, making it accessible from Koin's dependency graph. You define a Dagger `@EntryPoint` interface listing the components you need:

```kotlin
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DataModuleBridge {
   fun recentSearchQueryDao(): RecentSearchQueryDao
   fun newsResourceDao(): NewsResourceDao
   fun topicDao(): TopicDao
   fun niaPreferencesDataSource(): NiaPreferencesDataSource
   fun network(): NiaNetworkDataSource
}
```

This is standard Dagger/Hilt code. The `@EntryPoint` interface exposes specific components from Hilt's `SingletonComponent`.

Then in your Koin module, you create provider functions that retrieve these from Dagger:

```kotlin
@Module
@Configuration
class DataKoinModule {

   @Factory
   fun recentSearchQueryDao(scope: Scope): RecentSearchQueryDao =
      scope.dagger<DataModuleBridge>().recentSearchQueryDao()

   @Factory
   fun newsResourceDao(scope: Scope): NewsResourceDao =
      scope.dagger<DataModuleBridge>().newsResourceDao()

   // ... other DAOs
}
```

When Koin needs a `RecentSearchQueryDao`, it calls the provider function, which calls `scope.dagger<DataModuleBridge>()` to get the EntryPoint, then calls `recentSearchQueryDao()` to get the instance from Dagger. Koin doesn't manage the lifecycle - it's just a pass-through to Dagger.

The key insight: use `@Factory`, not `@Single`.

Dagger already manages these as singletons. If you use `@Single` in Koin, you'd cache Dagger's instance in Koin's container, creating dual ownership. `@Factory` means "call the provider function every time this dependency is requested." Since the provider function just retrieves from Dagger, Dagger remains the single source of truth for the lifecycle.

### Migration Sequence

This allowed a very specific migration sequence:

1. Commit 9e0b5711: Bridge core infrastructure (Dispatchers, Scopes)
   - Migrated foundational pieces to Koin
   - Rest of the app still on Dagger
   - App fully functional

2. Commit dbe94482: Bridge data module
   - Created `DataModuleBridge` for DAOs and DataSources
   - Repositories still in Dagger, now accessible from Koin

3. Commit b7d9f4a9: Migrate ViewModels to Koin
   - All 8 ViewModels moved to `@KoinViewModel`
   - Dependencies still resolved through bridge
   - UI fully functional with Koin ViewModels

4. Commit 0f266ea5: Migrate Use Cases
   - Domain layer moved to Koin
   - Still accessing repositories through bridge

5. Commit 122cb2b1: Migrate repositories
   - Data layer moved to Koin
   - Updated bridge to only expose DAOs

6. Final commit: Remove bridge entirely
   - Everything in Koin
   - No more Dagger dependencies

At each step, the app built and ran. We could test incrementally, catching issues early rather than discovering them after migrating everything.

### Compile Safety & Debugging

The compile-time safety from KSP generation is crucial during migration. When a dependency is missing, the build fails with a clear error message telling you exactly what's not declared. This helps you track whether a dependency should come from Koin or Dagger during the transition.

The generated code is also easy to inspect. You can check the generated Koin modules to verify components are registered in the correct module. This visibility makes debugging much faster - you're not guessing where things went wrong.

### Why This Matters for Large Teams

In a real company scenario, this approach is crucial:

- Parallel work: Different teams can migrate their modules independently
- Gradual rollout: Merge migrations as they complete, not wait for everything
- Risk management: Each merge is a small change, easy to rollback
- Continuous delivery: No "migration branch" that diverges for weeks
- Learning curve: Team learns Koin patterns gradually, not all at once

For Now in Android specifically, this was overkill - one person migrating in 2 hours doesn't need progressive migration. But for a large app with multiple teams? The bridge pattern is essential.

---

## Wrapping Up

JSR-330 compatibility is the killer feature. The fact that you can keep your `@Inject` constructors, `@Singleton` annotations, and custom qualifiers means the migration is mostly mechanical. You're changing configuration, not business logic.

`@ComponentScan` scales really well. Instead of manually registering 8 ViewModels, 3 use cases, and 6 repositories, we scan packages and let Koin discover them. Adding a new component is as simple as annotating it - no module updates required.

Compile-time generation and safety work. We had zero runtime crashes related to missing dependencies. If something was misconfigured, the build failed with a clear error message pointing to the exact problem.

Activity scopes are cleaner than expected. The `@ActivityScope` pattern for JankStats is simpler than Hilt's custom scopes. It's just an annotation on the provider function and an interface on the Activity. Lifecycle management is automatic.

Top-down migration is practical. Starting with the application entry point and working down through ViewModels to repositories means you can test the app at each step. You're not waiting until the entire migration is done to see if it works.

The Dagger bridge enables low-risk migration. Being able to run both frameworks simultaneously is a game-changer for large apps. You can migrate layer by layer, merge to main after each step, and the app stays functional throughout. This isn't just a technical feature - it's a team coordination tool.

---

## Should You Migrate?

If you're on Hilt and it's working fine, there's no urgent reason to switch. But if you're:

- Starting a new project and choosing a DI framework
- Frustrated with Hilt's complexity or build times
- Building a multi-module app and want better organization
- Looking for better testing ergonomics
- Building a Kotlin Multiplatform (KMP) app and need cross-platform DI

Then Koin Annotations 2.3 is worth considering. The JSR-330 compatibility means migration risk is low, and the compile-time safety addresses the main criticism of earlier Koin versions. For KMP projects, Koin is the natural choice since Hilt doesn't support multiplatform.

For Now in Android specifically, the migration took 2 hours and resulted in cleaner module organization. We removed boilerplate, improved discoverability with `@ComponentScan`, and simplified activity scoping. That's a win.

## What's Next?

This migration focused on dependency injection infrastructure. In a future article, we'll explore application architecture tracing with the Kotzilla platform - monitoring your app's performance in production, tracking dependency resolution timing, and identifying bottlenecks in real-world usage.

---

## Resources

- Now in Android Repository: [github.com/android/nowinandroid](https://github.com/android/nowinandroid)
- Koin Documentation: [insert-koin.io](https://insert-koin.io/)
- Koin Annotations Guide: [insert-koin.io/docs/reference/koin-annotations](https://insert-koin.io/docs/reference/koin-annotations/start)
- Kotzilla Platform: [kotzilla.io](https://kotzilla.io/) (optional monitoring)

---

Arnaud Giuliani
January 2025
Koin 4.1.1 | Koin Annotations 2.3.0
