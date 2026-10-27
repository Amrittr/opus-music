/**
 * Opus: dialogs for account actions (password reset, name, password, delete).
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.metrolist.music.auth.OpusAuth
import com.metrolist.music.auth.OpusAuthException
import kotlinx.coroutines.launch

/**
 * Shared shape for the account dialogs: shows [error], disables input while [action] runs,
 * and closes itself when the action succeeds.
 */
@Composable
private fun AuthActionDialog(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    validate: () -> String?,
    action: suspend () -> Unit,
    onSuccess: () -> Unit = onDismiss,
    destructive: Boolean = false,
    content: @Composable (enabled: Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var isWorking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isWorking) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content(!isWorking)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isWorking,
                onClick = {
                    error = validate()
                    if (error != null) return@TextButton
                    isWorking = true
                    scope.launch {
                        try {
                            action()
                            onSuccess()
                        } catch (e: OpusAuthException) {
                            error = e.message
                        } finally {
                            isWorking = false
                        }
                    }
                },
            ) {
                if (isWorking) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        confirmLabel,
                        color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isWorking) { Text("Cancel") }
        },
    )
}

@Composable
fun PasswordResetDialog(initialEmail: String, onDismiss: () -> Unit) {
    var email by remember { mutableStateOf(initialEmail) }
    var sent by remember { mutableStateOf(false) }

    if (sent) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Check your email") },
            text = { Text("If an account exists for ${email.trim()}, we've sent a link to reset its password.") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
        )
        return
    }

    AuthActionDialog(
        title = "Reset password",
        confirmLabel = "Send link",
        onDismiss = onDismiss,
        validate = { if (isValidEmail(email)) null else "Enter a valid email address." },
        action = { OpusAuth.sendPasswordReset(email) },
        onSuccess = { sent = true },
    ) { enabled ->
        Text("We'll email you a link to choose a new password.")
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun EditNameDialog(currentName: String, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    AuthActionDialog(
        title = "Edit name",
        confirmLabel = "Save",
        onDismiss = onDismiss,
        validate = { if (name.isBlank()) "Enter your name." else null },
        action = { OpusAuth.updateName(name) },
    ) { enabled ->
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Your name") },
            singleLine = true,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun ChangePasswordDialog(onDismiss: () -> Unit, onChanged: () -> Unit) {
    var current by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    AuthActionDialog(
        title = "Change password",
        confirmLabel = "Change",
        onDismiss = onDismiss,
        validate = {
            when {
                current.isEmpty() -> "Enter your current password."
                newPassword.length < OpusAuth.MIN_PASSWORD_LENGTH ->
                    "New password must be at least ${OpusAuth.MIN_PASSWORD_LENGTH} characters."
                newPassword != confirm -> "New passwords don't match."
                else -> null
            }
        },
        action = { OpusAuth.changePassword(current, newPassword) },
        onSuccess = onChanged,
    ) { enabled ->
        PasswordField(current, { current = it }, "Current password", visible, { visible = !visible }, enabled, ImeAction.Next) {}
        PasswordField(newPassword, { newPassword = it }, "New password", visible, { visible = !visible }, enabled, ImeAction.Next) {}
        PasswordField(confirm, { confirm = it }, "Confirm new password", visible, { visible = !visible }, enabled, ImeAction.Done) {}
    }
}

@Composable
fun DeleteAccountDialog(onDismiss: () -> Unit, onDeleted: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    AuthActionDialog(
        title = "Delete account?",
        confirmLabel = "Delete",
        destructive = true,
        onDismiss = onDismiss,
        validate = { if (password.isEmpty()) "Enter your password to confirm." else null },
        action = { OpusAuth.deleteAccount(password) },
        onSuccess = onDeleted,
    ) { enabled ->
        Text("This permanently deletes your Opus account. Music saved on this phone stays on this phone.")
        PasswordField(password, { password = it }, "Password", visible, { visible = !visible }, enabled, ImeAction.Done) {}
    }
}
