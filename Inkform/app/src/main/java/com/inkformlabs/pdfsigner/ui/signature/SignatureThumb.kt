package com.inkformlabs.pdfsigner.ui.signature

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.inkformlabs.pdfsigner.data.db.SignatureEntity
import com.inkformlabs.pdfsigner.ui.LocalAppContainer
import com.inkformlabs.pdfsigner.ui.theme.Paper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A saved signature drawn on a small piece of "paper". */
@Composable
fun SignatureThumb(signature: SignatureEntity, modifier: Modifier = Modifier) {
    val repo = LocalAppContainer.current.signatures
    val bitmap by produceState<ImageBitmap?>(initialValue = null, signature.id) {
        value = withContext(Dispatchers.IO) {
            val file = repo.file(signature)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
        }
    }
    Box(modifier.background(Paper.sheet), contentAlignment = Alignment.Center) {
        bitmap?.let {
            Image(
                it,
                contentDescription = if (signature.kind == "initials") "Saved initials" else "Saved signature",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(10.dp),
            )
        }
    }
}
