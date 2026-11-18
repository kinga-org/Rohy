package app.rohy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Building blocks.

@Composable
private fun Logo(size: Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(rohyColors.logo), contentAlignment = Alignment.Center) {
        Icon(RohyIcons.Mark, contentDescription = null, tint = Color.White, modifier = Modifier.size(size))
    }
}

/** Scrolling page on the ground color, below the status bar. */
@Composable
private fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().background(rohyColors.ground).windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        content = content,
    )
}

@Composable
private fun AppBar(actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        Logo(32.dp)
        Text(
            "Rohy", style = RohyText.appTitle, color = rohyColors.ink,
            modifier = Modifier.padding(start = 10.dp).weight(1f).semantics { heading() },
        )
        actions()
    }
}

@Composable
private fun BackBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 0.dp)) {
            Icon(RohyIcons.Back, contentDescription = "Retour", tint = rohyColors.ink)
        }
        Text(title, style = RohyText.screenTitle, color = rohyColors.ink, modifier = Modifier.semantics { heading() })
    }
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(rohyColors.surface).border(1.dp, rohyColors.line, shape),
        content = content,
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(text.uppercase(Locale.FRANCE), style = RohyText.section, color = rohyColors.muted, modifier = Modifier.semantics { heading() })
}

@Composable
private fun Divider() {
    Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(1.dp).background(rohyColors.divider))
}

@Composable
private fun IconTile(icon: ImageVector, background: Color, tint: Color, size: Dp = 40.dp, radius: Dp = 12.dp, iconSize: Dp = 20.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(radius)).background(background), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
private fun Dot(color: Color, hollow: Boolean = false, size: Dp = 8.dp) {
    val modifier = Modifier.size(size).clip(CircleShape)
    Box(if (hollow) modifier.border(1.5.dp, color, CircleShape) else modifier.background(color))
}

@Composable
private fun PrimaryButton(text: String, icon: ImageVector, onClick: () -> Unit, enabled: Boolean = true) {
    val c = rohyColors
    Button(
        onClick = onClick, enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = c.primary, contentColor = Color.White,
            disabledContainerColor = c.disabled, disabledContentColor = c.disabledText,
        ),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(text, style = RohyText.button, modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
private fun SecondaryButton(text: String, icon: ImageVector, onClick: () -> Unit, height: Dp = 52.dp) {
    val c = rohyColors
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(height),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, c.controlLine),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = c.surface, contentColor = c.ink),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(text, style = RohyText.button.copy(fontSize = RohyText.rowTitle.fontSize), modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
private fun PillButton(text: String, onClick: () -> Unit, filled: Boolean, container: Color? = null) {
    val c = rohyColors
    val colors = if (filled) {
        ButtonDefaults.buttonColors(containerColor = container ?: c.primary, contentColor = Color.White)
    } else {
        ButtonDefaults.outlinedButtonColors(containerColor = c.surface, contentColor = c.accentText)
    }
    val modifier = Modifier.heightIn(min = 36.dp)
    val padding = PaddingValues(horizontal = 14.dp)
    if (filled) {
        Button(onClick, modifier, shape = CircleShape, colors = colors, contentPadding = padding) { Text(text, style = RohyText.chip) }
    } else {
        OutlinedButton(
            onClick, modifier, shape = CircleShape, colors = colors, contentPadding = padding,
            border = androidx.compose.foundation.BorderStroke(1.dp, c.controlLine),
        ) { Text(text, style = RohyText.chip) }
    }
}

// Onboarding.

@Composable
fun OnboardingScreen(onScan: () -> Unit, onPasteCode: () -> Unit) {
    val c = rohyColors
    BoxWithConstraints(Modifier.fillMaxSize().background(c.ground).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).heightIn(min = maxHeight)
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                AppBar()
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconTile(RohyIcons.Phone, c.accentSoft, c.accentText, size = 64.dp, radius = 20.dp, iconSize = 30.dp)
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { repeat(6) { Dot(c.offline, size = 3.dp) } }
                        IconTile(RohyIcons.Computer, c.accentSoft, c.accentText, size = 64.dp, radius = 20.dp, iconSize = 32.dp)
                    }
                    Text(
                        "Relie ton ordinateur", style = RohyText.hero, color = c.ink,
                        modifier = Modifier.padding(top = 8.dp).semantics { heading() },
                    )
                    Text(
                        "Copie d'un côté, colle de l'autre. Tout passe par ton Wi-Fi, chiffré de bout en bout.",
                        style = RohyText.body, color = c.muted, textAlign = TextAlign.Center,
                    )
                }
                Card(Modifier.padding(vertical = 0.dp)) {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        listOf(
                            "Installe Rohy sur ton ordinateur : Mac, Windows ou Linux.",
                            "Dans le menu de son icône, choisis « Associer un téléphone ».",
                            "Scanne le QR code qui s'affiche.",
                        ).forEachIndexed { index, step ->
                            Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box(Modifier.size(28.dp).clip(CircleShape).background(c.primary), contentAlignment = Alignment.Center) {
                                    Text("${index + 1}", style = RohyText.chip.copy(fontSize = RohyText.rowTitle.fontSize.times(0.93f)), color = Color.White)
                                }
                                Text(step, style = RohyText.body, color = c.ink, modifier = Modifier.padding(top = 3.dp))
                            }
                        }
                    }
                }
            }
            Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton("Scanner le QR code", RohyIcons.Scan, onScan)
                SecondaryButton("Coller le code d'association", RohyIcons.Clipboard, onPasteCode)
            }
        }
    }
}

// Home.

/** Everything the home screen shows once a computer is paired. */
data class HomeContent(
    val computer: String,
    val pairedDetail: String,
    val header: HomeHeader,
    val activity: String?,
    val reliability: ReliabilitySummary,
)

@Composable
fun HomeScreen(
    content: HomeContent,
    onSend: () -> Unit,
    onReconnect: () -> Unit,
    onAllowLocalNetwork: () -> Unit,
    onAddTile: () -> Unit,
    onOpenReliability: () -> Unit,
    onOpenJournal: () -> Unit,
    onForget: () -> Unit,
) {
    val c = rohyColors
    val connected = content.header is HomeHeader.Connected
    Page {
        AppBar {
            IconButton(onClick = onOpenJournal) {
                Icon(RohyIcons.History, contentDescription = "Journal de connexion", tint = c.muted)
            }
        }
        when (val header = content.header) {
            is HomeHeader.Connected -> ConnectedBanner(header.computer, content.activity)
            is HomeHeader.Unreachable -> UnreachableCard(header, onReconnect)
            is HomeHeader.LocalNetworkDenied -> {
                UnreachableCard(HomeHeader.Unreachable("${header.computer} injoignable", "Accès au réseau local nécessaire", false), onReconnect)
                LocalNetworkAlert(onAllowLocalNetwork)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton("Envoyer le presse-papiers", RohyIcons.Send, onSend, enabled = connected)
            Text(
                if (connected) "Ce que tu copies sur l'ordinateur arrive ici tout seul." else "Disponible dès que l'ordinateur est joignable.",
                style = RohyText.small, color = c.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }
        if (connected) Shortcuts(onAddTile)
        ReliabilityRow(content.reliability, onOpenReliability)
        ComputerCard(content.computer, content.pairedDetail, onForget)
    }
}

@Composable
private fun ConnectedBanner(computer: String, activity: String?) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brand).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(RohyIcons.Computer, Color.White.copy(alpha = 0.14f), Color.White, size = 44.dp, radius = 22.dp, iconSize = 22.dp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("SYNCHRONISATION ACTIVE", style = RohyText.label, color = OnBrandLabel)
                Text("Connecté à $computer", style = RohyText.bannerTitle, color = Color.White, modifier = Modifier.semantics { heading() })
            }
        }
        if (activity != null) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.16f)))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val icon = if (activity.startsWith("Texte envoyé")) RohyIcons.Sent else RohyIcons.Received
                Icon(icon, contentDescription = null, tint = OnBrandBody, modifier = Modifier.size(16.dp))
                Text(activity, style = RohyText.small, color = OnBrandBody)
            }
        }
    }
}

@Composable
private fun UnreachableCard(header: HomeHeader.Unreachable, onReconnect: () -> Unit) {
    val c = rohyColors
    Card {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconTile(RohyIcons.WifiOff, c.neutralSoft, c.muted, size = 44.dp, radius = 22.dp, iconSize = 22.dp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(header.title, style = RohyText.cardTitle, color = c.ink, modifier = Modifier.semantics { heading() })
                    Text(header.detail, style = RohyText.small, color = c.muted)
                }
            }
            Text(
                "Vérifie que l'ordinateur est allumé, que Rohy y tourne et que vous êtes sur le même Wi-Fi.",
                style = RohyText.small.copy(fontSize = RohyText.body.fontSize.times(0.93f)), color = c.muted,
            )
            if (header.canReconnect) SecondaryButton("Reconnecter maintenant", RohyIcons.Refresh, onReconnect, height = 48.dp)
        }
    }
}

@Composable
private fun LocalNetworkAlert(onAllow: () -> Unit) {
    val c = rohyColors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.warningSoft).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(RohyIcons.Warning, contentDescription = null, tint = c.warning, modifier = Modifier.size(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Accès au réseau local refusé", style = RohyText.rowTitle, color = c.warningText)
            Text(
                "Sans cette autorisation, Android empêche Rohy de joindre l'ordinateur sur le Wi-Fi.",
                style = RohyText.small, color = c.warningText,
            )
            Box(Modifier.padding(top = 4.dp)) { PillButton("Autoriser", onAllow, filled = true, container = c.warning) }
        }
    }
}

@Composable
private fun Shortcuts(onAddTile: () -> Unit) {
    val c = rohyColors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Envoyer plus vite")
        Card {
            ShortcutRow(RohyIcons.Tile, "Tuile des réglages rapides", "Glisse depuis le haut de l'écran, touche « Envoyer ».") {
                PillButton("Ajouter", onAddTile, filled = false)
            }
            Divider()
            ShortcutRow(RohyIcons.TextSelect, "Menu de sélection", "Sélectionne un texte, puis « Envoyer à l'ordinateur ».")
            Divider()
            ShortcutRow(RohyIcons.Share, "Partager", "Depuis n'importe quelle app : Partager, puis Rohy.")
        }
    }
}

@Composable
private fun ShortcutRow(icon: ImageVector, title: String, detail: String, trailing: @Composable () -> Unit = {}) {
    val c = rohyColors
    Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconTile(icon, c.accentSoft, c.accentText)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = RohyText.rowTitle, color = c.ink)
            Text(detail, style = RohyText.small, color = c.muted)
        }
        trailing()
    }
}

@Composable
private fun ReliabilityRow(summary: ReliabilitySummary, onOpen: () -> Unit) {
    val c = rohyColors
    val (tile, tint) = if (summary.needsAttention) c.warningSoft to c.warning else c.successSoft to c.successText
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(c.surface).border(1.dp, c.line, shape)
            .clickable(role = Role.Button, onClick = onOpen).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(if (summary.needsAttention) RohyIcons.Shield else RohyIcons.Check, tile, tint)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Garder la connexion", style = RohyText.rowTitle, color = c.ink)
            Text(summary.status, style = RohyText.smallStrong, color = if (summary.needsAttention) c.warning else c.muted)
        }
        Icon(RohyIcons.Chevron, contentDescription = null, tint = c.muted, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ComputerCard(computer: String, detail: String, onForget: () -> Unit) {
    val c = rohyColors
    var confirm by remember { mutableStateOf(false) }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Oublier $computer ?", style = RohyText.cardTitle) },
            text = { Text("Il faudra scanner à nouveau le QR code de l'ordinateur pour le réassocier.", style = RohyText.body) },
            confirmButton = {
                TextButton(onClick = { confirm = false; onForget() }) { Text("Oublier", style = RohyText.chip, color = c.danger) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Annuler", style = RohyText.chip, color = c.ink) } },
            containerColor = c.surface,
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("Ordinateur associé")
        Card {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconTile(RohyIcons.Computer, c.neutralSoft, c.muted)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(computer, style = RohyText.rowTitle, color = c.ink)
                    Text(detail, style = RohyText.small, color = c.muted)
                }
                TextButton(onClick = { confirm = true }) { Text("Oublier", style = RohyText.smallStrong, color = c.danger) }
            }
        }
    }
}

// Reliability.

@Composable
fun ReliabilityScreen(checks: List<ReliabilityCheck>, onFix: (CheckId) -> Unit, onBack: () -> Unit) {
    val c = rohyColors
    val summary = summarize(checks)
    Page {
        BackBar("Garder la connexion", onBack)
        Card {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(summary.title, style = RohyText.cardTitle.copy(fontSize = RohyText.cardTitle.fontSize.times(0.95f)), color = c.ink, modifier = Modifier.weight(1f))
                    if (summary.toFix > 0) Text("${summary.toFix} à corriger", style = RohyText.smallStrong, color = c.warning)
                }
                val fraction = if (summary.total == 0) 1f else summary.ready.toFloat() / summary.total
                Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(c.divider)) {
                    Box(Modifier.fillMaxWidth(fraction).height(8.dp).clip(CircleShape).background(c.success))
                }
                Text(
                    "Android coupe les apps en arrière-plan pour économiser la batterie. Ces réglages gardent Rohy connecté, même écran éteint.",
                    style = RohyText.small, color = c.muted,
                )
            }
        }
        val groups = listOf(
            "À corriger" to checks.filter { it.state == CheckState.TO_FIX },
            "À vérifier" to checks.filter { it.state == CheckState.TO_CHECK },
            "Prêts" to checks.filter { it.state == CheckState.OK },
        )
        groups.filter { it.second.isNotEmpty() }.forEach { (title, items) ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(title)
                Card {
                    items.forEachIndexed { index, check ->
                        if (index > 0) Divider()
                        CheckRow(check, onFix)
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckRow(check: ReliabilityCheck, onFix: (CheckId) -> Unit) {
    val c = rohyColors
    val (icon, background, tint) = when (check.state) {
        CheckState.OK -> Triple(RohyIcons.Check, c.successSoft, c.successText)
        CheckState.TO_FIX -> Triple(RohyIcons.Exclamation, c.warningSoft, c.warning)
        CheckState.TO_CHECK -> Triple(RohyIcons.Question, c.neutralSoft, c.muted)
    }
    val ok = check.state == CheckState.OK
    Row(
        Modifier.padding(16.dp),
        verticalAlignment = if (ok) Alignment.CenterVertically else Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(icon, background, tint, size = 32.dp, radius = 16.dp, iconSize = 18.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(check.title, style = RohyText.rowTitle, color = c.ink)
            if (!ok) Text(check.explanation, style = RohyText.small, color = c.muted)
        }
        when (check.state) {
            CheckState.TO_FIX -> PillButton("Corriger", { onFix(check.id) }, filled = true)
            CheckState.TO_CHECK -> if (check.id != CheckId.RECENTS_LOCK) PillButton("Ouvrir", { onFix(check.id) }, filled = false)
            CheckState.OK -> Unit
        }
    }
}

// Journal.

@Composable
fun JournalScreen(days: List<JournalDay>, onBack: () -> Unit) {
    val c = rohyColors
    val time = remember { SimpleDateFormat("HH:mm", Locale.FRANCE) }
    Page {
        BackBar("Journal de connexion", onBack)
        Text(
            "Les derniers événements de connexion, pour comprendre une coupure. Rien ne quitte le téléphone.",
            style = RohyText.small, color = c.muted,
        )
        if (days.isEmpty()) {
            Card { Text("Aucun événement pour l'instant.", style = RohyText.body, color = c.muted, modifier = Modifier.padding(16.dp)) }
        }
        days.forEach { day ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(day.title)
                Card {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        day.events.forEach { event ->
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(time.format(Date(event.atMillis)), style = RohyText.smallStrong, color = c.muted, modifier = Modifier.width(44.dp))
                                when (eventKind(event.message)) {
                                    EventKind.CONNECTED -> Dot(c.success)
                                    EventKind.SEARCHING -> Dot(c.offline, hollow = true)
                                    EventKind.PROBLEM -> Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(c.warning))
                                    EventKind.INFO -> Dot(c.accentText)
                                }
                                Text(event.message, style = RohyText.small.copy(fontSize = RohyText.body.fontSize.times(0.93f)), color = c.ink)
                            }
                        }
                    }
                }
            }
        }
    }
}
