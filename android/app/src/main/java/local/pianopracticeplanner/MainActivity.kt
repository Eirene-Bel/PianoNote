package local.pianopracticeplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import local.pianopracticeplanner.ui.*

class MainActivity: ComponentActivity() {
    private val model: PianoViewModel by viewModels {
        viewModelFactory { initializer { PianoViewModel((application as PianoApplication).repository,createSavedStateHandle(),local.pianopracticeplanner.data.CardPreferences(application),audio=local.pianopracticeplanner.audio.NoteAudioPlayer(application),outbox=local.pianopracticeplanner.data.ProgressOutboxStore(application)) } }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        local.pianopracticeplanner.i18n.I18n.language = local.pianopracticeplanner.i18n.I18n.resolve(resources.configuration.locales[0].toLanguageTag())
        model.refreshLanguage()
        enableEdgeToEdge()
        setContent { PianoApp(model) }
    }
}
