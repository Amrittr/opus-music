/**
 * Opus: the developer page.
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.BuildConfig
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.utils.backToMain

private object Developer {
    const val NAME = "Amrit TR"
    const val ROLE = "Developer of Opus"
    const val GITHUB_URL = "https://github.com/Amrittr"
    const val LINKEDIN_URL = "" // Set to the LinkedIn profile URL to show the LinkedIn button
    const val EMAIL = "amrittr08@gmail.com"
}

private fun displayUrl(url: String) = url.removePrefix("https://").removePrefix("www.").trimEnd('/')

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperScreen(navController: NavController) {
    val uriHandler = LocalUriHandler.current
    val emailUri = "mailto:${Developer.EMAIL}?subject=Opus%20feedback"

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
        Spacer(Modifier.height(24.dp))

        Image(
            painter = painterResource(R.drawable.app_logo),
            contentDescription = null,
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = Developer.NAME,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = Developer.ROLE,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { uriHandler.openUri(Developer.GITHUB_URL) }) {
                Icon(painterResource(R.drawable.github), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("GitHub")
            }
            if (Developer.LINKEDIN_URL.isNotEmpty()) {
                FilledTonalButton(onClick = { uriHandler.openUri(Developer.LINKEDIN_URL) }) {
                    Icon(painterResource(R.drawable.linkedin), contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("LinkedIn")
                }
            }
            FilledTonalButton(onClick = { uriHandler.openUri(emailUri) }) {
                Icon(painterResource(R.drawable.mail), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Email")
            }
        }

        Spacer(Modifier.height(24.dp))
        Material3SettingsGroup(
            title = "Contact",
            items = buildList {
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.github),
                        title = { Text("GitHub") },
                        description = { Text(displayUrl(Developer.GITHUB_URL)) },
                        onClick = { uriHandler.openUri(Developer.GITHUB_URL) },
                    ),
                )
                if (Developer.LINKEDIN_URL.isNotEmpty()) {
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.linkedin),
                            title = { Text("LinkedIn") },
                            description = { Text(displayUrl(Developer.LINKEDIN_URL)) },
                            onClick = { uriHandler.openUri(Developer.LINKEDIN_URL) },
                        ),
                    )
                }
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.mail),
                        title = { Text("Email") },
                        description = { Text(Developer.EMAIL) },
                        onClick = { uriHandler.openUri(emailUri) },
                    ),
                )
            },
        )

        Spacer(Modifier.height(16.dp))
        Material3SettingsGroup(
            title = "About Opus",
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.info),
                    title = { Text("Version") },
                    description = { Text(BuildConfig.VERSION_NAME) },
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.music_note),
                    title = { Text("Built with") },
                    description = { Text("Kotlin, Jetpack Compose, Media3 and Firebase") },
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.favorite),
                    title = { Text("Based on Meld") },
                    description = { Text("By Francesco Grazioso, a fork of Metrolist and InnerTune") },
                    onClick = { uriHandler.openUri("https://github.com/FrancescoGrazioso/Meld") },
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.link),
                    title = { Text("License") },
                    description = { Text("GNU General Public License v3.0") },
                    onClick = { uriHandler.openUri("https://www.gnu.org/licenses/gpl-3.0.html") },
                ),
            ),
        )
        Spacer(Modifier.height(24.dp))
    }

    TopAppBar(
        title = { Text("Developer") },
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
