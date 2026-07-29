package com.djs66256.short_drama.feature.mall.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.djs66256.short_drama.feature.mall.model.DEFAULT_MALL_ERROR_MESSAGE
import com.djs66256.short_drama.feature.mall.model.MallBridgeMessage
import com.djs66256.short_drama.feature.mall.model.MallHostMessage
import com.djs66256.short_drama.feature.mall.model.MallHostMessage.RestoreContext
import com.djs66256.short_drama.feature.mall.model.MallHostMessage.SyncAuthState
import com.djs66256.short_drama.feature.mall.model.MallLoginContext
import com.djs66256.short_drama.feature.mall.model.MallPageEvent
import org.json.JSONObject

@Composable
fun MallWebViewContainer(
    url: String,
    modifier: Modifier = Modifier,
    onPageStateChanged: (MallPageEvent) -> Unit,
    onBridgeMessage: (MallBridgeMessage) -> Unit,
    hostMessageDispatcher: MallHostMessageDispatcher,
    isVisible: Boolean,
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(hostMessageDispatcher, webViewRef) {
        val listener: (MallHostMessage) -> Unit = { message ->
            webViewRef?.post {
                when (message) {
                    is SyncAuthState -> webViewRef?.evaluateJavascript(
                        message.toJavascript(),
                        null,
                    )
                    is RestoreContext -> webViewRef?.evaluateJavascript(
                        message.toJavascript(),
                        null,
                    )
                }
            }
        }
        hostMessageDispatcher.bind(listener)
        onDispose {
            hostMessageDispatcher.unbind()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
                configureMallWebView(onPageStateChanged, onBridgeMessage)
                loadUrl(url)
                webViewRef = this
            }
        },
        update = { webView ->
            webViewRef = webView
            webView.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
            if (webView.url != url) {
                webView.loadUrl(url)
            }
        },
        onRelease = { webView ->
            if (webViewRef === webView) {
                hostMessageDispatcher.unbind()
                webViewRef = null
            }
            webView.removeJavascriptInterface(MALL_BRIDGE_NAME)
            webView.stopLoading()
            webView.destroy()
        },
    )
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configureMallWebView(
    onPageStateChanged: (MallPageEvent) -> Unit,
    onBridgeMessage: (MallBridgeMessage) -> Unit,
) {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.loadsImagesAutomatically = true
    settings.cacheMode = WebSettings.LOAD_DEFAULT
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    addJavascriptInterface(MallJavascriptBridge(onBridgeMessage), MALL_BRIDGE_NAME)
    webViewClient = MallWebViewClient(onPageStateChanged)
}

private class MallJavascriptBridge(
    private val onBridgeMessage: (MallBridgeMessage) -> Unit,
) {
    @JavascriptInterface
    fun postMessage(rawPayload: String?) {
        onBridgeMessage(rawPayload.toMallBridgeMessage())
    }
}

private class MallWebViewClient(
    private val onPageStateChanged: (MallPageEvent) -> Unit,
) : WebViewClient() {
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onPageStateChanged(MallPageEvent.LoadStarted(url = url))
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageStateChanged(MallPageEvent.LoadSucceeded(url = url))
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?,
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            onPageStateChanged(
                MallPageEvent.LoadFailed(
                    url = request.url?.toString(),
                    message = error?.description?.toString().orEmpty().ifBlank {
                        DEFAULT_MALL_ERROR_MESSAGE
                    },
                ),
            )
        }
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?,
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        if (request?.isForMainFrame == true && errorResponse != null) {
            onPageStateChanged(
                MallPageEvent.LoadFailed(
                    url = request.url?.toString(),
                    message = "商城加载失败，请重试（HTTP ${errorResponse.statusCode}）",
                ),
            )
        }
    }
}

private fun String?.toMallBridgeMessage(): MallBridgeMessage {
    if (this.isNullOrBlank()) {
        return MallBridgeMessage.Invalid(type = null, reason = "payload is empty")
    }
    return runCatching {
        val root = JSONObject(this)
        val type = root.optString("type")
        val payload = root.optJSONObject("payload")
        when (type) {
            "mall.openSearch" -> MallBridgeMessage.OpenSearch(
                source = payload?.optString("source").orEmpty(),
                returnTarget = payload?.optString("returnTarget").orEmpty(),
            )

            "mall.requestLogin" -> MallBridgeMessage.RequestLogin(
                MallLoginContext(
                    source = payload?.optString("source").orEmpty(),
                    productId = payload?.optString("productId").orEmpty(),
                    returnTarget = payload?.optString("returnTarget").orEmpty(),
                ),
            )

            else -> MallBridgeMessage.Invalid(type = type, reason = "unsupported type")
        }
    }.getOrElse {
        MallBridgeMessage.Invalid(type = null, reason = "invalid json")
    }
}

private fun SyncAuthState.toJavascript(): String {
    val payload = JSONObject().apply {
        put("source", payload.source)
        put("isLoggedIn", payload.isLoggedIn)
        put("reason", payload.reason.wireValue)
        put("returnTarget", payload.returnTarget)
    }.toString()
    return "window.dispatchEvent(new CustomEvent('mall.syncAuthState', { detail: $payload }));"
}

private fun RestoreContext.toJavascript(): String {
    val payload = JSONObject().apply {
        put("source", payload.source)
        put("reason", payload.reason.wireValue)
        put("returnTarget", payload.returnTarget)
        put("preserveScroll", payload.preserveScroll)
    }.toString()
    return "window.dispatchEvent(new CustomEvent('mall.restoreContext', { detail: $payload }));"
}

private const val MALL_BRIDGE_NAME = "mallBridge"
