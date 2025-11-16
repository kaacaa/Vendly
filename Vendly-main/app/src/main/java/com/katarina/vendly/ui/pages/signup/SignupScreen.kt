package com.katarina.vendly.ui.pages.signup

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.katarina.vendly.components.ImagePicker
import com.katarina.vendly.ui.auth.AuthState
import com.katarina.vendly.ui.auth.AuthViewModel

@Composable
fun SignupScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    signupViewModel: SignupViewModel = viewModel()
) {
    val uiState = signupViewModel.uiState
    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        signupViewModel.events.collect { ev ->
            when (ev) {
                is SignupEvent.NavigateHome -> {
                    navController.navigate("home") {
                        popUpTo("signup") { inclusive = true }
                        launchSingleTop = true
                    }
                }
                is SignupEvent.ShowToast -> {
                    Toast.makeText(context, ev.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(authState.value) {
        when (val s = authState.value) {
            is AuthState.Authenticated -> {
                signupViewModel.handlePostAuthIfNeeded(context)
            }
            is AuthState.Error -> {
                Toast.makeText(context, s.message, Toast.LENGTH_SHORT).show()
                signupViewModel.setError(s.message)
            }
            is AuthState.Loading -> Unit
            else -> signupViewModel.setLoadingDone()
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Signup Page", fontSize = 32.sp)

        Spacer(modifier = Modifier.height(16.dp))

        ImagePicker(
            selectedImage = uiState.photoUri,
            onImagePicked = { signupViewModel.onPhotoPicked(it) },
            onRemove = { signupViewModel.onPhotoPicked(null) },
            size = 120.dp
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = uiState.email,
            onValueChange = { signupViewModel.onEmailChanged(it) },
            label = { Text(text = "Email") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.fullName,
            onValueChange = { signupViewModel.onFullNameChanged(it) },
            label = { Text(text = "Full name") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.phoneNumber,
            onValueChange = { signupViewModel.onPhoneChanged(it) },
            label = { Text(text = "Phone") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.password,
            onValueChange = { signupViewModel.onPasswordChanged(it) },
            label = { Text(text = "Password") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            singleLine = true,
            visualTransformation =
                if (uiState.password.isNotEmpty()) PasswordVisualTransformation()
                else VisualTransformation.None
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { signupViewModel.signup(authViewModel, context) },
            enabled = !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Text(text = "Create account")
            }
        }

        uiState.errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = Color.Red)
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = { navController.navigate("login") }) {
            Text(text = "Already have an account, Login")
        }
    }
}