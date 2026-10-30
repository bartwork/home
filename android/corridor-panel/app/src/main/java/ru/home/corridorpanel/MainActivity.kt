package ru.home.corridorpanel

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import ru.home.corridorpanel.ui.CorridorScreen
import ru.home.corridorpanel.ui.LocalClickSound
import ru.home.corridorpanel.ui.asGate
import ru.home.corridorpanel.ui.theme.CorridorPanelTheme

class MainActivity : ComponentActivity() {
    private val vm: CorridorViewModel by viewModels()
    private lateinit var ambient: AmbientBrightness
    private lateinit var clicks: ClickFeedback

    private val runtimePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        vm.startSip()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ambient = AmbientBrightness(this)
        clicks = ClickFeedback(this)

        vm.setLightSensorAvailable(ambient.available)
        syncDisplayFromState()
        ensureRuntimePermissionsOrStartSip()

        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        setContent {
            SideEffect { hideSystemBars() }
            val state by vm.uiState.collectAsStateWithLifecycle()
            CorridorPanelTheme(mode = state.themeMode) {
                val clickEnabled = rememberUpdatedState(state.clickSound)
                var prevClickSound by remember { mutableStateOf<Boolean?>(null) }
                var blanked by remember { mutableStateOf(false) }
                val blankedRef = rememberUpdatedState(blanked)

                LaunchedEffect(state.autoBrightness, state.manualBrightness) {
                    ambient.setAutoEnabled(state.autoBrightness)
                    ambient.setManualBrightness(state.manualBrightness)
                }
                LaunchedEffect(blanked) {
                    if (blanked) {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    ambient.setBlanked(blanked)
                }
                // входящий / исходящий звонок будит экран
                LaunchedEffect(state.sip.isInCall) {
                    if (state.sip.isInCall) blanked = false
                }
                LaunchedEffect(state.clickSound) {
                    clicks.setEnabled(state.clickSound)
                    if (state.clickSound && prevClickSound == false) {
                        delay(40)
                        if (clickEnabled.value) clicks.play()
                    }
                    prevClickSound = state.clickSound
                }

                // касание, разбудившее экран, сразу включает панель
                DisposableEffect(Unit) {
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context?, intent: Intent?) {
                            if (blankedRef.value) blanked = false
                        }
                    }
                    val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
                    if (Build.VERSION.SDK_INT >= 33) {
                        registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
                    } else {
                        @Suppress("UnspecifiedRegisterReceiverFlag")
                        registerReceiver(receiver, filter)
                    }
                    onDispose { unregisterReceiver(receiver) }
                }

                val gate = remember(clicks) {
                    clicks.asGate { clickEnabled.value }
                }
                CompositionLocalProvider(LocalClickSound provides gate) {
                    Box(Modifier.fillMaxSize()) {
                        CorridorScreen(
                            state = state,
                            vm = vm,
                            onBlankScreen = {
                                if (state.sip.isInCall) return@CorridorScreen
                                if (!Settings.System.canWrite(this@MainActivity)) {
                                    ambient.ensureWriteSettings(this@MainActivity)
                                }
                                blanked = true
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (blanked && !state.sip.isInCall) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) { blanked = false },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun ensureRuntimePermissionsOrStartSip() {
        val need = listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
            .filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
        if (need.isNotEmpty()) {
            runtimePermissions.launch(need.toTypedArray())
        } else {
            vm.startSip()
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
