package com.praxis.app

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.praxis.app.ui.theme.PraxisTheme
import com.praxis.app.widget.WidgetDataStore
import org.json.JSONObject
import java.io.File

class WebAppActivity : ComponentActivity() {

    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            PraxisTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = true
                                settings.allowContentAccess = true
                                
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                }
                                
                                webViewClient = WebViewClient()
                                webChromeClient = WebChromeClient()
                                
                                addJavascriptInterface(WebAppInterface(this@WebAppActivity), "AndroidWidget")
                                
                                // Load the built webapp from assets
                                loadUrl("file:///android_asset/webapp/index.html")
                                
                                webView = this
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webView?.destroy()
        webView = null
    }
}

/**
 * JavaScript interface for webapp ↔ native widget communication
 */
class WebAppInterface(private val activity: WebAppActivity) {
    
    @JavascriptInterface
    fun syncWidgetData(jsonData: String) {
        // Parse JSON and update widget cache
        try {
            val data = JSONObject(jsonData)
            val context = activity.applicationContext
            val streak = data.optInt("streak", 0)
            val pp = data.optInt("praxisPoints", 0)
            val quote = data.optString("quote", "")
            val trackers = data.optInt("trackers", 0)
            val lastAxiom = data.optString("lastAxiom", "")
            
            // Create a minimal user object for WidgetDataStore
            val user = com.praxis.app.data.model.User(
                id = "webapp-user",
                name = "User",
                age = 0,
                bio = "",
                currentStreak = streak,
                praxisPoints = pp,
                goalTree = mutableListOf()
            )
            
            WidgetDataStore.save(context, user, quote, trackers, lastAxiom)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    @JavascriptInterface
    fun openNativeScreen(screen: String) {
        // Handle navigation to native screens if needed
    }
}
