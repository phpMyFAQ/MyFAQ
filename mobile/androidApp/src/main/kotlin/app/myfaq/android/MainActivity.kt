package app.myfaq.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val themeState = ThemeState.create(this)

        setContent {
            CompositionLocalProvider(LocalThemeState provides themeState) {
                MyFaqAppTheme(themeState = themeState) {
                    MyFaqNavHost()
                }
            }
        }
    }
}
