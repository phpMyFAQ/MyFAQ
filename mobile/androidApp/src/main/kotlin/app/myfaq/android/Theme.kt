package app.myfaq.android

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

private val DarkColors = darkColorScheme()
private val LightColors = lightColorScheme()

/** User-selectable theme mode, persisted via SharedPreferences. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    val label: String
        get() =
            when (this) {
                SYSTEM -> "System"
                LIGHT -> "Light"
                DARK -> "Dark"
            }
}

/** Observable holder for the current theme preference. */
class ThemeState(
    prefs: SharedPreferences,
) {
    private val prefs = prefs

    var mode: ThemeMode by mutableStateOf(load())
        private set

    fun setMode(newMode: ThemeMode) {
        mode = newMode
        prefs.edit().putString(KEY, newMode.name).apply()
    }

    private fun load(): ThemeMode =
        try {
            ThemeMode.valueOf(prefs.getString(KEY, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        } catch (_: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }

    companion object {
        private const val KEY = "theme_mode"
        private const val PREFS_NAME = "app.myfaq.settings"

        fun create(context: Context): ThemeState =
            ThemeState(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
    }
}

/** CompositionLocal so any descendant can read/change the theme. */
val LocalThemeState = compositionLocalOf<ThemeState> { error("No ThemeState provided") }

@Composable
fun MyFaqAppTheme(
    themeState: ThemeState,
    content: @Composable () -> Unit,
) {
    val darkTheme =
        when (themeState.mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
