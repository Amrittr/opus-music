/**
 * Opus: sign-in / create-account screen shown until the user is logged in.
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.auth

import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.auth.OpusAuth
import com.metrolist.music.auth.OpusAuthException
import kotlinx.coroutines.launch

private enum class LoginMode { SIGN_IN, CREATE_ACCOUNT }

internal fun isValidEmail(email: String) = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

@Composable
fun OpusLoginScreen() {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var mode by rememberSaveable { mutableStateOf(LoginMode.SIGN_IN) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var isWorking by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    val creating = mode == LoginMode.CREATE_ACCOUNT

    fun validate(): String? = when {
        creating && name.isBlank() -> "Enter your name."
        !isValidEmail(email) -> "Enter a valid email address."
        password.length < OpusAuth.MIN_PASSWORD_LENGTH ->
            "Password must be at least ${OpusAuth.MIN_PASSWORD_LENGTH} characters."
        creating && password != confirmPassword -> "Passwords don't match."
        else -> null
    }

    fun submit() {
        if (isWorking) return
        error = validate()
        if (error != null) return
        focusManager.clearFocus()
        isWorking = true
        scope.launch {
            try {
                if (creating) OpusAuth.createAccount(name, email, password)
                else OpusAuth.signIn(email, password)
                // On success the auth gate swaps this screen for the app.
            } catch (e: OpusAuthException) {
                error = e.message
            } finally {
                isWorking = false
            }
        }
    }

    if (showResetDialog) {
        PasswordResetDialog(initialEmail = email, onDismiss = { showResetDialog = false })
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp)),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (creating) "Create your account to start listening"
                else "Sign in to continue listening",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))

            Column(
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    LoginMode.entries.forEachIndexed { index, item ->
                        SegmentedButton(
                            selected = mode == item,
                            onClick = {
                                mode = item
                                error = null
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, LoginMode.entries.size),
                        ) {
                            Text(if (item == LoginMode.SIGN_IN) "Sign in" else "Create account")
                        }
                    }
                }

                AnimatedVisibility(visible = creating) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Your name") },
                        singleLine = true,
                        enabled = !isWorking,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        leadingIcon = { Icon(painterResource(R.drawable.person), contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    enabled = !isWorking,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    leadingIcon = { Icon(painterResource(R.drawable.mail), contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                )

                PasswordField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    visible = showPassword,
                    onToggleVisible = { showPassword = !showPassword },
                    enabled = !isWorking,
                    imeAction = if (creating) ImeAction.Next else ImeAction.Done,
                    onImeAction = { if (creating) focusManager.moveFocus(FocusDirection.Down) else submit() },
                )

                AnimatedVisibility(visible = creating) {
                    PasswordField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = "Confirm password",
                        visible = showPassword,
                        onToggleVisible = { showPassword = !showPassword },
                        enabled = !isWorking,
                        imeAction = ImeAction.Done,
                        onImeAction = { submit() },
                    )
                }

                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Button(
                    onClick = { submit() },
                    enabled = !isWorking,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    if (isWorking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(if (creating) "Create account" else "Sign in")
                    }
                }

                if (!creating) {
                    TextButton(
                        onClick = { showResetDialog = true },
                        enabled = !isWorking,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text("Forgot password?")
                    }
                }
            }
        }
    }
}

@Composable
internal fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    enabled: Boolean,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        leadingIcon = { Icon(painterResource(R.drawable.lock), contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onToggleVisible) {
                Icon(
                    painterResource(if (visible) R.drawable.visibility_off else R.drawable.visibility),
                    contentDescription = if (visible) "Hide password" else "Show password",
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}
