package com.praxis.android.ui.web

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

/** The deployed web app the shell mirrors. */
const val WEB_APP_URL = "https://praxisweb.xyz/"

/** Consent-gated phone/app monitor preference (mirrors SettingsViewModel). */
private val Context.praxisDataStore by preferencesDataStore(name = "praxis_settings")
private val SHARE_PHONE_USAGE_KEY = booleanPreferencesKey("share_phone_usage_enabled")

private const val STARTUP_TIMEOUT_MS = 12_000L

/**
 * Map an internal deep-link route (widget taps, OAuth returns, share sends)
 * onto the equivalent web URL so the shell lands on the same screen the
 * native NavHost would have shown.
 */
fun urlForRoute(route: String): String {
    val path = route.substringBefore('?')
    val query = route.substringAfter('?', "")
    val base = when (path) {
        // The web app's home IS the daily loop; "checkin" has no dedicated page.
        "", "home", "checkin" -> "/"
        "capture/audio" -> "/capture"
        "capture/video" -> "/capture"
        "capture/photo" -> "/capture"
        "auth-callback" -> "/auth/callback"
        else -> "/$path"
    }
    return buildString {
        append(WEB_APP_URL.dropLast(1))
        append(base)
        if (query.isNotEmpty()) append("?$query")
    }
}

/**
 * Full-screen WebView shell that mirrors praxisweb.xyz — pixel parity with
 * the PWA without shipping a second UI implementation. Falls back to the
 * native Compose screens when the server cannot be reached.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebScreen(
    pendingRoute: String?,
    onRouteConsumed: () -> Unit,
    onUseNativeUi: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var offline by remember { mutableStateOf(false) }

    // Live connectivity: the startup timeout below only catches a hung main
    // frame, but a service-worker-served shell with a dead network stalls
    // AFTER onPageCommitVisible (lazy chunks fail to fetch) — the only honest
    // signal left is the network itself.
    val connectivity = remember { com.praxis.android.util.NetworkConnectivityObserver(context) }
    val isOnline by connectivity.isConnected.collectAsState(initial = true)

    // If the page never fires onPageFinished (hung network, OEM WebView jank),
    // surface the offline panel instead of an eternal blank screen.
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(STARTUP_TIMEOUT_MS)
        if (loading && !offline) {
            offline = true
            loading = false
        }
    }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.loadsImagesAutomatically = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(false)
            // Keep Google's "accounts you already use" working across sessions.
            settings.saveFormData = true
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            // Google blocks OAuth in WebView UAs ("disallowed_useragent") and
            // won't remember previously-granted accounts; an Android-Chrome UA
            // gets the normal account-picker flow with remembered grants while
            // keeping the mobile layout.
            settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
            // Dark backdrop: no white flash while the SPA boots. #080808 =
            // the web app's --color-bg, so boot matches the rendered page.
            setBackgroundColor(android.graphics.Color.rgb(8, 8, 8))
            // The shell mirrors the web app, which lets users attach photos /
            // videos (and the picture→log Pro feature) through <input type=file
            // capture>. A WebView needs a WebChromeClient with
            // filePathCallback wiring or those pickers silently do nothing.
            webChromeClient = object : android.webkit.WebChromeClient() {
                override fun onShowFileChooser(
                    view: WebView?,
                    filePathCallback: android.webkit.ValueCallback<Array<Uri>>?,
                    fileChooserParams: android.webkit.WebChromeClient.FileChooserParams?,
                ): Boolean {
                    com.praxis.android.ui.web.WebFileChooser.pendingCallback = filePathCallback
                    val intent = fileChooserParams?.createIntent()
                    try {
                        val act = (context as? androidx.activity.ComponentActivity)
                            ?: (view?.context as? androidx.activity.ComponentActivity)
                        if (act != null && intent != null) {
                            act.startActivityForResult(intent, com.praxis.android.ui.web.WebFileChooser.RESULT_CODE)
                        } else {
                            filePathCallback?.onReceiveValue(null)
                        }
                    } catch (e: Exception) {
                        filePathCallback?.onReceiveValue(null)
                        com.praxis.android.ui.web.WebFileChooser.pendingCallback = null
                    }
                    return true
                }
            }
            // The shell is always dark; make CSS media queries agree so the
            // page header/nav render the dark theme instead of a light band.
            if (androidx.webkit.WebViewFeature.isFeatureSupported(
                    androidx.webkit.WebViewFeature.ALGORITHMIC_DARKENING,
                )
            ) {
                androidx.webkit.WebSettingsCompat.setAlgorithmicDarkeningAllowed(
                    settings, true,
                )
            }
            settings.mixedContentMode =
                android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            // Generic phone/app monitor: when the user enables "Share phone usage"
            // in Settings, inject window.PraxisNative so the web app can read the
            // on-device screen-time (per-app UsageStats) and Health Connect data.
            // Without consent the object is simply absent and the web app falls
            // back to its other oracle paths — nothing is read or uploaded.
            val shareEnabled = runBlocking {
                context.praxisDataStore.data.first()[SHARE_PHONE_USAGE_KEY] ?: false
            }
            if (shareEnabled) {
                addJavascriptInterface(
                    com.praxis.android.util.PraxisNativeBridge(context),
                    "PraxisNative",
                )
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?,
                ): Boolean {
                    val url = request?.url ?: return false
                    // OAuth returns to praxis-auth:// would leave the shell with
                    // no handler; rewrite to the https callback the SPA serves.
                    if (url.scheme == "praxis-auth") {
                        val q = url.query
                        val target = buildString {
                            append("https://praxisweb.xyz/auth/callback")
                            if (!q.isNullOrEmpty()) append("?$q")
                        }
                        view?.loadUrl(target)
                        return true
                    }
                    return false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    loading = true
                    offline = false
                }

                override fun onPageCommitVisible(view: WebView?, url: String?) {
                    loading = false
                }

                // OEMs (Realme/ColorOS included) kill the sandboxed renderer
                // opportunistically; unhandled, that crashes the whole app.
                // Rebuild the WebView instead.
                override fun onRenderProcessGone(
                    view: WebView?,
                    detail: android.webkit.RenderProcessGoneDetail?,
                ): Boolean {
                    offline = true
                    loading = false
                    view?.let { wv ->
                        (wv.parent as? android.view.ViewGroup)?.removeView(wv)
                        wv.destroy()
                    }
                    return true
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    loading = false
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: android.webkit.WebResourceError?,
                ) {
                    if (request?.isForMainFrame == true) {
                        offline = true
                        loading = false
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: android.webkit.WebResourceResponse?,
                ) {
                    if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 0) >= 400) {
                        offline = true
                        loading = false
                    }
                }
            }
            loadUrl(WEB_APP_URL)
        }
    }

    // Deep links arrive as internal routes; translate and navigate the page.
    LaunchedEffect(pendingRoute) {
        pendingRoute?.let { route ->
            webView.post { webView.loadUrl(urlForRoute(route)) }
            onRouteConsumed()
        }
    }

    // In-app back navigates the page history before leaving the app.
    BackHandler(enabled = webView.canGoBack()) { webView.goBack() }

    // Network gone → offline panel. Network back → auto-reload once, so a
    // stalled shell (SW-served page with dead lazy-chunk fetches) recovers
    // without the user noticing.
    LaunchedEffect(webView, isOnline) {
        if (!isOnline) {
            offline = true
            loading = false
        } else if (offline) {
            offline = false
            loading = true
            webView.post { webView.reload() }
        }
    }

    // Free the renderer when the shell leaves composition (native-mode switch).
    androidx.compose.runtime.DisposableEffect(webView) {
        onDispose {
            connectivity.unregister()
            webView.destroy()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())

        if (loading && !offline) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp),
            )
        }

        // The panel keys on the live network state, not just the `offline`
        // flag: page callbacks (onPageStarted et al.) fire during the cached
        // shell's own navigation churn and would otherwise hide the panel
        // while the network is still down.
        if (offline || !isOnline) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Server non raggiungibile",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "L'interfaccia web richiede la connessione.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
                )
                Button(onClick = { loading = true; webView.reload() }) {
                    Text("Riprova")
                }
                OutlinedButton(
                    onClick = onUseNativeUi,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text("Usa l'app nativa")
                }
            }
        }
    }
}

/** Persisted UI-mode helpers shared with MainActivity. */
object UiMode {
    private const val PREFS = "praxis_ui"
    private const val KEY = "mode"
    const val WEB = "web"
    const val NATIVE = "native"

    fun read(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, WEB) ?: WEB

    fun write(context: Context, mode: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, mode).apply()
    }
}

/**
 * File-chooser bridge for the WebView shell. When the mirrored web app opens an
 * <input type=file capture> (photo / video / voice attachment), the WebChrome
 * client stashes the callback here and launches the system picker from
 * MainActivity; the activity's onActivityResult delivers the chosen URI back
 * via [handleFileChooserResult].
 */
object WebFileChooser {
    const val RESULT_CODE = 9001
    var pendingCallback: ValueCallback<Array<Uri>>? = null

    /** Call from the activity's onActivityResult. */
    fun handle(resultCode: Int, data: Intent?) {
        val cb = pendingCallback ?: return
        pendingCallback = null
        if (resultCode != android.app.Activity.RESULT_OK || data == null) {
            cb.onReceiveValue(null)
            return
        }
        val results: Array<Uri> = when {
            data.dataString != null -> arrayOf(Uri.parse(data.dataString))
            data.clipData != null -> {
                val items = data.clipData!!
                Array(items.itemCount) { i -> items.getItemAt(i).uri }
            }
            else -> emptyArray()
        }
        cb.onReceiveValue(results)
    }
}
