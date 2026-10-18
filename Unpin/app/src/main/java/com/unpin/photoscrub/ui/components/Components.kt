package com.unpin.photoscrub.ui.components

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.unpin.photoscrub.ui.theme.Card
import com.unpin.photoscrub.ui.theme.Ink
import com.unpin.photoscrub.ui.theme.Line
import com.unpin.photoscrub.ui.theme.LineSoft
import com.unpin.photoscrub.ui.theme.Muted
import com.unpin.photoscrub.ui.theme.Paper
import com.unpin.photoscrub.ui.theme.Signal
import com.unpin.photoscrub.ui.theme.UnpinText

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    container: Color = Signal,
    content: Color = Ink,
    iconTint: Color = content,
    enabled: Boolean = true,
) {
    Row(
        modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) container else container.copy(alpha = 0.4f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = UnpinText.button, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Card)
            .border(1.dp, Line, RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = Ink, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = UnpinText.button.copy(fontSize = 15.sp), color = Ink, maxLines = 1)
    }
}

@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bordered: Boolean = true,
    tint: Color = Ink,
) {
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .then(if (bordered) Modifier.background(Card).border(1.dp, Line, CircleShape) else Modifier)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun UnpinSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String, modifier: Modifier = Modifier) {
    val track by animateColorAsState(if (checked) Ink else Color(0xFFCFC9BB), label = "track")
    val knob by animateDpAsState(if (checked) 24.dp else 4.dp, label = "knob")
    Box(
        modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(track)
            .semantics { contentDescription = label }
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    ) {
        Box(
            Modifier
                .offset(x = knob, y = 4.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Card)
        )
    }
}

@Composable
fun UnpinCheck(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier) {
    val bg by animateColorAsState(if (checked) Ink else Card, label = "check")
    Box(
        modifier
            .size(44.dp)
            .then(
                if (onCheckedChange != null) Modifier.toggleable(checked, role = Role.Checkbox, onValueChange = onCheckedChange)
                else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(bg)
                .border(2.dp, if (checked) Ink else Color(0xFFBDB6A6), RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(UnpinIcons.Check, null, tint = Paper, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = Muted) {
    Text(text.uppercase(), style = UnpinText.label, color = color, modifier = modifier)
}

@Composable
fun CardBox(modifier: Modifier = Modifier, padding: Dp = 16.dp, content: @Composable () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Card)
            .border(BorderStroke(1.dp, Line), RoundedCornerShape(22.dp))
            .padding(horizontal = padding),
    ) { content() }
}

@Composable
fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit, showDivider: Boolean) {
    Column {
        if (showDivider) Box(Modifier.fillMaxWidth().height(1.dp).background(LineSoft))
        Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = UnpinText.bodyStrong, color = Ink)
                Text(subtitle, style = UnpinText.small, color = Muted)
            }
            Spacer(Modifier.width(12.dp))
            UnpinSwitch(checked, onChange, title)
        }
    }
}

@Composable
fun PhotoThumb(uri: Uri, modifier: Modifier = Modifier, corner: Dp = 16.dp) {
    Box(modifier.clip(RoundedCornerShape(corner)).background(Color(0xFFD8D2C4))) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun Tag(text: String, background: Color, color: Color, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier.clip(RoundedCornerShape(8.dp)).background(background).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(text, style = UnpinText.label.copy(letterSpacing = 0.5.sp), color = color)
    }
}

@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (highlight) Signal else Card)
            .then(if (highlight) Modifier else Modifier.border(1.dp, Line, RoundedCornerShape(18.dp)))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(value, style = UnpinText.number, color = Ink, maxLines = 1)
        Text(label, style = UnpinText.small, color = if (highlight) Ink else Muted)
    }
}

@Composable
fun RowScope.NavItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .weight(1f)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier
                .size(width = 56.dp, height = 30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(if (selected) Ink else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = if (selected) Paper else Muted, modifier = Modifier.size(20.dp))
        }
        Text(label, style = UnpinText.small.copy(fontSize = 11.5.sp), color = if (selected) Ink else Muted)
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        LiveLogoMark(Modifier.size(96.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, style = UnpinText.title, color = Ink)
        Spacer(Modifier.height(6.dp))
        Text(body, style = UnpinText.body, color = Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        action()
    }
}

