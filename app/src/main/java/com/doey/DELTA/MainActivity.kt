package com.doey.DELTA

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.doey.VAULT.ProfileStore
import com.doey.DELTA.core.DoeyApp

class MainActivity : ComponentActivity() {

    companion object {
        var instance: MainActivity? = null
    }

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* results handled at runtime */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this

        // Habilitar diseño de borde a borde para eliminar barras de sistema de colores fijos
        enableEdgeToEdge()

        // Solo solicitar permisos básicos en runtime si el onboarding ya fue completado
        val profileStore = ProfileStore(this)
        if (profileStore.isOnboardingDone()) {
            requestEssentialPermissions()
        }

        setContent { DoeyApp() }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)

        // Manejar invocación desde asistente del sistema (FIX BUG-6)
        if (intent?.action == Intent.ACTION_ASSIST ||
            intent?.action == Intent.ACTION_VOICE_COMMAND ||
            intent?.getBooleanExtra("from_assistant", false) == true) {
            // Marcar para que DoeyApp inicie escucha automáticamente
            intent?.putExtra("auto_listen", true)
        }

        // Manejar query del Modo Friendly
        val friendlyQuery = intent?.getStringExtra("friendly_query")
        if (!friendlyQuery.isNullOrBlank()) {
            intent?.putExtra("pending_query", friendlyQuery)
        }

        // Manejar voz del Modo Friendly
        if (intent?.getBooleanExtra("friendly_voice", false) == true) {
            intent?.putExtra("auto_listen", true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
    }

    private fun requestEssentialPermissions() {
        val needed = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            needed.add(Manifest.permission.POST_NOTIFICATIONS)

        val missing = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) permLauncher.launch(missing.toTypedArray())
    }
}
