package ru.home.kitchenpanel.ui

import androidx.compose.runtime.staticCompositionLocalOf
import ru.home.kitchenpanel.ClickFeedback

val LocalClickSound = staticCompositionLocalOf { ClickSoundGate.NoOp }

fun interface ClickSoundGate {
    fun play()

    companion object {
        val NoOp = ClickSoundGate { }
    }
}

fun ClickFeedback.asGate(isEnabled: () -> Boolean): ClickSoundGate =
    ClickSoundGate {
        if (isEnabled()) play()
    }
