package app.layerline.toolkit3d.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.layerline.toolkit3d.LayerlineApp
import app.layerline.toolkit3d.data.Repository

/** Builds a ViewModel factory that hands the shared [Repository] to the ViewModel. */
inline fun <reified VM : ViewModel> repoFactory(crossinline create: (Repository) -> VM): ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as LayerlineApp
            create(app.repository)
        }
    }

/** Everything a screen needs from the app shell. */
class ScreenContext(
    val app: LayerlineApp,
    val open: (String) -> Unit,
    val back: () -> Unit,
    /** Call after a major action completes; may show an interstitial (capped to 1 per 30 s). */
    val majorAction: () -> Unit,
)

/** Scrolling screen body with the standard side gutter and status-bar inset. */
@Composable
fun ScreenScroll(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        content()
        Spacer(Modifier.height(28.dp))
    }
}
