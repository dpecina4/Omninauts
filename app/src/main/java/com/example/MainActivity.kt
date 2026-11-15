package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.onGoogleSignInClicked
import com.example.auth.signOutCommander
import com.example.data.model.ElementType
import com.example.data.repository.GameProgressionRepository
import com.example.data.repository.OfflineProgressionStorage
import com.example.game.SectorSpec
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BaseHubScreen
import com.example.ui.screens.ExpeditionScreen
import com.example.ui.theme.CavernautsTheme
import com.example.ui.viewmodel.BaseProgressionViewModel
import com.example.ui.viewmodel.BaseUiState
import com.example.ui.viewmodel.ExpeditionViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
    )
    setContent { CavernautsTheme { CavernautsRoot() } }
  }
}

private data class ActiveExpeditionLaunch(
  val launchKey: String,
  val sector: SectorSpec,
  val deployedElements: Set<ElementType>,
  val initialCounts: Map<ElementType, Int>,
)

@Composable
fun CavernautsRoot() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val offlineStorage = remember(context) { OfflineProgressionStorage(context) }

  val auth = Firebase.auth
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  var isOfflineMode by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    if (auth.currentUser == null) {
      attemptAutoSignIn(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = { isOfflineMode = false },
        onUnauthenticated = {},
        scope = scope,
      )
    }
  }

  val user = currentUser
  if (user == null && !isOfflineMode) {
    AuthScreen(
      onAuthSuccess = { isOfflineMode = false },
      onPlayOffline = { isOfflineMode = true },
    )
  } else {
    val isOffline = user == null || isOfflineMode
    val uid = user?.uid ?: "offline_commander"
    val callsign = user?.displayName ?: user?.email?.substringBefore("@") ?: "Offline Commander"
    key(uid, isOffline) {
      val baseViewModel: BaseProgressionViewModel =
        viewModel(
          key = "$uid-$isOffline",
          factory =
            viewModelFactory {
              initializer {
                val app =
                  checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                  }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                BaseProgressionViewModel(
                  repository =
                    GameProgressionRepository(
                      db = db,
                      auth = Firebase.auth,
                      offlineStorage = offlineStorage,
                      isOfflineMode = isOffline,
                    ),
                  currentUserId = uid,
                  callsignHint = callsign,
                )
              }
            },
        )

      val baseState by baseViewModel.uiState.collectAsStateWithLifecycle()
      var activeLaunch by remember { mutableStateOf<ActiveExpeditionLaunch?>(null) }

      val launch = activeLaunch
      val readyState = baseState as? BaseUiState.Ready
      if (launch != null && readyState != null) {
        val expViewModel: ExpeditionViewModel =
          viewModel(
            key = launch.launchKey,
            factory =
              viewModelFactory {
                initializer {
                  ExpeditionViewModel(
                    sector = launch.sector,
                    deployedElements = launch.deployedElements,
                    leaderStates = readyState.leaders,
                    labStates = readyState.labs,
                    initialSquadCounts = launch.initialCounts,
                  )
                }
              },
          )
        ExpeditionScreen(
          viewModel = expViewModel,
          onAbortExpedition = { activeLaunch = null },
          onCommitAndExit = { outcome ->
            baseViewModel.commitExpeditionOutcome(outcome) { activeLaunch = null }
          },
        )
      } else {
        BaseHubScreen(
          viewModel = baseViewModel,
          isOffline = isOffline,
          onLaunchExpedition = { sector, elements, counts ->
            activeLaunch =
              ActiveExpeditionLaunch(
                launchKey = UUID.randomUUID().toString(),
                sector = sector,
                deployedElements = elements,
                initialCounts = counts,
              )
          },
          onSignOut = {
            activeLaunch = null
            if (isOffline) {
              isOfflineMode = false
            } else {
              signOutCommander(credentialManager, scope)
            }
          },
          onLinkGoogleCloud = {
            onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = {
                isOfflineMode = false
              },
              onAuthError = { /* handled */ },
              scope = scope,
            )
          },
        )
      }
    }
  }
}
