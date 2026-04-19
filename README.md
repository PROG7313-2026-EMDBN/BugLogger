# Building the BugLogger App

## Objective

In this guide, you will build and understand the BugLogger Android app using Jetpack Compose.

By the end of this guide, you should have an app that:

- uses Jetpack Compose for the user interface
- connects to a live REST API using Retrofit
- fetches and displays a list of bugs
- opens a detail screen for a selected bug
- allows the user to add a new bug
- uses ViewModels and a Repository to separate concerns
- targets API 25 and above

Repo to refer to:

```text
https://github.com/PROG7313-2026-EMDBN/BugLogger
```

## Overall Project Structure

This project follows a simple layered structure.

```text
app/src/main/java/com/prog7313/buglogger
│
├── data
│   ├── model
│   │   └── Bug.kt
│   ├── remote
│   │   ├── BugApiService.kt
│   │   └── RetrofitProvider.kt
│   └── repository
│       └── BugRepository.kt
│
├── ui
│   ├── navigation
│   │   └── AppNavGraph.kt
│   ├── screens
│   │   ├── AddBugScreen.kt
│   │   ├── BugDetailScreen.kt
│   │   └── BugListScreen.kt
│   └── theme
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
│
├── util
│   └── UiState.kt
│
├── viewmodel
│   ├── AddBugViewModel.kt
│   ├── BugDetailViewModel.kt
│   └── BugListViewModel.kt
│
└── MainActivity.kt
```

### What each part does

- `data/model` holds the data class used by the API
- `data/remote` handles the Retrofit API setup
- `data/repository` acts as the data middle layer
- `viewmodel` holds screen logic
- `ui/screens` contains the Compose screens
- `ui/navigation` controls movement between screens
- `util/UiState.kt` is used to represent loading, success and error states

## Step 1: Create the project

Create a new Android Studio project using:

- **Empty Activity**
- **Language:** Kotlin
- **Minimum SDK:** API 25

Use the package name:

```text
com.prog7313.buglogger
```

## Step 2: Add the required dependencies

This app uses Compose, Navigation, Retrofit and lifecycle ViewModels.

Update `app/build.gradle.kts` to include the required dependencies.

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.prog7313.buglogger"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.prog7313.buglogger"
        minSdk = 25
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation("androidx.navigation:navigation-compose:2.8.9")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```

This gives the project everything it needs for UI, navigation and API communication.

## Step 3: Add internet permission

Because the app calls a live API, it needs internet access.

Update `AndroidManifest.xml` to include the internet permission.

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

Your manifest should follow this structure:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.BugLogger">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

## Step 4: Create the Bug data model

The app works with bugs returned from the API.  
Create a `Bug` data class in `data/model/Bug.kt`.

```kotlin
package com.prog7313.buglogger.data.model

data class Bug(
    val id: Int = 0,
    val title: String? = "",
    val description: String? = "",
    val severity: String? = "",
    val reportedBy: String? = "",
    val createdAt: String = "",
    val isResolved: Boolean = false
)
```

This class is used for both reading bugs from the API and sending a new bug to the API.

## Step 5: Define the Retrofit API interface

Next, define the endpoints the app will call.  
Create `data/remote/BugApiService.kt`.

```kotlin
package com.prog7313.buglogger.data.remote

import com.prog7313.buglogger.data.model.Bug
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface BugApiService {
    @GET("bugs")
    suspend fun getBugs(): List<Bug>

    @GET("bugs/{id}")
    suspend fun getBugById(@Path("id") id: Int): Bug

    @POST("bugs")
    suspend fun createBug(@Body request: Bug): Bug
}
```

This interface tells Retrofit how to talk to the API - meaning what type of requests and to which endpoints.

## Step 6: Create the Retrofit provider

Now create the Retrofit instance that will be reused across the app.  
Add `data/remote/RetrofitProvider.kt`.

```kotlin
package com.prog7313.buglogger.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitProvider {
    private const val BASE_URL = "https://bugloggerapi.onrender.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: BugApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BugApiService::class.java)
    }
}
```

This object creates one Retrofit client that talks to the API at the `BASE_URL `, allows for fetching and deserialisation of the JSON data, and exposes the API service through `api`.

## Step 7: Add the repository

The repository keeps API logic out of the screens and ViewModels.  
Create `data/repository/BugRepository.kt`.

```kotlin
package com.prog7313.buglogger.data.repository

import com.prog7313.buglogger.data.model.Bug
import com.prog7313.buglogger.data.remote.RetrofitProvider

class BugRepository {
    private val api = RetrofitProvider.api

    suspend fun getBugs(): Result<List<Bug>> = runCatching {
        api.getBugs()
    }

    suspend fun getBugById(id: Int): Result<Bug> = runCatching {
        api.getBugById(id)
    }

    suspend fun createBug(request: Bug): Result<Bug> = runCatching {
        api.createBug(request)
    }
}
```

Using `Result` makes it easier to handle success and failure cleanly.

## Step 8: Add a shared UI state class

Each screen needs to know if data is loading, successful, idle, or failed.

Create `util/UiState.kt`.

```kotlin
package com.prog7313.buglogger.util

sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
```

This is used by the ViewModels to communicate with the UI.

## Step 9: Create the list ViewModel

The list screen needs to fetch all bugs and expose state to the UI.

Create `viewmodel/BugListViewModel.kt`.

```kotlin
package com.prog7313.buglogger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prog7313.buglogger.data.model.Bug
import com.prog7313.buglogger.data.repository.BugRepository
import com.prog7313.buglogger.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BugListViewModel : ViewModel() {
    private val repository = BugRepository()

    private val _uiState = MutableStateFlow<UiState<List<Bug>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Bug>>> = _uiState.asStateFlow()

    init {
        loadBugs()
    }

    fun loadBugs(forceRefresh: Boolean = false) {
        if (!forceRefresh && _uiState.value is UiState.Loading) return

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.getBugs()
                .onSuccess { _uiState.value = UiState.Success(it) }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Failed to load bugs") }
        }
    }
}
```

This ViewModel fetches and loads the bug list when created and can refresh it later when needed.

## Step 10: Create the detail ViewModel

The detail screen needs to fetch one bug based on its id.

Create `viewmodel/BugDetailViewModel.kt`.

```kotlin
package com.prog7313.buglogger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prog7313.buglogger.data.model.Bug
import com.prog7313.buglogger.data.repository.BugRepository
import com.prog7313.buglogger.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BugDetailViewModel : ViewModel() {
    private val repository = BugRepository()

    private val _uiState = MutableStateFlow<UiState<Bug>>(UiState.Idle)
    val uiState: StateFlow<UiState<Bug>> = _uiState.asStateFlow()

    fun loadBug(id: Int) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.getBugById(id)
                .onSuccess { _uiState.value = UiState.Success(it) }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Failed to load bug") }
        }
    }
}
```

This ViewModel is responsible for fetching and loading one selected bug.

## Step 11: Create the add ViewModel

The add screen needs to build a bug object and submit it to the API.

Create `viewmodel/AddBugViewModel.kt`.

```kotlin
package com.prog7313.buglogger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prog7313.buglogger.data.model.Bug
import com.prog7313.buglogger.data.repository.BugRepository
import com.prog7313.buglogger.util.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AddBugViewModel : ViewModel() {
    private val repository = BugRepository()

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val uiState: StateFlow<UiState<Unit>> = _uiState.asStateFlow()

    fun saveBug(
        title: String,
        description: String,
        severity: String,
        reportedBy: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            val request = Bug(
                title = title,
                description = description,
                severity = severity,
                reportedBy = reportedBy,
                createdAt = getDate(),
                isResolved = false
            )

            repository.createBug(request)
                .onSuccess {
                    _uiState.value = UiState.Success(Unit)
                    onSuccess()
                }
                .onFailure {
                    _uiState.value = UiState.Error(it.message ?: "Failed to save bug")
                }
        }
    }

    private fun getDate(): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date())
    }
}
```

This ViewModel is responsible for posting a new bug to the API. It keeps form submission logic out of the UI layer

## Step 12: Add navigation

Now connect the screens using a NavHost.  
Create `ui/navigation/AppNavGraph.kt`.

```kotlin
package com.prog7313.buglogger.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.prog7313.buglogger.ui.screen.BugListScreen
import com.prog7313.buglogger.ui.screens.AddBugScreen
import com.prog7313.buglogger.ui.screens.BugDetailScreen
import com.prog7313.buglogger.viewmodel.AddBugViewModel
import com.prog7313.buglogger.viewmodel.BugDetailViewModel
import com.prog7313.buglogger.viewmodel.BugListViewModel

object Routes {
    const val BUG_LIST = "bug_list"
    const val ADD_BUG = "add_bug"
    const val BUG_DETAIL = "bug_detail"
}

@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.BUG_LIST,
        modifier = modifier
    ) {
        composable(Routes.BUG_LIST) {
            val viewModel: BugListViewModel = viewModel()
            BugListScreen(
                viewModel = viewModel,
                onAddClick = { navController.navigate(Routes.ADD_BUG) },
                onBugClick = { id -> navController.navigate("${Routes.BUG_DETAIL}/$id") }
            )
        }

        composable(Routes.ADD_BUG) {
            val viewModel: AddBugViewModel = viewModel()
            AddBugScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = "${Routes.BUG_DETAIL}/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            val viewModel: BugDetailViewModel = viewModel()

            BugDetailScreen(
                bugId = id,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
```

This class handles navigation between the list, add, and detail screens.

## Step 13: Create the bug list screen
The list screen shows all bugs returned from the API and refreshes when it becomes active again.

Create `ui/screens/BugListScreen.kt`.

```kotlin
package com.prog7313.buglogger.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.prog7313.buglogger.data.model.Bug
import com.prog7313.buglogger.util.UiState
import com.prog7313.buglogger.viewmodel.BugListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugListScreen(
    viewModel: BugListViewModel,
    onAddClick: () -> Unit,
    onBugClick: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.loadBugs(forceRefresh = true)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Bug Logger") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Text("+")
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = state.message)
                }
            }

            is UiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.data) { bug ->
                        BugCard(
                            bug = bug,
                            onClick = { onBugClick(bug.id) }
                        )
                    }
                }
            }

            else -> Unit
        }
    }
}

@Composable
private fun BugCard(
    bug: Bug,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = bug.title.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(text = bug.severity.orEmpty())
            }

            Text(
                text = bug.description.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp)
            )

            Text(
                text = "Reported by: ${bug.reportedBy.orEmpty()}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = if (bug.isResolved) "Resolved" else "Open",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
```

This screen displays the list and handles loading and error states.

## Step 14: Create the detail screen

The detail screen loads and displays a single bug.

Create `ui/screens/BugDetailScreen.kt`.

```kotlin
package com.prog7313.buglogger.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prog7313.buglogger.util.UiState
import com.prog7313.buglogger.viewmodel.BugDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugDetailScreen(
    bugId: Int,
    viewModel: BugDetailViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(bugId) {
        viewModel.loadBug(bugId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bug Detail") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.message)
                }
            }

            is UiState.Success -> {
                val bug = state.data

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = bug.title.orEmpty(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    DetailRow("Description", bug.description.orEmpty())
                    DetailRow("Severity", bug.severity.orEmpty())
                    DetailRow("Reported By", bug.reportedBy.orEmpty())
                    DetailRow("Created At", bug.createdAt)
                    DetailRow("Status", if (bug.isResolved) "Resolved" else "Open")
                    DetailRow("Bug ID", bug.id.toString())
                }
            }

            else -> Unit
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
```

This screen is driven by the selected bug id passed through navigation.

## Step 15: Create the add screen

The add screen collects user input and passes it to the ViewModel.

Create `ui/screens/AddBugScreen.kt`.

```kotlin
package com.prog7313.buglogger.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prog7313.buglogger.util.UiState
import com.prog7313.buglogger.viewmodel.AddBugViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBugScreen(
    viewModel: AddBugViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("") }
    var reportedBy by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Bug") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = severity,
                onValueChange = { severity = it },
                label = { Text("Severity") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = reportedBy,
                onValueChange = { reportedBy = it },
                label = { Text("Reported By") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (title.isNotBlank() && description.isNotBlank()) {
                        viewModel.saveBug(
                            title = title,
                            description = description,
                            severity = severity,
                            reportedBy = reportedBy,
                            onSuccess = onSaved
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Bug")
            }

            when (val state = uiState) {
                is UiState.Loading -> {
                    CircularProgressIndicator()
                }

                is UiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                else -> Unit
            }
        }
    }
}
```

This screen holds the form fields and delegates saving to the ViewModel.

## Step 16: Set up MainActivity

The activity is the entry point of the app and loads the navigation graph.

Create `MainActivity.kt`.

```kotlin
package com.prog7313.buglogger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import com.prog7313.buglogger.ui.navigation.AppNavGraph
import com.prog7313.buglogger.ui.theme.BugLoggerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            BugLoggerTheme {
                Surface {
                    AppNavGraph()
                }
            }
        }
    }
}
```

This loads the app theme and starts navigation.

## Step 17: Run and test the app

Once all files are in place:

1. Sync Gradle
2. Build the project
3. Run the app on an emulator or device
4. Confirm that the list screen opens
5. Tap the `+` button and add a new bug
6. Return to the list and confirm it refreshes
7. Tap a bug to view its details

## In summary

The flow in this app when data is posted:
```text
Screen → ViewModel → Repository → Retrofit API → Server
```

And when data is retrieved:
```text
Server → Retrofit API → Repository → ViewModel → UiState → Screen
```