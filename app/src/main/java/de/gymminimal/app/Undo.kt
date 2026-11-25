package de.gymminimal.app

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Rückgängig statt Nachfrage: Einfache Löschungen passieren sofort, unten erscheint kurz „Rückgängig“.
 * Endgültiges (Phase löschen, alles zurücksetzen, Backup importieren) fragt weiterhin nach.
 */
class UndoController(private val host: SnackbarHostState, private val scope: CoroutineScope) {
    fun offer(message: String, undo: () -> Unit) {
        scope.launch {
            host.currentSnackbarData?.dismiss()
            val result = host.showSnackbar(message, actionLabel = "Rückgängig", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) undo()
        }
    }
}

val LocalUndo = staticCompositionLocalOf<UndoController?> { null }
