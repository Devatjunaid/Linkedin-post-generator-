package com.superdevlabs.petfood.portioncalc.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.superdevlabs.petfood.portioncalc.AppContainer
import com.superdevlabs.petfood.portioncalc.R
import com.superdevlabs.petfood.portioncalc.data.WikiSummary
import java.io.File

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }

object Brand {
    val Navy = Color(0xFF1C2541)
    val Orange = Color(0xFFFF6B2C)
    val OrangeText = Color(0xFFB93F0B)
    val Mint = Color(0xFF2BB59A)
    val MintText = Color(0xFF0F6B5C)
    val Peach = Color(0xFFFFE3CF)
    val MintSoft = Color(0xFFD9F3EC)
    val Sky = Color(0xFFDCEBFF)
    val Butter = Color(0xFFFFF0C7)
}

private val LightColors: ColorScheme = lightColorScheme(
    primary = Brand.Navy,
    onPrimary = Color.White,
    primaryContainer = Brand.Peach,
    onPrimaryContainer = Color(0xFF4A2A12),
    secondary = Brand.MintText,
    onSecondary = Color.White,
    secondaryContainer = Brand.MintSoft,
    onSecondaryContainer = Color(0xFF0B3D34),
    tertiary = Brand.OrangeText,
    tertiaryContainer = Brand.Butter,
    background = Color(0xFFF4F6FB),
    onBackground = Brand.Navy,
    surface = Color.White,
    onSurface = Brand.Navy,
    surfaceVariant = Color(0xFFEEF1F7),
    onSurfaceVariant = Color(0xFF4F5870),
    outline = Color(0xFFC3CADB),
    outlineVariant = Color(0xFFE1E5EE),
    error = Color(0xFFB3261E),
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFFFF8A50),
    onPrimary = Brand.Navy,
    primaryContainer = Color(0xFF4A2A12),
    onPrimaryContainer = Brand.Peach,
    secondary = Color(0xFF6FE0C4),
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF0E4A3F),
    onSecondaryContainer = Brand.MintSoft,
    tertiary = Color(0xFFFFB68F),
    tertiaryContainer = Color(0xFF4A3B12),
    background = Color(0xFF10141F),
    onBackground = Color(0xFFEEF1F8),
    surface = Color(0xFF1B2133),
    onSurface = Color(0xFFEEF1F8),
    surfaceVariant = Color(0xFF262D42),
    onSurfaceVariant = Color(0xFFB5BDD0),
    outline = Color(0xFF4A5470),
    outlineVariant = Color(0xFF2C3550),
)

@Composable
fun PetFoodTheme(content: @Composable () -> Unit) {
    val base = MaterialTheme.typography
    val typography = base.copy(
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Bold),
    )
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = typography,
        content = content,
    )
}

@Composable
fun AppIcon(@DrawableRes id: Int, contentDescription: String?, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.onSurface) {
    Icon(painterResource(id), contentDescription, modifier.size(22.dp), tint = tint)
}

/** Simple top bar that pads itself below the status bar (edge-to-edge). */
@Composable
fun AppTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) { AppIcon(R.drawable.ic_back, "Back") }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        actions()
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    shape: Shape = RoundedCornerShape(24.dp),
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, @DrawableRes icon: Int? = null) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        if (icon != null) {
            Icon(painterResource(icon), null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, @DrawableRes icon: Int? = null) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
    ) {
        if (icon != null) {
            Icon(painterResource(icon), null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Minus / value / plus control with 48dp touch targets. */
@Composable
fun Stepper(value: String, onMinus: () -> Unit, onPlus: () -> Unit, minusLabel: String, plusLabel: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalIconButton(onClick = onMinus, modifier = Modifier.size(48.dp), colors = IconButtonDefaults.filledTonalIconButtonColors()) {
            Icon(painterResource(R.drawable.ic_minus), minusLabel, Modifier.size(20.dp))
        }
        Text(value, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f, fill = false))
        FilledTonalIconButton(onClick = onPlus, modifier = Modifier.size(48.dp)) {
            Icon(painterResource(R.drawable.ic_add), plusLabel, Modifier.size(20.dp))
        }
    }
}

@Composable
fun Pill(text: String, color: Color = MaterialTheme.colorScheme.secondaryContainer, textColor: Color = MaterialTheme.colorScheme.onSecondaryContainer) {
    Text(
        text,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(color).padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = textColor,
    )
}

@Composable
fun rememberWikiSummary(title: String?): WikiSummary? {
    val wiki = LocalAppContainer.current.wiki
    val state by produceState<WikiSummary?>(initialValue = null, title) {
        value = title?.let { wiki.summary(it) }
    }
    return state
}

/**
 * A real photo: the user's own picture when set, otherwise the Wikipedia photo of [wikiTitle].
 * Loaded by Coil (downsampling + caching); shows a paw placeholder while loading or offline.
 */
@Composable
fun RealPhoto(
    wikiTitle: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    localPhotoPath: String? = null,
    size: Dp? = null,
    shape: Shape = CircleShape,
) {
    val summary = if (localPhotoPath == null) rememberWikiSummary(wikiTitle) else null
    val model: Any? = localPhotoPath?.let { File(it) } ?: summary?.imageUrl
    val sized = if (size != null) modifier.size(size) else modifier
    Box(sized.clip(shape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            placeholder = painterResource(R.drawable.ic_photo_placeholder),
            error = painterResource(R.drawable.ic_photo_placeholder),
            fallback = painterResource(R.drawable.ic_photo_placeholder),
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
    }
}

fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
    }
}

/** Opens this app's Play Store page (Play app first, browser as fallback). */
fun openPlayStore(context: Context) {
    val pkg = context.packageName
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$pkg".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        openUrl(context, "https://play.google.com/store/apps/details?id=$pkg")
    }
}

fun shareApp(context: Context) {
    val pkg = context.packageName
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(
        Intent.EXTRA_TEXT,
        "I use Pet Food Calculator to work out exact dog & cat food portions: https://play.google.com/store/apps/details?id=$pkg",
    )
    context.startActivity(Intent.createChooser(send, "Share app").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

