package gooner.demo.tranning_rxandroid.editor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import gooner.demo.tranning_rxandroid.editor.ui.PhotoEditorScreen
import gooner.demo.tranning_rxandroid.editor.ui.theme.EditorTheme

/**
 * A small photo editor: pick a picture, recolour it with a filter or the manual
 * adjustments, drop captions and stickers on it, attach a sound, then save or share
 * the result.
 *
 * The screen is entirely Jetpack Compose; the editing session lives in
 * [PhotoEditorViewModel] so it survives rotation, and every slow step is a coroutine.
 */
class PhotoEditorActivity : ComponentActivity() {

    private val viewModel: PhotoEditorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EditorTheme {
                PhotoEditorScreen(viewModel)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.pauseSound()
    }
}
