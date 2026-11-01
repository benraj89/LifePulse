package com.vibecheck.lifepulse.ui.privacy

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.vibecheck.lifepulse.R
import com.vibecheck.lifepulse.ui.neobrutalism.NeoColors
import com.vibecheck.lifepulse.ui.neobrutalism.NeoPageHeader
import com.vibecheck.lifepulse.ui.neobrutalism.NeoTypography

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val crashlytics = remember { FirebaseCrashlytics.getInstance() }
    var enabled by remember { mutableStateOf(crashlytics.isCrashlyticsCollectionEnabled) }
    val html = remember(context) {
        context.assets.open("privacy-policy.html").bufferedReader().use { it.readText() }
            .substringAfter("<main>").substringBefore("</main>")
    }
    PrivacyContent(html, enabled, onEnabledChange = {
        crashlytics.setCrashlyticsCollectionEnabled(it)
        enabled = it
    }, onBack = onBack)
}

@Composable
internal fun PrivacyContent(
    policyHtml: String,
    crashReportsEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val linkError = stringResource(R.string.privacy_link_error)
    val policy = remember(policyHtml, uriHandler, context, linkError) {
        AnnotatedString.fromHtml(policyHtml,
            linkStyles = TextLinkStyles(SpanStyle(color = NeoColors.Ink, textDecoration = TextDecoration.Underline)),
            linkInteractionListener = LinkInteractionListener { link ->
                if (link is LinkAnnotation.Url) {
                    try {
                        uriHandler.openUri(link.url)
                    } catch (_: IllegalArgumentException) {
                        Toast.makeText(context, linkError, Toast.LENGTH_LONG).show()
                    }
                }
            })
    }
    Box(Modifier.fillMaxSize().background(NeoColors.Background).navigationBarsPadding(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 720.dp).fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            NeoPageHeader(stringResource(R.string.privacy_support)) {
                TextButton(onClick = onBack, colors = ButtonDefaults.textButtonColors(contentColor = NeoColors.Ink)) {
                    Text(stringResource(R.string.privacy_back), style = NeoTypography.labelLarge)
                }
            }
            Row(Modifier.fillMaxWidth().toggleable(value = crashReportsEnabled, role = Role.Switch,
                onValueChange = onEnabledChange).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.privacy_crash_title), Modifier.weight(1f),
                    style = NeoTypography.titleMedium, color = NeoColors.Ink)
                Switch(checked = crashReportsEnabled, onCheckedChange = null)
            }
            Text(stringResource(R.string.privacy_crash_description), style = NeoTypography.bodyMedium, color = NeoColors.Ink)
            Text(stringResource(R.string.privacy_crash_disable_hint), style = NeoTypography.bodyMedium, color = NeoColors.Ink)
            HorizontalDivider(color = NeoColors.Ink)
            SelectionContainer {
                Text(policy, style = NeoTypography.bodyLarge, color = NeoColors.Ink)
            }
        }
    }
}
