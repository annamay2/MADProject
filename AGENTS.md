# AGENTS.md

## Project Overview

Android (Kotlin) receipt-tracking app for gluten-free purchases using Jetpack Compose,
Firebase (Auth + Firestore), Room, and WorkManager. Single-module Gradle project.

- **Min SDK**: 24 | **Target/Compile SDK**: 36 | **JVM Target**: 17
- **Kotlin**: 2.0.21 | **AGP**: 8.13.2 | **Gradle**: 8.13 (Kotlin DSL + Version Catalog)
- **Namespace**: `com.example.madprojectactivity`

## Build & Test Commands

```bash
# Build
./gradlew assembleDebug            # Debug APK
./gradlew assembleRelease          # Release APK
./gradlew build                    # Full build (compile + lint + test)

# Unit tests (JVM, no device required)
./gradlew test                     # Run all unit tests
./gradlew testDebugUnitTest        # Debug variant only
./gradlew test --tests "com.example.madprojectactivity.ExampleUnitTest"              # Single test class
./gradlew test --tests "com.example.madprojectactivity.ExampleUnitTest.addition_isCorrect"  # Single test method

# Instrumented tests (requires emulator or device)
./gradlew connectedAndroidTest     # Run all instrumented tests
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.madprojectactivity.ExampleInstrumentedTest  # Single class

# Lint
./gradlew lint                     # Android lint (no ktlint/detekt configured)
./gradlew lintDebug                # Lint debug variant only

# Clean
./gradlew clean
```

Test locations:
- Unit: `app/src/test/java/com/example/madprojectactivity/`
- Instrumented: `app/src/androidTest/java/com/example/madprojectactivity/`
- Runner: `androidx.test.runner.AndroidJUnitRunner` | JUnit 4, Espresso, Compose UI testing

## Architecture

**MVVM with UiState pattern** -- each feature has a Screen (Composable), ViewModel, and UiState data class.
Package structure: `screens/{feature}/` contains `[Feature]Screen.kt`, `[Feature]ViewModel.kt`, `[Feature]UiState.kt`.
Data layer: `data/local/` (Room), `data/model/` (entities), `data/repository/`, `data/worker/` (WorkManager sync).

**Offline-first**: Receipts save to Room first, then sync to Firestore via WorkManager.
**No dependency injection framework** -- ViewModels instantiate dependencies directly.

## Code Style Guidelines

### Kotlin Style

- **Indentation**: 4 spaces, no tabs
- **Kotlin code style**: `official` (set in `gradle.properties`)
- **Trailing commas**: Not used
- **Explicit `public` modifier**: Omitted (default visibility is public)
- **`internal` modifier**: Not used
- **`private`**: Used for ViewModel backing fields, Firebase/Room instances, and internal methods
- **No `lateinit` or `lazy`** -- direct initialization preferred

### Naming Conventions

| Element             | Convention    | Example                                       |
|---------------------|---------------|-----------------------------------------------|
| Classes             | PascalCase    | `HomeViewModel`, `ReceiptEntity`              |
| Screens             | `[Feature]Screen` | `LoginScreen`, `ProfileScreen`            |
| ViewModels          | `[Feature]ViewModel` | `HomeViewModel`, `LoginViewModel`      |
| UI state classes    | `[Feature]UiState` | `LoginUiState`, `HomeUiState`            |
| Functions           | camelCase     | `saveCeliacSpend`, `observeLocalReceipts`     |
| Event handlers      | `on[Event]Change` | `onEmailChange`, `onAmountChange`         |
| Variables           | camelCase     | `receiptDao`, `workManager`                   |
| Backing StateFlow   | `_uiState`    | `private val _uiState = MutableStateFlow(...)` |
| Exposed StateFlow   | `uiState`     | `val uiState: StateFlow<UiState> = _uiState`  |
| Colors (top-level)  | PascalCase    | `Purple80`, `PurpleGrey80`                    |
| Test methods        | snake_case    | `addition_isCorrect`                          |
| Nav routes          | camelCase strings | `"home"`, `"uploadReceipt"`, `"viewReceipt/{receiptId}"` |

### Imports

- **Wildcard imports** for Compose packages: `androidx.compose.foundation.layout.*`,
  `androidx.compose.material3.*`, `androidx.compose.runtime.*`
- **Specific imports** for non-Compose packages: `kotlinx.coroutines.flow.MutableStateFlow`,
  `com.google.firebase.auth.FirebaseAuth`
- General order: Android SDK -> Third-party -> Project -> Kotlin stdlib -> Java stdlib
  (not strictly enforced)

### State Management

- ViewModels expose `StateFlow<UiState>` backed by `private MutableStateFlow`
- State updates use `_uiState.update { it.copy(...) }` (atomic copy)
- Composables collect state via `val state by vm.uiState.collectAsState()`
- Ephemeral UI state uses `remember { mutableStateOf(...) }`
- Saved state uses `rememberSaveable { mutableStateOf(...) }`

### Error Handling

- **try/catch** in `viewModelScope.launch` blocks, updating `errorMessage: String?` in UiState
- Validation via early return with error message before launching coroutines
- No `Result<T>` or sealed error hierarchies -- errors are nullable strings in UiState
- Firebase callbacks use `addOnFailureListener` with `Log.e` in repository layer

### Composable Patterns

- `@Composable` on the line immediately above `fun`, no blank line between
- `@OptIn(ExperimentalMaterial3Api::class)` used at file-level or function-level
- First parameter: `modifier: Modifier = Modifier`
- ViewModel parameter with default: `vm: HomeViewModel = viewModel()`
- Navigation callbacks as trailing lambdas: `onBack: () -> Unit`, `onDone: () -> Unit = {}`
- Every screen uses `Scaffold` with `topBar`
- `LaunchedEffect` for one-time navigation side effects on state changes
- No `@Preview` functions are defined
- Strings are hardcoded (not using string resources)
- Currency formatted as `"€${"%.2f".format(value)}"`

### Coroutines

- All async work via `viewModelScope.launch { }` (Main dispatcher by default)
- Firebase Task -> suspend via `kotlinx.coroutines.tasks.await()`
- Room queries return `Flow<List<T>>` collected in `viewModelScope.launch`
- `CoroutineWorker` for background sync (`SyncWorker`)
- No explicit `Dispatchers.IO` / `withContext` usage

### Data Classes

- **Data classes** for: UiState holders, domain models, Room entities
- **Regular classes** for: ViewModels, Activities, Repositories, Workers, Database
- **Sealed class** for navigation routes (`Screen`)
- **Interface** for Room DAO (`ReceiptDao`)

### Room / Database

- `AppDatabase` uses double-checked locking singleton (`@Volatile` + `synchronized`)
- DAO methods are `suspend` functions (except Flow-returning queries)
- Entity uses `@Entity(tableName = "...")` with `@PrimaryKey` on UUID string field

### ViewModels

- Feature ViewModels needing `Application` context extend `AndroidViewModel(application)`
- `LoginViewModel` (Firebase-only) extends plain `ViewModel()`
- `onCleared()` used to remove auth listeners and Firestore snapshot listeners

## Known Issues / Gotchas

- `UploadReceiptScreen.kt` and `UploadReceiptViewModel.kt` declare package
  `com.example.madprojectactivity.screens.receipts` but live in `screens/uploadReceipt/`
- `google-services.json` is committed to the repo (contains Firebase API keys)
- `FirestoreTest.kt` in main source set is a debug Composable, not a proper test
- Some UiState classes are in separate files, others are inline in the ViewModel file
- Hardcoded colors (`Color(0xFF...)`) appear alongside theme colors
