package ru.home.kitchenpanel

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import ru.home.kitchenpanel.ui.KitchenScreen
import ru.home.kitchenpanel.ui.LocalClickSound
import ru.home.kitchenpanel.ui.asGate
import ru.home.kitchenpanel.ui.theme.KitchenPanelTheme

class MainActivity : ComponentActivity() {
    private val vm: KitchenViewModel by viewModels()
    private lateinit var ambient: AmbientBrightness
    private lateinit var clicks: ClickFeedback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ambient = AmbientBrightness(this)
        clicks = ClickFeedback(this)

        vm.setLightSensorAvailable(ambient.available)
        syncDisplayFromState()

        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        setContent {
            SideEffect { hideSystemBars() }
            KitchenPanelTheme {
                val state by vm.uiState.collectAsStateWithLifecycle()
                val clickEnabled = rememberUpdatedState(state.clickSound)
                var prevClickSound by remember { mutableStateOf<Boolean?>(null) }

                LaunchedEffect(state.autoBrightness, state.manualBrightness) {
                    ambient.setAutoEnabled(state.autoBrightness)
                    ambient.setManualBrightness(state.manualBrightness)
                }
                LaunchedEffect(state.clickSound) {
                    clicks.setEnabled(state.clickSound)
                    // звук только при ручном включении, не при каждом старте Activity
                    if (state.clickSound && prevClickSound == false) {
                        delay(40)
                        if (clickEnabled.value) clicks.play()
                    }
                    prevClickSound = state.clickSound
                }

                val gate = remember(clicks) {
                    clicks.asGate { clickEnabled.value }
                }
                CompositionLocalProvider(LocalClickSound provides gate) {
                    KitchenScreen(
                        state = state,
                        vm = vm,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        syncDisplayFromState()
        ambient.start()
    }

    override fun onPause() {
        ambient.stop()
        super.onPause()
    }

    override fun onDestroy() {
        ambient.release()
        clicks.release()
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun syncDisplayFromState() {
        val s = vm.uiState.value
        ambient.setAutoEnabled(s.autoBrightness)
        ambient.setManualBrightness(s.manualBrightness)
        clicks.setEnabled(s.clickSound)
    }

    private fun hideSystemBars(root: View = window.decorView) {
        val controller = WindowInsetsControllerCompat(window, root)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
