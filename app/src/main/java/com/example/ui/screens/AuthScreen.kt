package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.onGoogleSignInClicked
import com.example.data.model.ElementType
import com.example.ui.theme.BioCyan
import com.example.ui.theme.CavernAlloy
import com.example.ui.theme.CavernBorder
import com.example.ui.theme.CavernObsidian
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.accentColor

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
  onPlayOffline: () -> Unit = {},
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  BoxWithConstraints(modifier = Modifier.fillMaxSize().background(CavernObsidian)) {
    val useSplitPane = maxWidth >= 600.dp && (maxHeight < 520.dp || maxWidth >= 720.dp)
    val isShortHeight = maxHeight < 460.dp
    val isNarrowWidth = maxWidth < 370.dp
    val heroBannerHeight = (maxHeight * 0.38f).coerceIn(140.dp, 360.dp)

    if (useSplitPane) {
      Row(
        modifier =
          Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(if (isShortHeight) 10.dp else 20.dp),
        horizontalArrangement = Arrangement.spacedBy(if (isShortHeight) 10.dp else 20.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier.weight(1f)
              .fillMaxHeight()
              .clip(RoundedCornerShape(20.dp))
              .border(1.dp, CavernBorder, RoundedCornerShape(20.dp)),
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_auth_cavern_hero),
            contentDescription = "Bioluminescent subterranean cavern illustration",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
          )
          Box(
            modifier =
              Modifier.fillMaxSize()
                .background(
                  Brush.verticalGradient(
                    0.0f to Color.Transparent,
                    0.65f to CavernObsidian.copy(alpha = 0.65f),
                    1.0f to CavernObsidian.copy(alpha = 0.92f),
                  )
                )
          )
          Column(modifier = Modifier.align(Alignment.BottomStart).padding(if (isShortHeight) 12.dp else 20.dp)) {
            Text(
              text = "SUBTERRANEAN EXPEDITION",
              style = MaterialTheme.typography.labelMedium,
              color = BioCyan,
            )
            Text(
              text = "OMNINAUTS",
              style = if (isShortHeight) MaterialTheme.typography.titleLarge else MaterialTheme.typography.displaySmall,
              color = MaterialTheme.colorScheme.onBackground,
            )
          }
        }

        Box(
          modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
          contentAlignment = Alignment.Center,
        ) {
          AuthCardContent(
            compact = isShortHeight || isNarrowWidth,
            omitTitleHeader = isShortHeight,
            isShortLandscape = isShortHeight,
            isLoading = isLoading,
            errorMessage = errorMessage,
            onPlayOffline = onPlayOffline,
            onSignInClick = {
              isLoading = true
              errorMessage = null
              onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                  isLoading = false
                  onAuthSuccess()
                },
                onAuthError = { msg ->
                  isLoading = false
                  errorMessage = msg
                },
                scope = scope,
                onAuthCancelled = { isLoading = false },
              )
            },
          )
        }
      }
    } else {
      Image(
        painter = painterResource(id = R.drawable.img_auth_cavern_hero),
        contentDescription = "Bioluminescent subterranean cavern illustration",
        modifier = Modifier.fillMaxWidth().height(heroBannerHeight + 48.dp),
        contentScale = ContentScale.Crop,
      )

      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(
              Brush.verticalGradient(
                0.0f to Color.Transparent,
                0.28f to CavernObsidian.copy(alpha = 0.72f),
                0.48f to CavernObsidian,
              )
            )
      )

      Column(
        modifier =
          Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (isShortHeight) Arrangement.Center else Arrangement.Bottom,
      ) {
        if (!isShortHeight) {
          Spacer(modifier = Modifier.height((heroBannerHeight - 50.dp).coerceAtLeast(32.dp)))
        }
        AuthCardContent(
          compact = isNarrowWidth || isShortHeight,
          omitTitleHeader = isShortHeight,
          isShortLandscape = isShortHeight,
          isLoading = isLoading,
          errorMessage = errorMessage,
          onPlayOffline = onPlayOffline,
          onSignInClick = {
            isLoading = true
            errorMessage = null
            onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = {
                isLoading = false
                onAuthSuccess()
              },
              onAuthError = { msg ->
                isLoading = false
                errorMessage = msg
              },
              scope = scope,
              onAuthCancelled = { isLoading = false },
            )
          },
        )
      }
    }
  }
}

@Composable
private fun AuthCardContent(
  compact: Boolean,
  omitTitleHeader: Boolean,
  isShortLandscape: Boolean = false,
  isLoading: Boolean,
  errorMessage: String?,
  onPlayOffline: () -> Unit,
  onSignInClick: () -> Unit,
) {
  Surface(
    color = CavernAlloy.copy(alpha = 0.9f),
    shape = RoundedCornerShape(20.dp),
    modifier =
      Modifier.widthIn(max = 520.dp)
        .fillMaxWidth()
        .border(1.dp, CavernBorder, RoundedCornerShape(20.dp)),
  ) {
    Column(
      modifier = Modifier.padding(if (compact) 10.dp else 22.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      if (!omitTitleHeader) {
        Text(
          text = "SUBTERRANEAN EXPEDITION",
          style = MaterialTheme.typography.labelMedium,
          color = BioCyan,
        )
        Spacer(modifier = Modifier.height(if (compact) 4.dp else 6.dp))
        Text(
          text = "OMNINAUTS",
          style = if (compact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall,
          color = MaterialTheme.colorScheme.onBackground,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(if (compact) 4.dp else 8.dp))
      }
      Text(
        text =
          if (omitTitleHeader) "Command Pyronauts, Hydronauts & Voltnauts across deep hazards."
          else "Command elemental Pyronauts, Hydronauts, and Voltnauts through procedural caverns, hatch field eggs, and upgrade base labs.",
        style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
        textAlign = TextAlign.Center,
      )

      if (!isShortLandscape) {
        Spacer(modifier = Modifier.height(if (compact) 6.dp else 16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
          ElementBadge(ElementType.PYRO, Icons.Filled.LocalFireDepartment, compact = compact)
          ElementBadge(ElementType.HYDRO, Icons.Filled.WaterDrop, compact = compact)
          ElementBadge(ElementType.VOLT, Icons.Filled.Bolt, compact = compact)
        }
      }

      Spacer(modifier = Modifier.height(if (compact) 8.dp else 20.dp))

      if (errorMessage != null) {
        Surface(
          color = DangerCrimson.copy(alpha = 0.16f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        ) {
          Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = DangerCrimson,
            modifier = Modifier.padding(8.dp),
            textAlign = TextAlign.Center,
          )
        }
      }

      if (isShortLandscape) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = onSignInClick,
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = BioCyan, contentColor = CavernObsidian),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).height(44.dp).testTag("google_sign_in_button"),
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = CavernObsidian,
                strokeWidth = 2.dp,
              )
            } else {
              Text(
                text = "Sign in",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
              )
            }
          }

          OutlinedButton(
            onClick = onPlayOffline,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).height(44.dp).testTag("play_offline_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.CloudOff,
              contentDescription = "Play offline without sign-in",
              tint = TextSecondary,
              modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
              text = "Play Offline",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      } else {
        Button(
          onClick = onSignInClick,
          enabled = !isLoading,
          colors = ButtonDefaults.buttonColors(containerColor = BioCyan, contentColor = CavernObsidian),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().height(if (compact) 46.dp else 52.dp).testTag("google_sign_in_button"),
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              color = CavernObsidian,
              strokeWidth = 2.dp,
            )
          } else {
            Text(
              text = "Sign in with Google",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
            )
          }
        }

        Spacer(modifier = Modifier.height(if (compact) 6.dp else 10.dp))

        OutlinedButton(
          onClick = onPlayOffline,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().height(if (compact) 44.dp else 48.dp).testTag("play_offline_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = "Play offline without sign-in",
            tint = TextSecondary,
            modifier = Modifier.size(16.dp),
          )
          Spacer(Modifier.width(6.dp))
          Text(
            text = "Play Offline (Local Save)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }

        if (!compact) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Offline-first progression enabled. Connect Google anytime to sync to the cloud.",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            textAlign = TextAlign.Center,
          )
        }
      }
    }
  }
}

@Composable
private fun ElementBadge(
  element: ElementType,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  compact: Boolean = false,
) {
  val accent = element.accentColor()
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
      modifier =
        Modifier.size(if (compact) 30.dp else 40.dp)
          .clip(CircleShape)
          .background(accent.copy(alpha = 0.18f))
          .border(1.dp, accent.copy(alpha = 0.7f), CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = element.displayName,
        tint = accent,
        modifier = Modifier.size(if (compact) 16.dp else 20.dp),
      )
    }
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = element.unitName,
      style = MaterialTheme.typography.labelSmall,
      color = accent,
    )
  }
}
