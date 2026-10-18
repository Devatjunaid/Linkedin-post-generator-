package com.inkformlabs.pdfsigner.ui.home

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.inkformlabs.pdfsigner.ads.NativeAdCard
import com.inkformlabs.pdfsigner.data.db.SignatureEntity
import com.inkformlabs.pdfsigner.ui.LocalAppContainer
import com.inkformlabs.pdfsigner.ui.components.ButtonKind
import com.inkformlabs.pdfsigner.ui.components.InkButton
import com.inkformlabs.pdfsigner.ui.components.InkCard
import com.inkformlabs.pdfsigner.ui.signature.SignatureThumb
import com.inkformlabs.pdfsigner.ui.theme.InkIcons
import com.inkformlabs.pdfsigner.ui.theme.LocalInk
import kotlinx.coroutines.launch

@Composable
fun SignaturesTab(onCreateSignature: () -> Unit, onCreateInitials: () -> Unit) {
    val p = LocalInk.current
    val repo = LocalAppContainer.current.signatures
    val signatures by repo.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var toDelete by remember { mutableStateOf<SignatureEntity?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Signatures", style = MaterialTheme.typography.headlineSmall, color = p.ink)
                Text(
                    "Stored privately on this phone, ready to place with one tap.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = p.muted,
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InkButton("New signature", onClick = onCreateSignature, icon = InkIcons.Signature, modifier = Modifier.weight(1f))
                InkButton("Initials", onClick = onCreateInitials, kind = ButtonKind.Secondary, icon = InkIcons.Pen, modifier = Modifier.weight(1f))
            }
        }
        if (signatures.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                InkCard {
                    Text(
                        "No signatures yet. Draw one with your finger, type it in a handwriting style, or import a photo of your signature on paper.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = p.muted,
                    )
                }
            }
        }
        items(signatures, key = { it.id }) { sig ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.6f)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, p.line, RoundedCornerShape(18.dp)),
            ) {
                SignatureThumb(sig, Modifier.fillMaxSize())
                Text(
                    if (sig.kind == "initials") "Initials" else "Signature",
                    style = MaterialTheme.typography.labelSmall,
                    color = PaperText,
                    modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                )
                IconButton(onClick = { toDelete = sig }, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(InkIcons.Trash, contentDescription = "Delete signature", tint = PaperText)
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { NativeAdCard() }
    }

    toDelete?.let { sig ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete this ${if (sig.kind == "initials") "initials" else "signature"}?") },
            text = { Text("Documents you already signed keep it. This only removes the saved copy.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repo.delete(sig) }
                    toDelete = null
                }) { Text("Delete", color = p.hi) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel", color = p.ink) } },
            containerColor = p.surface,
        )
    }
}

/** Dark grey readable on the always-light signature paper. */
private val PaperText = androidx.compose.ui.graphics.Color(0xFF55534A)
