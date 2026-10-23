package com.walkies.dogwalk.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import com.walkies.dogwalk.R
import com.walkies.dogwalk.ui.theme.WalkiesColors

/**
 * The user's own dog photo, or the bundled default (res/drawable-nodpi/dog_default.webp).
 * Loaded through Coil, which downsamples to the view size, caches and decodes off the main thread.
 */
@Composable
fun DogPhoto(
    photoUri: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = photoUri ?: R.drawable.dog_default,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        placeholder = ColorPainter(WalkiesColors.ForestSoft),
        error = painterResource(R.drawable.dog_default),
    )
}

/**
 * Android photo picker: no storage permission is needed, and the user only shares the one
 * photo they choose. Read access is kept across restarts where the picker allows it.
 */
@Composable
fun rememberDogPhotoPicker(onPicked: (Uri) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
                // Some providers don't offer persistable grants; the photo still shows this session.
            }
            onPicked(uri)
        }
    }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}
