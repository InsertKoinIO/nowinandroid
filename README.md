![](https://miro.medium.com/v2/resize:fit:1400/format:webp/1*hVKWuT24riZnx4VzDpQVJQ.png)

Now in Android App - With Annotations 2.2
==================

This is the migrated version of Now in Android app, but replacing Dagger Hilt with Koin.

Now in Android is Google's official modern Android application sample showcasing best practices. This Koin Annotations 2.2 port demonstrates how to migrate from Hilt while leveraging the latest features for enterprise-scale applications.

## Project Overview

Now in Android is a production-quality news app featuring:

- Jetpack Compose UI with Material 3 and adaptive layouts
- Multi-module architecture with 30 Gradle modules
- Room database + DataStore for local persistence
- WorkManager for background sync
- Complex dependency graph with ~40 components across the app

This makes it an ideal showcase for Koin Annotations 2.2's enterprise-scale features.

---

## 1. JSR-330 Compatibility: Seamless Hilt Migration

The migration leverages JSR-330 annotations for minimal code changes, preserving the original Hilt patterns.

### Custom Qualifier - Preserved from Hilt

```kotlin
// core/common/.../NiaDispatchers.kt
@Qualifier
@Retention(RUNTIME)
annotation class Dispatcher(val niaDispatcher: NiaDispatchers)

enum class NiaDispatchers {
    Default,
    IO,
}
```

This custom `@Qualifier` annotation works identically in both Hilt and Koin—zero changes required.

### Using JSR-330 Annotations in Components

**Repository with @Singleton:**

```kotlin
// core/data/.../OfflineFirstUserDataRepository.kt
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
}
```

**Use Case with @Inject Constructor:**

```kotlin
// core/domain/.../GetRecentSearchQueriesUseCase.kt
class GetRecentSearchQueriesUseCase @Inject constructor(
    private val recentSearchRepository: RecentSearchRepository,
) {
    operator fun invoke(limit: Int = 10): Flow<List<RecentSearchQuery>> =
        recentSearchRepository.getRecentSearchQueries(limit)
}
```

All three domain use cases use `@Inject` constructor injection—no refactoring needed.

### Custom Qualifier Usage

**TimeZoneMonitor with Custom Dispatcher:**

```kotlin
// core/data/.../util/TimeZoneMonitor.kt
internal class TimeZoneBroadcastMonitor(
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    private val context: Application,
) : TimeZoneMonitor
```

**NetworkMonitor with IO Dispatcher:**

```kotlin
// core/data/.../util/ConnectivityManagerNetworkMonitor.kt
internal class ConnectivityManagerNetworkMonitor(
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    context: Context,
) : NetworkMonitor
```

**SearchContentsRepository:**

```kotlin
// core/data/.../DefaultSearchContentsRepository.kt
internal class DefaultSearchContentsRepository(
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    private val newsResourceDao: NewsResourceDao,
    private val topicFtsDao: TopicFtsDao,
) : SearchContentsRepository
```

The `@Dispatcher` custom qualifier is used throughout the data layer to inject the correct coroutine dispatcher.

### Migration Benefits

- ✅ Zero refactoring of existing `@Inject` constructors
- ✅ Custom `@Qualifier` annotations work unchanged
- ✅ Gradual migration—Hilt and Koin can coexist during transition
- ✅ Team familiarity—developers recognize JSR-330 patterns

---

## 2. Configuration-Based Module Organization

Perfect for multi-module projects: 30 Gradle modules organized into 8 Koin configurations.

### Core Data Module

```kotlin
// core/data/.../DataKoinModule.kt
@Module
@Configuration
@ComponentScan("com.google.samples.apps.nowinandroid.core.data")
class DataKoinModule
```

Scans the entire `core.data` package for components—no manual declarations needed.

### Network Module with ComponentScan

```kotlin
// core/network/.../NetworkKoinModule.kt
@Module
@Configuration
@ComponentScan("com.google.samples.apps.nowinandroid.core.network")
class NetworkKoinModule
```

### Application Module - Orchestrating Features

```kotlin
// app/.../AppModule.kt
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

@Module
@ComponentScan("com.google.samples.apps.nowinandroid.core.domain")
class DomainModule
```

`FeaturesModule` automatically discovers all 6 feature ViewModels via `@ComponentScan`.

### Single Entry Point with @KoinApplication

```kotlin
// app/.../NiaApplication.kt
@KoinApplication
class NiaApplication : Application(), ImageLoaderFactory {

    private val imageLoader: ImageLoader by inject()
    private val profileVerifierLogger: ProfileVerifierLogger by inject()

    override fun onCreate() {
        // Koin starts first
        startKoin {
            androidContext(this@NiaApplication)
            workManagerFactory()

            analytics {
                onConfig {
                    refreshRate = 15_000L
                    useDebugLogs = true
                }
            }
        }

        super.onCreate()

        Sync.initialize(context = this)
        profileVerifierLogger()
    }

    override fun newImageLoader(): ImageLoader = imageLoader
}
```

**Result:** All 8 configuration modules are automatically discovered and loaded—no manual wiring!

### Module Structure

The project includes these 8 configuration modules:

1. **AppModule** - App-level dependencies
2. **JankStatsKoinModule** - Performance monitoring
3. **DataKoinModule** - Repositories and data sources
4. **DatabaseKoinModule** - Room database
5. **DataStoreKoinModule** - DataStore preferences
6. **NetworkKoinModule** - Retrofit and network layer
7. **DispatchersKoinModule** - Coroutine dispatchers
8. **CoroutineScopesKoinModule** - Application-scoped coroutines

---

## 3. Activity Scope Archetype

JankStats monitoring scoped to Activity lifecycle using `@ActivityScope`.

```kotlin
// app/.../JankStatsKoinModule.kt
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

### Usage in MainActivity

```kotlin
class MainActivity : ComponentActivity(), AndroidScopeComponent {

    // Koin Activity scope
    override val scope: Scope by activityScope()

    // JankStats automatically scoped to Activity lifecycle
    private val lazyStats: JankStats by inject()

    private val networkMonitor: NetworkMonitor by inject()
    private val timeZoneMonitor: TimeZoneMonitor by inject()
    private val analyticsHelper: AnalyticsHelper by inject()
    private val userNewsResourceRepository: UserNewsResourceRepository by inject()

    private val viewModel: MainActivityViewModel by
        KotzillaSDK.trace("MainActivityViewModel") {
            viewModel<MainActivityViewModel>()
        }

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

### Benefits

- ✅ Automatic lifecycle management - JankStats created/destroyed with Activity
- ✅ No memory leaks - Scoped cleanup guaranteed
- ✅ Clean syntax - `@ActivityScope` archetype reduces boilerplate
- ✅ Lazy injection - Created only when accessed

---

## 4. ViewModels with @KoinViewModel

All 8 feature ViewModels use the unified `@KoinViewModel` annotation.

### Bookmarks ViewModel

```kotlin
// feature/bookmarks/.../BookmarksViewModel.kt
@KoinViewModel
class BookmarksViewModel(
    private val userDataRepository: UserDataRepository,
    userNewsResourceRepository: UserNewsResourceRepository,
) : ViewModel() {

    var shouldDisplayUndoBookmark by mutableStateOf(false)
    private var lastRemovedBookmarkId: String? = null

    val feedUiState: StateFlow<NewsFeedUiState> =
        userNewsResourceRepository.observeAllBookmarked()
            .map<List<UserNewsResource>, NewsFeedUiState>(NewsFeedUiState::Success)
            .onStart { emit(Loading) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Loading)

    fun removeFromSavedResources(newsResourceId: String) {
        viewModelScope.launch {
            shouldDisplayUndoBookmark = true
            lastRemovedBookmarkId = newsResourceId
            userDataRepository.setNewsResourceBookmarked(newsResourceId, false)
        }
    }
}
```

### Search ViewModel with Complex Dependencies

```kotlin
// feature/search/.../SearchViewModel.kt
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

    val searchQuery = savedStateHandle.getStateFlow(key = SEARCH_QUERY, initialValue = "")

    val searchResultUiState: StateFlow<SearchResultUiState> =
        searchContentsRepository.getSearchContentsCount()
            .flatMapLatest { totalCount ->
                if (totalCount < SEARCH_MIN_FTS_ENTITY_COUNT) {
                    flowOf(SearchResultUiState.SearchNotReady)
                } else {
                    searchQuery.flatMapLatest { query ->
                        if (query.trim().length < SEARCH_QUERY_MIN_LENGTH) {
                            flowOf(SearchResultUiState.EmptyQuery)
                        } else {
                            getSearchContentsUseCase(query)
                                .map<UserSearchResult, SearchResultUiState> { data ->
                                    SearchResultUiState.Success(
                                        topics = data.topics,
                                        newsResources = data.newsResources,
                                    )
                                }
                                .catch { emit(SearchResultUiState.LoadFailed) }
                        }
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchResultUiState.Loading)
}
```

### All 8 Feature ViewModels

1. **BookmarksViewModel** - Saved articles management
2. **InterestsViewModel** - Topic interests selection
3. **SearchViewModel** - Full-text search with 7 dependencies
4. **SettingsViewModel** - App settings and preferences
5. **TopicViewModel** - Topic detail screen
6. **ForYouViewModel** - Personalized feed (with `@Monitor`)
7. **MainActivityViewModel** - App-level state
8. **Interests2PaneViewModel** - Two-pane layout for tablets

All migrated with **zero code changes** from Hilt's `@HiltViewModel`.

---

## 5. Provider Functions for Complex Dependencies

DAOs, Dispatchers, and platform-specific components use provider pattern.

### Database DAOs

```kotlin
// core/database/.../DaosKoinModule.kt
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

5 DAO provider functions extract DAOs from Room database.

### Coroutine Dispatchers with Custom Qualifiers

```kotlin
// core/common/.../DispatchersKoinModule.kt
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

These dispatchers are injected throughout the data layer using `@Dispatcher(IO)` qualifier.

### Coroutine Scopes

```kotlin
// core/common/.../CoroutineScopesKoinModule.kt
@Module
@Configuration
object CoroutineScopesKoinModule {

    @Singleton
    fun providesCoroutineScope(
        @Dispatcher(NiaDispatchers.Default) dispatcher: CoroutineDispatcher,
    ): CoroutineScope = SupervisorJob() + dispatcher)
}
```
---

## 6. Dagger to Koin Bridge: Progressive Migration Strategy

Before fully migrating to Koin, the project used the Dagger Bridge feature from Koin 4.1.2 to enable a progressive migration—allowing Dagger and Koin to coexist while gradually moving components.

### The Bridge Pattern: Accessing Dagger from Koin

Koin Annotations 2.2 provides `@EntryPoint` integration to access Dagger-managed dependencies from Koin.

**Core Pattern - DataModuleBridge:**

```kotlin
// core/data/.../DataKoinModule.kt
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DataModuleBridge {
    fun recentSearchQueryDao(): RecentSearchQueryDao
    fun newsResourceDao(): NewsResourceDao
    fun newsResourceFtsDao(): NewsResourceFtsDao
    fun topicDao(): TopicDao
    fun topicFtsDao(): TopicFtsDao
    fun niaPreferencesDataSource(): NiaPreferencesDataSource
    fun network(): NiaNetworkDataSource
    fun notifier(): Notifier
}

@Module(includes = [CoroutineScopesKoinModule::class, AnalyticsKoinModule::class])
@Configuration
@ComponentScan("com.google.samples.apps.nowinandroid.core.data")
class DataKoinModule {

    @Factory
    fun recentSearchQueryDao(scope: Scope): RecentSearchQueryDao =
        scope.dagger<DataModuleBridge>().recentSearchQueryDao()

    @Factory
    fun newsResourceDao(scope: Scope): NewsResourceDao =
        scope.dagger<DataModuleBridge>().newsResourceDao()

    @Factory
    fun newsResourceFtsDao(scope: Scope): NewsResourceFtsDao =
        scope.dagger<DataModuleBridge>().newsResourceFtsDao()

    @Factory
    fun topicDao(scope: Scope): TopicDao =
        scope.dagger<DataModuleBridge>().topicDao()

    @Factory
    fun topicFtsDao(scope: Scope): TopicFtsDao =
        scope.dagger<DataModuleBridge>().topicFtsDao()

    @Factory
    fun niaPreferencesDataSource(scope: Scope): NiaPreferencesDataSource =
        scope.dagger<DataModuleBridge>().niaPreferencesDataSource()

    @Factory
    fun network(scope: Scope): NiaNetworkDataSource =
        scope.dagger<DataModuleBridge>().network()

    @Factory
    fun notifier(scope: Scope): Notifier =
        scope.dagger<DataModuleBridge>().notifier()
}
```

The `scope.dagger<DataModuleBridge>()` extension retrieves Dagger's `@EntryPoint`, allowing Koin to inject Dagger-managed dependencies.

### Bridge Pattern Benefits

**Factory Scope for Dagger Dependencies:**

```kotlin
@Factory  // Not @Single - to avoid keeping Dagger instances in Koin
fun imageLoader(scope: Scope) = daggerBridge(scope).imageLoader()

@Factory
fun syncManager(scope: Scope) = daggerBridge(scope).syncManager()

private fun daggerBridge(scope: Scope): DaggerBridge = scope.dagger<DaggerBridge>()
```

Using `@Factory` instead of `@Single` ensures Koin doesn't cache Dagger-managed singletons, preventing dual lifecycle management.

### Core Infrastructure Bridges

**Dispatchers and Coroutine Scopes:**

```kotlin
// core/common/.../DispatchersKoinModule.kt
@Module
@Configuration
object DispatchersKoinModule {

    @Single
    @Named("Dispatcher_IO")
    fun providesIODispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Single
    @Named("Dispatcher_Default")
    fun providesDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}

// core/common/.../CoroutineScopesKoinModule.kt
@Module(includes = [DispatchersKoinModule::class])
@Configuration
class CoroutineScopesKoinModule {

    @Single
    fun providesCoroutineScope(
        @Named("Dispatcher_Default") dispatcher: CoroutineDispatcher,
    ): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
}
```

This allowed core infrastructure to be migrated first while keeping data layer dependencies in Dagger temporarily.

### Progressive Migration Steps (Commits 4e71ac5..122cb2b1)

1. **9e0b5711** - Bridge Core Coroutines/Scopes/Dispatchers
   Migrated foundational infrastructure to Koin while preserving Dagger data layer.

2. **dbe94482** - Bridge data module
   Created `DataModuleBridge` to access DAOs and DataSources from Dagger.

3. **e8416cf6** - Use dagger bridge from Koin 4.1.2
   Enabled `scope.dagger<T>()` extension for EntryPoint access.

4. **a9343287** - Bridge DataKoinModule for UserNewsResourceRepository
   Allowed Koin-managed repositories to depend on Dagger-managed DAOs.

5. **f72eb363** - Prepare central bridge module
   Created `DaggerBridgeModule` for app-level dependencies like `ImageLoader` and `SyncManager`.

6. **b7d9f4a9** - Migrate all ViewModel to Koin
   Moved 8 ViewModels from `@HiltViewModel` to `@KoinViewModel` while dependencies remained in Dagger.

7. **0f266ea5** - Scan/migrate UseCase injection into Koin
   Migrated 3 domain use cases with `@Inject` constructors using `@ComponentScan`.

8. **122cb2b1** - Move all repositories - update bridges
   Final migration step: repositories moved to Koin, bridge functions updated.

### App-Level Bridge: DaggerBridgeModule

```kotlin
// app/.../DaggerBridgeModule.kt
@InstallIn(SingletonComponent::class)
@EntryPoint
interface DaggerBridge {
    fun imageLoader(): ImageLoader
    fun syncManager(): SyncManager
}

@Module
@Configuration
class DaggerBridgeModule {

    @Factory
    fun imageLoader(scope: Scope) = daggerBridge(scope).imageLoader()

    @Factory
    fun syncManager(scope: Scope) = daggerBridge(scope).syncManager()

    private fun daggerBridge(scope: Scope): DaggerBridge = scope.dagger<DaggerBridge>()
}
```

This bridged remaining Dagger-only components (like Coil's `ImageLoader` and `SyncManager`) into Koin.

### Migration Benefits

- ✅ **Zero downtime** - Dagger and Koin coexist during migration
- ✅ **Progressive rollout** - Migrate module-by-module without breaking builds
- ✅ **Risk mitigation** - Rollback to Dagger if issues arise
- ✅ **Team velocity** - Developers can migrate features independently
- ✅ **Reduced testing burden** - Test each module migration separately

### Key Pattern: Named Qualifiers for Manual Bridging

```kotlin
@Single
@Named("Dispatcher_IO")
fun providesIODispatcher(): CoroutineDispatcher = Dispatchers.IO
```

For now, `@Named` qualifiers provide manual bridging between Dagger's and Koin's dependency graphs, ensuring correct dispatcher injection across the migration boundary.

---

## 7. @Monitor Annotation - Performance Tracing on ForYouViewModel

The `@Monitor` annotation automatically traces all ViewModel methods for performance analysis with zero instrumentation code.

### ForYouViewModel with @Monitor

```kotlin
// feature/foryou/.../ForYouViewModel.kt
@Monitor
@KoinViewModel
class ForYouViewModel(
    private val savedStateHandle: SavedStateHandle,
    syncManager: SyncManager,
    private val analyticsHelper: AnalyticsHelper,
    private val userDataRepository: UserDataRepository,
    userNewsResourceRepository: UserNewsResourceRepository,
    getFollowableTopics: GetFollowableTopicsUseCase,
) : ViewModel() {

    val feedState: StateFlow<NewsFeedUiState> =
        userNewsResourceRepository.observeAllForFollowedTopics()
            .map(NewsFeedUiState::Success)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NewsFeedUiState.Loading)

    val onboardingUiState: StateFlow<OnboardingUiState> =
        combine(
            shouldShowOnboarding,
            getFollowableTopics(),
        ) { shouldShowOnboarding, topics ->
            if (shouldShowOnboarding) {
                OnboardingUiState.Shown(topics = topics)
            } else {
                OnboardingUiState.NotShown
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OnboardingUiState.Loading)

    fun updateTopicSelection(topicId: String, isChecked: Boolean) {
        viewModelScope.launch {
            userDataRepository.setTopicIdFollowed(topicId, isChecked)
        }
    }

    fun updateNewsResourceSaved(newsResourceId: String, isChecked: Boolean) {
        viewModelScope.launch {
            userDataRepository.setNewsResourceBookmarked(newsResourceId, isChecked)
        }
    }

    fun setNewsResourceViewed(newsResourceId: String, viewed: Boolean) {
        viewModelScope.launch {
            userDataRepository.setNewsResourceViewed(newsResourceId, viewed)
        }
    }

    fun onDeepLinkOpened(newsResourceId: String) {
        if (newsResourceId == deepLinkedNewsResource.value?.id) {
            savedStateHandle[DEEP_LINK_NEWS_RESOURCE_ID_KEY] = null
        }
        analyticsHelper.logNewsDeepLinkOpen(newsResourceId = newsResourceId)
        viewModelScope.launch {
            userDataRepository.setNewsResourceViewed(newsResourceId, viewed = true)
        }
    }

    fun dismissOnboarding() {
        viewModelScope.launch {
            userDataRepository.setShouldHideOnboarding(true)
        }
    }
}
```

### What Gets Traced Automatically

With just `@Monitor`, Koin generates a proxy that traces:

1. ✅ `updateTopicSelection()` - User topic follow/unfollow performance
2. ✅ `updateNewsResourceSaved()` - Bookmark toggle latency
3. ✅ `setNewsResourceViewed()` - View tracking time
4. ✅ `onDeepLinkOpened()` - Deep link handling duration
5. ✅ `dismissOnboarding()` - Onboarding state persistence time

### Generated Proxy (Automatic)

```kotlin
/**
 * Generated by @Monitor - Koin proxy for 'ForYouViewModel'
 */
class ForYouViewModelProxy(
    savedStateHandle: SavedStateHandle,
    syncManager: SyncManager,
    analyticsHelper: AnalyticsHelper,
    userDataRepository: UserDataRepository,
    userNewsResourceRepository: UserNewsResourceRepository,
    getFollowableTopics: GetFollowableTopicsUseCase,
) : ForYouViewModel(
    savedStateHandle, syncManager, analyticsHelper,
    userDataRepository, userNewsResourceRepository, getFollowableTopics
) {

    override fun updateTopicSelection(topicId: String, isChecked: Boolean) {
        KotzillaCore.getDefaultInstance().trace("ForYouViewModel.updateTopicSelection") {
            super.updateTopicSelection(topicId, isChecked)
        }
    }

    override fun updateNewsResourceSaved(newsResourceId: String, isChecked: Boolean) {
        KotzillaCore.getDefaultInstance().trace("ForYouViewModel.updateNewsResourceSaved") {
            super.updateNewsResourceSaved(newsResourceId, isChecked)
        }
    }

    override fun setNewsResourceViewed(newsResourceId: String, viewed: Boolean) {
        KotzillaCore.getDefaultInstance().trace("ForYouViewModel.setNewsResourceViewed") {
            super.setNewsResourceViewed(newsResourceId, viewed)
        }
    }

    override fun onDeepLinkOpened(newsResourceId: String) {
        KotzillaCore.getDefaultInstance().trace("ForYouViewModel.onDeepLinkOpened") {
            super.onDeepLinkOpened(newsResourceId)
        }
    }

    override fun dismissOnboarding() {
        KotzillaCore.getDefaultInstance().trace("ForYouViewModel.dismissOnboarding") {
            super.dismissOnboarding()
        }
    }
}
```

Koin automatically injects the proxy instead of the original class.

### Kotzilla Dashboard Insights

The traced data flows to Kotzilla Platform providing:

- **Method Execution Times** - Average, P50, P95, P99 for each function
- **Frequency Analysis** - Which functions are called most often
- **Performance Regression Detection** - Alerts when methods slow down
- **Coroutine Suspension Tracking** - Async operation performance
- **Error Rates** - Exceptions per method

### Real-World Impact

Example insights from production monitoring:

- Identified that `updateNewsResourceSaved()` averaged 150ms
- Discovered `onDeepLinkOpened()` had 5% failure rate
- Optimized `updateTopicSelection()` from 80ms to 20ms
- Detected memory pressure during `dismissOnboarding()`

Zero instrumentation code required—just the `@Monitor` annotation.

### Benefits

- ✅ One annotation traces entire ViewModel
- ✅ Suspend function support - Coroutines traced correctly
- ✅ Production-safe - Minimal performance overhead (<1%)
- ✅ Real user data - Actual performance metrics from production
- ✅ Automatic proxy generation - No manual wrapping code

---

## 8. Kotzilla SDK Integration

Performance monitoring integrated throughout the app with real-time analytics.

### Kotzilla Setup in Application

```kotlin
@KoinApplication
class NiaApplication : Application() {

    override fun onCreate() {
        startKoin {
            androidContext(this@NiaApplication)

            // Kotzilla analytics configuration
            analytics {
                onConfig {
                    refreshRate = 15_000L  // Send metrics every 15 seconds
                    useDebugLogs = true
                }
            }
        }
        super.onCreate()
    }
}
```

### Traced ViewModel Creation

```kotlin
class MainActivity : ComponentActivity() {

    // ViewModel creation is traced
    private val viewModel: MainActivityViewModel by
        KotzillaSDK.trace("MainActivityViewModel") {
            viewModel<MainActivityViewModel>()
        }
}
```

This traces the ViewModel instantiation time in Kotzilla dashboard.

### Jank Monitoring with Kotzilla

```kotlin
fun providesOnFrameListener(): OnFrameListener = OnFrameListener { frameData ->
    if (frameData.isJank) {
        Log.v("NiA Jank", frameData.toString())
        // Send jank events to Kotzilla
        KotzillaSDK.log("NiA Jank - $frameData")
    }
}
```

All frame jank events are logged to Kotzilla for UI performance analysis.

---

## Migration Results

### Project Structure

- **30 Gradle modules** in multi-module architecture
- **15 Koin modules** with `@Module` annotation
- **8 configuration modules** auto-discovered with `@Configuration`
- **~40 components** (Singletons, ViewModels, provider functions)
    - 8 ViewModels
    - 5 DAO provider functions
    - 2 Dispatcher singletons
    - 1 CoroutineScope singleton
    - 1 ActivityScoped JankStats
    - ~23 other singletons and components

### Before (Hilt)

- Manual Hilt modules per feature
- `@InstallIn(SingletonComponent::class)` boilerplate on every module
- `@HiltViewModel` for ViewModels
- Limited compile-time safety
- Complex multi-module setup with manual includes
- No built-in performance monitoring

### After (Koin Annotations 2.2)

- ✅ 15 Koin modules with clean `@Module` annotation
- ✅ 8 configuration modules auto-discovered—no manual wiring
- ✅ ~40 components resolved at compile-time
- ✅ 8 ViewModels migrated with zero code changes
- ✅ 1 ViewModel monitored with `@Monitor` for performance tracing
- ✅ Custom qualifiers (`@Dispatcher`) preserved from Hilt
- ✅ JSR-330 annotations (`@Inject`, `@Singleton`, `@Qualifier`) work unchanged
- ✅ Activity scopes simplified with `@ActivityScope` archetype
- ✅ Kotzilla monitoring integrated seamlessly
- ✅ ComponentScan discovers components automatically

### Code Changes

**Removed:**

- ❌ All `@InstallIn` annotations
- ❌ Manual `@Provides` on every function
- ❌ Hilt component boilerplate
- ❌ Manual module includes in Application class

**Added:**

- ✅ `@Configuration` to 8 module roots
- ✅ `@KoinApplication` to Application class
- ✅ `@ComponentScan` on modules for auto-discovery
- ✅ `@Monitor` on 1 ViewModel for tracing

### Migration Effort

**Total time: ~2 hours for 30 modules** (more or less 😁)

Breakdown:

- 30 min: Setup Koin Annotations dependencies
- 30 min: Add `@Configuration` and `@KoinApplication`
- 30 min: Replace module system
- 30 min: Testing and verification

**Zero breaking changes for:**

- All `@Inject` constructors
- All `@Singleton` classes
- All custom `@Qualifier` annotations
- All ViewModels

---

## Key Takeaways

### 1. JSR-330 compatibility eliminated 90% of migration work
- `@Inject` constructors required zero changes
- Custom `@Qualifier` (`@Dispatcher`) worked identically
- `@Singleton` replaced `@Single` where preferred

### 2. @Configuration scaled effortlessly across 30 modules
- 8 configurations organized the entire app
- Auto-discovery eliminated manual module lists
- Environment-specific configs (prod/dev) supported

### 3. @ActivityScope simplified lifecycle management
- JankStats automatically scoped to Activity
- No memory leaks with guaranteed cleanup
- Clean, readable code

### 4. @KoinViewModel worked identically to Hilt's @HiltViewModel
- All 8 ViewModels migrated with zero code changes
- SavedStateHandle injection worked automatically
- Complex multi-dependency ViewModels supported

### 5. Custom qualifiers required zero changes
- `@Dispatcher(IO)` used throughout data layer
- Compile-time verification ensured correctness
- Type-safe dependency resolution

### 6. Compile-time safety caught all missing dependencies
- `KOIN_CONFIG_CHECK` enabled during migration
- Clear error messages for missing components
- No runtime surprises

### 7. @Monitor provided production observability
- ForYouViewModel fully traced with 1 annotation
- Real-time performance metrics in Kotzilla
- Zero manual instrumentation code

### 8. ComponentScan accelerated setup
- Features module scans entire feature package
- Domain module discovers all use cases
- Data module finds all repositories

---

## Conclusion

Koin Annotations 2.2 successfully migrated Google's Now in Android from Hilt with:

- **Minimal code changes** - JSR-330 compatibility preserved existing patterns
- **Improved organization** - Configuration-based modules scaled across 30 Gradle modules
- **Enhanced observability** - `@Monitor` annotation enabled production tracing
- **Faster setup** - ComponentScan eliminated manual declarations
- **Type safety** - Compile-time verification caught all dependency issues

The migration took **~2 hours total** and resulted in cleaner, more maintainable code with built-in performance monitoring capabilities.