package com.praxis.app.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.praxis.app.BuildConfig
import com.praxis.app.ui.viewmodel.AuthState
import com.praxis.app.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

private const val TAG = "OnboardingScreen"

@Composable
fun OnboardingScreen(
    onGoogleSignIn: (userId: String, token: String, displayName: String) -> Unit,
    onManualContinue: (name: String, age: Int, bio: String) -> Unit,
    authViewModel: AuthViewModel = viewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var showFieldError by remember { mutableStateOf(false) }

    val credentialManager = remember { CredentialManager.create(context) }

    // Handle auth state changes
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> {
                if (name.isEmpty()) name = state.displayName
                onGoogleSignIn(state.userId, state.accessToken, state.displayName)
                authViewModel.reset()
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Welcome to Praxis",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            InfoButton(
                title = "Getting Started",
                description = "Create your Praxis profile. Your name and age help partners know who they're working with. A bio is optional but increases your match quality.",
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "A Social Operating System for Real Progress",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Google Sign-In
        Button(
            onClick = {
                Log.d(TAG, "Google Sign-In button clicked")
                coroutineScope.launch {
                    try {
                        Log.d(TAG, "Using Client ID: ${BuildConfig.GOOGLE_WEB_CLIENT_ID}")
                        val googleIdOption = GetGoogleIdOption.Builder()
                            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                            .setFilterByAuthorizedAccounts(false)
                            .setAutoSelectEnabled(false)
                            .build()
                        
                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()
                        
                        Log.d(TAG, "Requesting credentials...")
                        val result = credentialManager.getCredential(context, request)
                        Log.d(TAG, "Credential result received: ${result.credential.type}")
                        
                        val credential = result.credential
                        if (credential is CustomCredential &&
                            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                        ) {
                            val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                            Log.d(TAG, "ID Token obtained successfully")
                            authViewModel.signInWithGoogle(googleIdToken)
                        } else {
                            Log.w(TAG, "Unexpected credential type: ${credential.type}")
                            Toast.makeText(context, "Unexpected sign-in result", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: GetCredentialException) {
                        Log.e(TAG, "Credential error: ${e.message} (Type: ${e.javaClass.simpleName})")
                        Toast.makeText(context, "Sign-in error: ${e.message}", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Log.e(TAG, "Unexpected error during sign-in", e)
                        Toast.makeText(context, "Unexpected error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = authState !is AuthState.Loading,
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("Sign in with Google", fontSize = 16.sp)
            }
        }

        if (authState is AuthState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = (authState as AuthState.Error).message ?: "Sign-in failed",
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("or continue manually", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = age,
            onValueChange = { if (it.all(Char::isDigit)) age = it },
            label = { Text("Age") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Short Bio (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            placeholder = { Text("Tell us a bit about yourself...") },
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (showFieldError) {
            Text(
                text = "Please enter your name and a valid age (18–100)",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        Button(
            onClick = {
                val ageInt = age.toIntOrNull()
                if (name.isNotBlank() && ageInt != null && ageInt in 18..100) {
                    onManualContinue(name, ageInt, bio)
                } else {
                    showFieldError = true
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text("Continue to Goal Selection", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "By continuing, you agree to build your best self",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
