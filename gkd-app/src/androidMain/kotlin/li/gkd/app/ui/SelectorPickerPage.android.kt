package li.gkd.app.ui

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import li.gkd.app.snapshot.SnapshotStore
import li.gkd.app.ui.navigation.SelectorPickerRoute
import li.gkd.app.ui.platform.UiHost
import li.gkd.app.ui.share.LocalDarkTheme
import li.gkd.app.util.ToastUtils
import li.gkd.db.Db
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
@Composable
actual fun SelectorPickerPage(
    route: SelectorPickerRoute,
    host: UiHost,
) {
    val mainVm = MainViewModel.requireCurrent()
    val dark = LocalDarkTheme.current
    var snapshotJson by remember { mutableStateOf<String?>(null) }
    var loadError by remember { mutableStateOf(false) }

    LaunchedEffect(route.snapshotId) {
        try {
            val id = route.snapshotId
                ?: Db.snapshotDao.query().first().firstOrNull()?.id
            if (id == null) {
                loadError = true
                return@LaunchedEffect
            }
            val json = withContext(Dispatchers.IO) {
                SnapshotStore.snapshotFile(id).readText()
            }
            snapshotJson = json
        } catch (e: Exception) {
            loadError = true
        }
    }

    BackHandler {
        mainVm.navigator.pop()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            loadError -> {
                Text(
                    text = "无可用快照 No snapshot available",
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            snapshotJson == null -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            else -> {
                val json = snapshotJson!!
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = true
                            webViewClient = WebViewClient()
                            addJavascriptInterface(PickerJsApi(dark), "GkdBridge")
                            loadUrl("file:///android_asset/selector-picker/index.html")
                        }
                    },
                    update = { view ->
                        view.evaluateJavascript(
                            "loadSnapshot(${JSONObject.quote(json)})",
                            null,
                        )
                    },
                )
            }
        }
    }
}

private class PickerJsApi(
    private val dark: Boolean,
) {
    @JavascriptInterface
    fun isDark(): Boolean = dark

    @JavascriptInterface
    fun copyText(text: String) {
        li.gkd.app.util.copyText(text)
    }

    @JavascriptInterface
    fun toast(text: String) {
        ToastUtils.show(text)
    }

    @JavascriptInterface
    fun saveSelector(selector: String) {
        li.gkd.app.util.copyText(selector)
        ToastUtils.show("已复制选择器，请粘贴到规则中使用 Copied, paste it into your rule")
    }
}
