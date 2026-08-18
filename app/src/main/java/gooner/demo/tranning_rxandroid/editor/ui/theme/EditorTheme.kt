package gooner.demo.tranning_rxandroid.editor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EditorColorScheme = darkColorScheme(
    primary = Color(0xFFFFD54F),
    onPrimary = Color(0xFF1A1A1A),
    secondary = Color(0xFFFFD54F),
    onSecondary = Color(0xFF1A1A1A),
    background = Color(0xFF121212),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF262626),
    onSurfaceVariant = Color(0xFFB3B3B3),
    outline = Color(0xFF4D4D4D)
)

/** The dark palette the photo editor is drawn in. */
@Composable
fun EditorTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = EditorColorScheme, content = content)
}
