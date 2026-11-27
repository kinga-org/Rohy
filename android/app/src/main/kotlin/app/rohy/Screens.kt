package app.rohy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Building blocks.

/** Lets long host names ("RAHERINO-MacBook-Pro") wrap after their hyphens instead of mid-word. */
private fun breakable(name: String) = name.replace("-", "-\u200B")

@Composable
private fun Logo(size: Dp) {
    Image(painterResource(R.drawable.rohy_logo), contentDescription = null, modifier = Modifier.size(size))
}

/** Scrolling page on the ground color, below the status bar. */
@Composable
private fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().background(rohyColors.ground).windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        content = content,
    )
}

@Composable
private fun AppBar(actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().height(56.dp).padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Logo(40.dp)
        Text(
            "Rohy", style = RohyText.appTitle, color = rohyColors.ink,
            modifier = Modifier.padding(start = 12.dp).weight(1f).semantics { heading() },
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
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(rohyColors.surface),
        content = content,
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = RohyText.section, color = rohyColors.muted, modifier = Modifier.padding(start = 8.dp).semantics { heading() })
}

@Composable
private fun Divider() {
    Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(1.dp).background(rohyColors.divider))
}

@Composable
private fun IconTile(icon: ImageVector, background: Color, tint: Color, size: Dp = 44.dp, radius: Dp = 16.dp, iconSize: Dp = 20.dp) {
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
        modifier = Modifier.fillMaxWidth().height(60.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = c.primary, contentColor = c.onPrimary,
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
        shape = CircleShape,
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
        ButtonDefaults.buttonColors(containerColor = container ?: c.primary, contentColor = c.onPrimary)
    } else {
        ButtonDefaults.buttonColors(containerColor = c.accentSoft, contentColor = c.onAccentSoft)
    }
    val modifier = Modifier.heightIn(min = 40.dp)
    val padding = PaddingValues(horizontal = 14.dp)
    // Unfilled = Material 3 tonal button (soft blueberry), as on the board.
    Button(onClick, modifier, shape = CircleShape, colors = colors, contentPadding = padding) { Text(text, style = RohyText.chip) }
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
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(c.heroCard).padding(vertical = 18.dp, horizontal = 20.dp),
                        contentAlignment = Alignment.Center,
                    ) { DuoIllustration(heart = false) }
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
                                    Text("${index + 1}", style = RohyText.chip.copy(fontSize = RohyText.rowTitle.fontSize.times(0.93f)), color = c.onPrimary)
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
    handleEnabled: Boolean,
    onToggleHandle: (Boolean) -> Unit,
    onOpenReliability: () -> Unit,
    onOpenJournal: () -> Unit,
    onForget: () -> Unit,
) {
    val c = rohyColors
    val connected = content.header is HomeHeader.Connected
    Page {
        AppBar {
            IconButton(onClick = onOpenJournal, modifier = Modifier.size(48.dp).clip(CircleShape).background(c.accentSoft)) {
                Icon(RohyIcons.History, contentDescription = "Journal de connexion", tint = c.onAccentSoft)
            }
        }
        when (val header = content.header) {
            is HomeHeader.Connected -> ConnectedBanner(header.computer, content.activity)
            is HomeHeader.Unreachable -> UnreachableCard(header, onReconnect, onPairAgain = onForget)
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
        if (connected) Shortcuts(onAddTile, handleEnabled, onToggleHandle)
        ReliabilityRow(content.reliability, onOpenReliability)
        ComputerCard(content.computer, content.pairedDetail, onForget)
    }
}

@Composable
private fun ConnectedBanner(computer: String, activity: String?) {
    val c = rohyColors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(c.heroCard)
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { DuoIllustration(heart = true) }
        Row(
            Modifier.clip(CircleShape).background(c.heroDevice).padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Dot(c.success)
            Text("Synchronisation active", style = RohyText.chip, color = c.successText)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(breakable("Connecté à $computer"), style = RohyText.bannerTitle, color = c.onHeroCard, modifier = Modifier.semantics { heading() })
            if (activity != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val icon = if (activity.startsWith("Texte envoyé")) RohyIcons.Sent else RohyIcons.Received
                    Icon(icon, contentDescription = null, tint = c.onHeroCard, modifier = Modifier.size(16.dp))
                    Text(activity, style = RohyText.small, color = c.onHeroCard)
                }
            }
        }
    }
}

/** The phone and the computer smiling at each other (board: home illustration), drawn in the board's 318 x 110 frame. */
@Composable
private fun DuoIllustration(heart: Boolean) {
    val c = rohyColors
    val smallSmile = remember { PathParser().parsePathString("M64 67 q7 6 14 0").toPath() }
    val bigSmile = remember { PathParser().parsePathString("M233 62 q10 8 20 0").toPath() }
    val link = remember { PathParser().parsePathString("M100 62 C 134 18, 180 18, 208 54").toPath() }
    val heartPath = remember {
        PathParser().parsePathString("M154 40 c -7 -5 -13 -9 -13 -15 a 6.5 6.5 0 0 1 13 -2 a 6.5 6.5 0 0 1 13 2 c 0 6 -6 10 -13 15 z").toPath()
    }
    Canvas(Modifier.widthIn(max = 340.dp).fillMaxWidth().aspectRatio(318f / 110f)) {
        val k = size.width / 318f
        withTransform({ scale(k, k, pivot = Offset.Zero) }) {
            drawPath(link, c.primary, style = Stroke(width = 4f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(1f, 10f))))
            if (heart) drawPath(heartPath, c.peach)
            val face = Stroke(width = 2.8f, cap = StrokeCap.Round)
            // Phone.
            drawRoundRect(c.heroDevice, Offset(44f, 16f), Size(54f, 86f), CornerRadius(15f))
            drawRoundRect(c.line, Offset(62f, 22f), Size(18f, 4f), CornerRadius(2f))
            drawCircle(c.ink, 3.6f, Offset(62f, 56f))
            drawCircle(c.ink, 3.6f, Offset(80f, 56f))
            drawPath(smallSmile, c.ink, style = face)
            drawOval(c.peach.copy(alpha = 0.7f), Offset(51f, 62.4f), Size(8f, 5.2f))
            drawOval(c.peach.copy(alpha = 0.7f), Offset(83f, 62.4f), Size(8f, 5.2f))
            // Computer.
            drawRoundRect(c.heroDevice, Offset(196f, 24f), Size(94f, 62f), CornerRadius(13f))
            drawRoundRect(c.heroDevice, Offset(184f, 88f), Size(118f, 12f), CornerRadius(6f))
            drawCircle(c.ink, 3.8f, Offset(231f, 50f))
            drawCircle(c.ink, 3.8f, Offset(255f, 50f))
            drawPath(bigSmile, c.ink, style = face)
            drawOval(c.peach.copy(alpha = 0.7f), Offset(215.6f, 57.2f), Size(8.8f, 5.6f))
            drawOval(c.peach.copy(alpha = 0.7f), Offset(261.6f, 57.2f), Size(8.8f, 5.6f))
        }
    }
}

@Composable
private fun UnreachableCard(header: HomeHeader.Unreachable, onReconnect: () -> Unit, onPairAgain: () -> Unit = {}) {
    val c = rohyColors
    Card {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconTile(RohyIcons.WifiOff, c.neutralSoft, c.muted, size = 44.dp, radius = 22.dp, iconSize = 22.dp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(breakable(header.title), style = RohyText.cardTitle, color = c.ink, modifier = Modifier.semantics { heading() })
                    Text(header.detail, style = RohyText.small, color = c.muted)
                }
            }
            Text(
                if (header.rejected) {
                    "Sur l'ordinateur, l'association a été oubliée. Associe-les à nouveau avec un nouveau QR code."
                } else {
                    "Vérifie que l'ordinateur est allumé, que Rohy y tourne et que vous êtes sur le même Wi-Fi."
                },
                style = RohyText.small.copy(fontSize = RohyText.body.fontSize.times(0.93f)), color = c.muted,
            )
            if (header.canReconnect) SecondaryButton("Reconnecter maintenant", RohyIcons.Refresh, onReconnect, height = 48.dp)
            if (header.rejected) SecondaryButton("Associer à nouveau", RohyIcons.Scan, onPairAgain, height = 48.dp)
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

/** Shape of one row in a Material 3 Expressive grouped list: the ends are rounder than the joints. */
private fun groupShape(index: Int, count: Int): RoundedCornerShape {
    val outer = 26.dp
    val inner = 8.dp
    return RoundedCornerShape(
        topStart = if (index == 0) outer else inner, topEnd = if (index == 0) outer else inner,
        bottomStart = if (index == count - 1) outer else inner, bottomEnd = if (index == count - 1) outer else inner,
    )
}

@Composable
private fun Shortcuts(onAddTile: () -> Unit, handleEnabled: Boolean, onToggleHandle: (Boolean) -> Unit) {
    val c = rohyColors
    @Composable
    fun item(index: Int) = Modifier.fillMaxWidth().clip(groupShape(index, 4)).background(c.surface)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Envoyer plus vite")
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            // The whole row toggles, so TalkBack reads one switch with its label.
            ShortcutRow(
                RohyIcons.Send, "Poignée d'envoi", "Glisse le bord droit vers le centre.",
                modifier = item(0).toggleable(value = handleEnabled, role = Role.Switch, onValueChange = onToggleHandle),
            ) {
                Switch(
                    checked = handleEnabled, onCheckedChange = null,
                    colors = SwitchDefaults.colors(checkedTrackColor = c.primary, checkedThumbColor = c.onPrimary),
                )
            }
            ShortcutRow(
                RohyIcons.Tile, "Tuile des réglages rapides", "Glisse depuis le haut, touche « Envoyer ».",
                tile = c.peachSoft to c.onPeachSoft, modifier = item(1),
            ) {
                PillButton("Ajouter", onAddTile, filled = false)
            }
            ShortcutRow(RohyIcons.TextSelect, "Menu de sélection", "Sélectionne un texte, puis « Envoyer à l'ordinateur ».", modifier = item(2))
            ShortcutRow(
                RohyIcons.Share, "Partager", "Depuis n'importe quelle app : Partager, puis Rohy.",
                tile = c.peachSoft to c.onPeachSoft, modifier = item(3),
            )
        }
    }
}

@Composable
private fun ShortcutRow(
    icon: ImageVector,
    title: String,
    detail: String,
    tile: Pair<Color, Color>? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    val c = rohyColors
    Row(modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        IconTile(icon, tile?.first ?: c.accentSoft, tile?.second ?: c.onAccentSoft)
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
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(c.surface)
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Ordinateur associé")
        Card {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconTile(RohyIcons.Computer, c.accentSoft, c.onAccentSoft)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(computer, style = RohyText.rowTitle, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(detail, style = RohyText.small, color = c.muted)
                }
                TextButton(onClick = { confirm = true }) { Text("Oublier", style = RohyText.smallStrong, color = c.danger) }
            }
        }
    }
}

// Edge handle.

/** Explains the edge handle before Android asks for "Afficher par-dessus les autres apps" (board: BubblePermission). */
@Composable
fun HandlePermissionScreen(onAllow: () -> Unit, onLater: () -> Unit) {
    val c = rohyColors
    Page {
        BackBar("Poignée d'envoi", onLater)
        HandlePreview()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Envoie depuis n'importe quelle app, en un geste", style = RohyText.cardTitle, color = c.ink, modifier = Modifier.semantics { heading() })
            Text(
                "Une fine barre discrète sur le bord de l'écran. Copie un texte, même dans un jeu, glisse la barre vers le centre puis touche « Envoyer » : il arrive sur ton ordinateur.",
                style = RohyText.body, color = c.muted,
            )
        }
        Card {
            Guarantee(RohyIcons.Computer, "Elle n'apparaît que quand ton téléphone est connecté à l'ordinateur.")
            Divider()
            Guarantee(RohyIcons.Shield, "Rohy ne lit le presse-papiers que quand tu touches « Envoyer ».")
            Divider()
            Guarantee(RohyIcons.Close, "Touche ✕ pour la masquer, à tout moment.")
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Android va te demander d'autoriser « Afficher par-dessus les autres apps » pour Rohy.",
                style = RohyText.small, color = c.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton("Autoriser et activer", RohyIcons.Send, onAllow)
            TextButton(onClick = onLater, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Plus tard", style = RohyText.button, color = c.accentText)
            }
        }
    }
}

/** The handle drawn over an app in full screen, as on the board. */
@Composable
private fun HandlePreview() {
    Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF10161F))) {
        Column(
            Modifier.align(Alignment.Center).padding(bottom = 40.dp).width(220.dp).clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1A2433)).padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Code copié", style = RohyText.small, color = Color(0xFFA9B4C4))
            Text("4F7K-92QX", style = RohyText.cardTitle, color = Color.White)
        }
        Box(
            Modifier.align(Alignment.TopEnd).padding(top = 36.dp, end = 4.dp).width(3.dp).height(50.dp).clip(CircleShape)
                .background(Color(0xCCB8B8B8)),
        )
        Row(
            Modifier.align(Alignment.BottomEnd).padding(bottom = 24.dp).height(44.dp)
                .clip(RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp)).background(Brand).padding(start = 6.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconTile(RohyIcons.Send, Color.White.copy(alpha = 0.16f), Color.White, size = 32.dp, radius = 16.dp, iconSize = 16.dp)
            Text("Envoyer à l'ordinateur", style = RohyText.smallStrong, color = Color.White)
        }
    }
}

@Composable
private fun Guarantee(icon: ImageVector, text: String) {
    val c = rohyColors
    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = c.accentText, modifier = Modifier.size(20.dp))
        Text(text, style = RohyText.body, color = c.ink)
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
