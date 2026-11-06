package com.tanqueo.ui.components

import android.app.RemoteInput
import android.content.ActivityNotFoundException
import android.view.inputmethod.EditorInfo
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.wear.input.RemoteInputIntentHelper
import androidx.wear.input.wearableExtender

private const val INPUT_KEY = "texto"

/**
 * Abre la entrada del sistema de Wear OS (voz o teclado) y entrega el texto, o null si se canceló.
 * Se usa RemoteInput porque el reconocedor de voz del reloj no devuelve el texto por RecognizerIntent.
 */
@Composable
fun rememberTextInputLauncher(prompt: String, onResult: (String?) -> Unit, onUnavailable: () -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = result.data?.let { RemoteInput.getResultsFromIntent(it) }?.getCharSequence(INPUT_KEY)
        onResult(text?.toString())
    }
    return {
        val input = RemoteInput.Builder(INPUT_KEY)
            .setLabel(prompt)
            .wearableExtender {
                setEmojisAllowed(false)
                setInputActionType(EditorInfo.IME_ACTION_DONE)
            }
            .build()
        val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
        RemoteInputIntentHelper.putRemoteInputsExtra(intent, listOf(input))
        try {
            launcher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            onUnavailable()
        }
    }
}
