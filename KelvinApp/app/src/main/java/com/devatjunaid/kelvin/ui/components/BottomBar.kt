package com.devatjunaid.kelvin.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.devatjunaid.kelvin.ui.theme.K

enum class Tab(val label: String, val icon: ImageVector) {
    Home("Today", KIcons.Home),
    Protocols("Protocols", KIcons.Sliders),
    Log("Log", KIcons.Chart),
    Settings("Settings", KIcons.Settings),
}

/**
 * Floating pill navigation. A soft "liquid" glow slides under the selected tab with a spring,
 * and the raised centre button (spinning thermal rim) opens Quick Start.
 */
@Composable
fun KelvinBottomBar(
    selected: Tab,
    onSelect: (Tab) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val slots = listOf(Tab.Home, Tab.Protocols, null, Tab.Log, Tab.Settings)
    Box(modifier.fillMaxWidth().height(92.dp), contentAlignment = Alignment.BottomCenter) {
        BoxWithConstraints(
            Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 10.dp)
                .fillMaxWidth()
                .height(68.dp)
                .shadow(24.dp, RoundedCornerShape(34.dp), ambientColor = Color.Black, spotColor = Color.Black)
                .clip(RoundedCornerShape(34.dp))
                .background(Color(0xF2131A22))
                .border(1.dp, K.Line, RoundedCornerShape(34.dp)),
        ) {
            val slotWidth = maxWidth / slots.size
            val selectedIndex = slots.indexOf(selected).coerceAtLeast(0)
            val glowX by animateDpAsState(slotWidth * selectedIndex, spring(dampingRatio = 0.62f, stiffness = 380f), label = "glow")
            Box(
                Modifier
                    .offset(x = glowX)
                    .size(slotWidth, 68.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(K.Ice.copy(alpha = 0.28f), Color.Transparent))),
                )
            }
            Row(Modifier.fillMaxSize()) {
                slots.forEach { tab ->
                    Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (tab != null) {
                            val isSelected = tab == selected
                            val tint by animateColorAsState(if (isSelected) K.Ice else K.Muted, label = "tint")
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .semantics { this.selected = isSelected }
                                    .pressable(onClick = { onSelect(tab) }, role = Role.Tab, label = tab.label),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(tab.icon, contentDescription = tab.label, tint = tint, modifier = Modifier.size(23.dp))
                            }
                        }
                    }
                }
            }
        }
        StartOrb(onClick = onStart, modifier = Modifier.padding(bottom = 36.dp))
    }
}

@Composable
private fun StartOrb(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val loop = rememberInfiniteTransition(label = "startOrb")
    val spin by loop.animateFloat(0f, 360f, infiniteRepeatable(tween(6_000, easing = LinearEasing)), label = "spin")
    Box(
        modifier
            .size(64.dp)
            .shadow(18.dp, CircleShape, ambientColor = K.Ice, spotColor = K.Ember)
            .clip(CircleShape)
            .pressable(onClick = onClick, label = "Quick start a session"),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            rotate(spin) { drawCircle(Brush.sweepGradient(listOf(K.Ice, K.IceDeep, K.EmberDeep, K.Ember, K.Ice))) }
            drawCircle(K.Ink, radius = size.minDimension / 2f - 3.dp.toPx())
        }
        Icon(KIcons.Thermal, contentDescription = "Quick start a session", tint = K.Text, modifier = Modifier.size(26.dp))
    }
}
