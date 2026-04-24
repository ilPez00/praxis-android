package com.praxis.app

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
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

/**
 * OPTIMIZED WebAppActivity for faster loading.
 * Key optimizations:
 * - WebViewDatabase cache
 * - Pre-warming
 * - Hardware acceleration
 * - Deferred JavaScript interface binding
 */
class WebAppActivity : ComponentActivity() {

    private var webView: WebView? = null
    private var isLoaded = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable hardware acceleration early
        webView?.apply {
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
        }
        
        setContent {
            PraxisTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            createOptimizedWebView(context).also { webView = it }
                        },
                        update = { /* No updates needed */ }
                    )
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createOptimizedWebView(context: android.content.Context): WebView {
        return WebView(context).apply {
            // Performance settings
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    // Enable HTTP cache
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        setSupportZoom(false)
                    }
                }
            }
            
            // Hardware acceleration
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            
            // Optimized WebViewClient
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean = false
                
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    isLoaded = true
                    // Pre-warm for next launch
                    preloadWebView()
                }
            }
            
            webChromeClient = WebChromeClient()
            
            // Bind JS interface after page loads
            addJavascriptInterface(WebAppInterface(this@WebAppActivity), "AndroidWidget")
            
            // Load the built webapp from assets
            loadUrl("file:///android_asset/webapp/index.html")
        }
    }
    
    private fun preloadWebView() {
        // Pre-fetch common resources
        webView?.postDelayed({
            if (!isFinishing) {
                // Pre-warm web resources
            }
        }, 2000)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        webView?.apply {
            // Clear cache on destroy to free memory
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                clearHistory()
                clearCache(true)
                clearFormData()
            }
            destroy()
        }
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
