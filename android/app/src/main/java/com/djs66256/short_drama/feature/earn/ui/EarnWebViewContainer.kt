package com.djs66256.short_drama.feature.earn.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
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
import com.djs66256.short_drama.feature.earn.model.DEFAULT_EARN_ERROR_MESSAGE
import com.djs66256.short_drama.feature.earn.model.EARN_HOST_MESSAGE_EVENT
import com.djs66256.short_drama.feature.earn.model.EarnBridgeMessage
import com.djs66256.short_drama.feature.earn.model.EarnHostMessage
import com.djs66256.short_drama.feature.earn.model.EarnLoginContext
import com.djs66256.short_drama.feature.earn.model.EarnPageEvent
import com.djs66256.short_drama.feature.earn.model.EarnTaskContext
import org.json.JSONObject

@Composable
fun EarnWebViewContainer(
    url: String,
    modifier: Modifier = Modifier,
    onPageStateChanged: (EarnPageEvent) -> Unit,
    onBridgeMessage: (EarnBridgeMessage) -> Unit,
    hostMessageDispatcher: EarnHostMessageDispatcher,
    isVisible: Boolean,
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(hostMessageDispatcher, webViewRef) {
        val listener: (EarnHostMessage) -> Unit = { message ->
            webViewRef?.post {
                webViewRef?.evaluateJavascript(message.toJavascript(), null)
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
                configureEarnWebView(onPageStateChanged, onBridgeMessage)
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
            webView.removeJavascriptInterface(EARN_BRIDGE_NAME)
            webView.stopLoading()
            webView.destroy()
        },
    )
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configureEarnWebView(
    onPageStateChanged: (EarnPageEvent) -> Unit,
    onBridgeMessage: (EarnBridgeMessage) -> Unit,
) {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.loadsImagesAutomatically = true
    settings.cacheMode = WebSettings.LOAD_DEFAULT
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    addJavascriptInterface(EarnJavascriptBridge(onBridgeMessage), EARN_BRIDGE_NAME)
    webViewClient = EarnWebViewClient(onPageStateChanged)
}

private class EarnJavascriptBridge(
    private val onBridgeMessage: (EarnBridgeMessage) -> Unit,
) {
    @JavascriptInterface
    fun postMessage(rawPayload: String?) {
        onBridgeMessage(rawPayload.toEarnBridgeMessage())
    }
}

private class EarnWebViewClient(
    private val onPageStateChanged: (EarnPageEvent) -> Unit,
) : WebViewClient() {
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onPageStateChanged(EarnPageEvent.LoadStarted(url = url))
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageStateChanged(EarnPageEvent.LoadSucceeded(url = url))
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?,
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            onPageStateChanged(
                EarnPageEvent.LoadFailed(
                    url = request.url?.toString(),
                    message = error?.description?.toString().orEmpty().ifBlank {
                        DEFAULT_EARN_ERROR_MESSAGE
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
                EarnPageEvent.LoadFailed(
                    url = request.url?.toString(),
                    message = "赚钱页加载失败，请重试（HTTP ${errorResponse.statusCode}）",
                ),
            )
        }
    }
}

private fun String?.toEarnBridgeMessage(): EarnBridgeMessage {
    if (this.isNullOrBlank()) {
        return EarnBridgeMessage.Invalid(type = null, reason = "payload is empty")
    }
    return runCatching {
        val root = JSONObject(this)
        val type = root.optString("type")
        val payload = root.optJSONObject("payload")
        when (type) {
            "earn.requestLogin" -> EarnBridgeMessage.RequestLogin(
                EarnLoginContext(
                    source = payload?.optString("source").orEmpty(),
                    returnTarget = payload?.optString("returnTarget").orEmpty(),
                ),
            )

            "earn.openTaskPlayer" -> EarnBridgeMessage.OpenTaskPlayer(
                EarnTaskContext(
                    taskId = payload?.optString("taskId").orEmpty(),
                    source = payload?.optString("source").orEmpty(),
                    returnTarget = payload?.optString("returnTarget").orEmpty(),
                    videoId = payload?.optString("videoId").orEmpty(),
                ),
            )

            else -> EarnBridgeMessage.Invalid(type = type, reason = "unsupported type")
        }
    }.getOrElse {
        Log.w(EARN_WEB_VIEW_TAG, "Failed to parse earn bridge payload", it)
        EarnBridgeMessage.Invalid(type = null, reason = "invalid json")
    }
}

private fun EarnHostMessage.toJavascript(): String {
    val detail = JSONObject().apply {
        when (this@toJavascript) {
            is EarnHostMessage.SyncAuthState -> {
                put("type", "earn.syncAuthState")
                put(
                    "payload",
                    JSONObject().apply {
                        put("source", payload.source)
                        put("isLoggedIn", payload.isLoggedIn)
                        put("reason", payload.reason.wireValue)
                        put("returnTarget", payload.returnTarget)
                        put("apiAccessToken", payload.apiAccessToken ?: JSONObject.NULL)
                        put("expiresAt", payload.expiresAt ?: JSONObject.NULL)
                    },
                )
            }

            is EarnHostMessage.RestoreContext -> {
                put("type", "earn.restoreContext")
                put(
                    "payload",
                    JSONObject().apply {
                        put("source", payload.source)
                        put("reason", payload.reason.wireValue)
                        put("returnTarget", payload.returnTarget)
                        put("preserveScroll", payload.preserveScroll)
                    },
                )
            }

            is EarnHostMessage.CompleteTask -> {
                put("type", "earn.completeTask")
                put(
                    "payload",
                    JSONObject().apply {
                        put("source", payload.source)
                        put("taskId", payload.taskId)
                        put("videoId", payload.videoId)
                        put("completed", payload.completed)
                        put("reason", payload.reason.wireValue)
                    },
                )
            }
        }
    }.toString()
    return "window.dispatchEvent(new CustomEvent('$EARN_HOST_MESSAGE_EVENT', { detail: $detail }));"
}

private const val EARN_BRIDGE_NAME = "earnBridge"
private const val EARN_WEB_VIEW_TAG = "EarnWebViewContainer"
