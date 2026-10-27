/**
 * Opus: the signed-in user's page.
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.auth.OpusAuth
import com.metrolist.music.auth.OpusAuthException
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.utils.backToMain
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private fun formatDate(ms: Long?): String =
    ms?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it)) } ?: "Unknown"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpusProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val scope = rememberCoroutineScope()

    val user by OpusAuth.user.collectAsState()
    val likedSongs by remember(database) { database.likedSongsCount() }.collectAsState(initial = 0)
    val playlists by remember(database) { database.playlistsByCreateDateAsc() }.collectAsState(initial = emptyList())
    val plays by remember(database) { database.eventCount() }.collectAsState(initial = 0)

    var showEditName by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var showSignOut by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var verificationBusy by remember { mutableStateOf(false) }

    // Signed out from somewhere else: the auth gate is already showing the login screen.
    val current = user ?: return

    fun stopPlaybackAndSignOut() {
        playerConnection?.player?.let {
            it.stop()
            it.clearMediaItems()
        }
        OpusAuth.signOut()
    }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    if (showEditName) EditNameDialog(current.name) { showEditName = false }
    if (showChangePassword) {
        ChangePasswordDialog(
            onDismiss = { showChangePassword = false },
            onChanged = {
                showChangePassword = false
                toast("Password changed")
            },
        )
    }
    if (showDelete) {
        DeleteAccountDialog(
            onDismiss = { showDelete = false },
            onDeleted = {
                showDelete = false
                stopPlaybackAndSignOut()
            },
        )
    }
    if (showSignOut) {
        AlertDialog(
            onDismissRequest = { showSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("You'll need to sign in again to use Opus. Playback will stop.") },
            confirmButton = {
                TextButton(onClick = {
                    showSignOut = false
                    stopPlaybackAndSignOut()
                }) { Text("Sign out") }
            },
            dismissButton = { TextButton(onClick = { showSignOut = false }) { Text("Cancel") } },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))
        Spacer(Modifier.height(16.dp))

        // Avatar with the first letter of the user's name
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = current.name.firstOrNull()?.uppercase() ?: "A",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = current.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = current.email,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        AssistChip(
            onClick = {},
            label = { Text(if (current.isEmailVerified) "Email verified" else "Email not verified") },
            leadingIcon = {
                Icon(
                    painterResource(if (current.isEmailVerified) R.drawable.verified_check else R.drawable.error),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
        )

        if (!current.isEmailVerified) {
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Verify your email", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "We sent a link to ${current.email}. Open it, then tap \"I've verified\".",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            enabled = !verificationBusy,
                            onClick = {
                                verificationBusy = true
                                scope.launch {
                                    try {
                                        OpusAuth.sendVerificationEmail()
                                        toast("Verification email sent")
                                    } catch (e: OpusAuthException) {
                                        toast(e.message ?: "Could not send the email")
                                    } finally {
                                        verificationBusy = false
                                    }
                                }
                            },
                        ) { Text("Resend email") }
                        Button(
                            enabled = !verificationBusy,
                            onClick = {
                                verificationBusy = true
                                scope.launch {
                                    try {
                                        OpusAuth.refresh()
                                        if (OpusAuth.user.value?.isEmailVerified != true) toast("Not verified yet")
                                    } catch (e: OpusAuthException) {
                                        toast(e.message ?: "Could not check")
                                    } finally {
                                        verificationBusy = false
                                    }
                                }
                            },
                        ) { Text("I've verified") }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Liked songs", likedSongs.toString(), Modifier.weight(1f))
            StatCard("Playlists", playlists.size.toString(), Modifier.weight(1f))
            StatCard("Plays", plays.toString(), Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))
        Material3SettingsGroup(
            title = "Account details",
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.mail),
                    title = { Text("Email") },
                    description = { Text(current.email) },
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.calendar_event),
                    title = { Text("Member since") },
                    description = { Text(formatDate(current.createdAtMs)) },
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.history),
                    title = { Text("Last signed in") },
                    description = { Text(formatDate(current.lastSignInAtMs)) },
                ),
            ),
        )

        Spacer(Modifier.height(16.dp))
        Material3SettingsGroup(
            title = "Manage account",
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.edit),
                    title = { Text("Edit name") },
                    onClick = { showEditName = true },
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.lock),
                    title = { Text("Change password") },
                    onClick = { showChangePassword = true },
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.logout),
                    title = { Text("Sign out") },
                    onClick = { showSignOut = true },
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.delete),
                    title = { Text("Delete account", color = MaterialTheme.colorScheme.error) },
                    onClick = { showDelete = true },
                ),
            ),
        )
        Spacer(Modifier.height(24.dp))
    }

    TopAppBar(
        title = { Text("Your account") },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        },
    )
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
