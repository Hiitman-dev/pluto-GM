package com.pluto.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pluto.core.designsystem.CosmicBackground
import com.pluto.core.designsystem.PLUTOButton
import com.pluto.core.designsystem.PLUTOColors
import com.pluto.core.designsystem.PLUTOOutlinedButton
import com.pluto.core.designsystem.PLUTOTypography
import com.pluto.core.designsystem.PlutoIcons

/**
 * AuthScreen — placeholder email/password auth UI.
 *
 * Per the spec: "Backend not yet implemented" — this screen does NOT
 * actually authenticate. It shows the UX for email login and a
 * "Continue as Guest" option, but the actual auth backend is a TODO
 * (the spec says: do not fake features).
 *
 * When the user taps "Continue", we call [onAuthenticated] so the nav
 * graph routes to Home. When they tap "Continue as Guest", same thing.
 *
 * In a future version, this screen will:
 *   - Validate email + password
 *   - Call the real auth backend
 *   - Show loading / error states
 *   - Persist token via DataStore
 */
@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        CosmicBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back button
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(50))
                        .background(PLUTOColors.Glass2)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = PlutoIcons.Back,
                        contentDescription = "Back",
                        tint = PLUTOColors.FrostWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = "Welcome to PLUTO",
                style = PLUTOTypography.displayMedium,
                color = PLUTOColors.FrostWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 48.dp)
            )
            Text(
                text = "Sign in to sync your watchlist across devices.\nOr continue as a guest — your data stays on this device.",
                style = PLUTOTypography.bodyMedium,
                color = PLUTOColors.MutedStar,
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = PLUTOColors.FrostWhite,
                    unfocusedTextColor = PLUTOColors.FrostWhite,
                    focusedContainerColor = PLUTOColors.Glass2,
                    unfocusedContainerColor = PLUTOColors.Glass2,
                    focusedIndicatorColor = PLUTOColors.GlowBlue,
                    unfocusedIndicatorColor = PLUTOColors.GlassBorder,
                    focusedLabelColor = PLUTOColors.IceBlue,
                    unfocusedLabelColor = PLUTOColors.MutedStar,
                    cursorColor = PLUTOColors.GlowBlue
                )
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = PLUTOColors.FrostWhite,
                    unfocusedTextColor = PLUTOColors.FrostWhite,
                    focusedContainerColor = PLUTOColors.Glass2,
                    unfocusedContainerColor = PLUTOColors.Glass2,
                    focusedIndicatorColor = PLUTOColors.GlowBlue,
                    unfocusedIndicatorColor = PLUTOColors.GlassBorder,
                    focusedLabelColor = PLUTOColors.IceBlue,
                    unfocusedLabelColor = PLUTOColors.MutedStar,
                    cursorColor = PLUTOColors.GlowBlue
                )
            )

            PLUTOButton(
                text = "Sign In",
                onClick = onAuthenticated,
                modifier = Modifier.fillMaxWidth(),
                icon = PlutoIcons.Play
            )

            PLUTOOutlinedButton(
                text = "Continue as Guest",
                onClick = onAuthenticated,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Auth backend is not yet implemented.\nBoth options take you to Home.",
                style = PLUTOTypography.bodySmall,
                color = PLUTOColors.MutedStar,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
