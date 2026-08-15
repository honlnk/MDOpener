package com.honlnk.md_opener.app.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.items
import com.honlnk.md_opener.app.R
import com.honlnk.md_opener.app.model.OpenedFile
import com.honlnk.md_opener.app.model.TocItem
import com.honlnk.md_opener.app.ui.components.MarkdownWebView
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    file: OpenedFile,
    isDark: Boolean,
    fontSizeSp: Int,
    maxWidthDp: Int,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    val toc = remember { mutableStateListOf<TocItem>() }
    var showToc by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchCount by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        file.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Filled.Search, stringResource(R.string.search))
                    }
                    IconButton(onClick = { showToc = true }) {
                        Icon(Icons.Filled.List, stringResource(R.string.toc))
                    }
                    IconButton(onClick = { share(context, file) }) {
                        Icon(Icons.Filled.Share, stringResource(R.string.share))
                    }
                    IconButton(onClick = { exportPdf(context, webViewRef.value, file.name) }) {
                        Icon(Icons.Filled.PictureAsPdf, stringResource(R.string.export_pdf))
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (showSearch) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { q ->
                        searchQuery = q
                        webViewRef.value?.evaluateJavascript(
                            "window.findText(${JSONObject.quote(q)})"
                        ) { res -> searchCount = res?.toIntOrNull() ?: 0 }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    placeholder = { Text("搜索（${searchCount} 处）") },
                    singleLine = true,
                    trailingIcon = {
                        Text("$searchCount", style = MaterialTheme.typography.labelSmall)
                    }
                )
            }

            when {
                file.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
                file.error -> Box(
                    Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.read_error), style = MaterialTheme.typography.bodyLarge)
                }
                else -> MarkdownWebView(
                    markdown = file.content ?: "",
                    isDark = isDark,
                    fontSizeSp = fontSizeSp,
                    maxWidthDp = maxWidthDp,
                    baseUri = file.uri,
                    onTocReady = {
                        toc.clear()
                        toc.addAll(it)
                    },
                    onWebViewCreated = { webViewRef.value = it },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (showToc) {
            AlertDialog(
                onDismissRequest = { showToc = false },
                title = { Text(stringResource(R.string.toc)) },
                text = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(toc) { item ->
                            TextButton(
                                onClick = {
                                    webViewRef.value?.evaluateJavascript(
                                        "window.scrollToHeading('${item.id}')",
                                        null
                                    )
                                    showToc = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    item.text.ifBlank { "(无标题)" },
                                    modifier = Modifier.fillMaxWidth(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (item.level == 1)
                                        androidx.compose.ui.text.font.FontWeight.Bold
                                    else
                                        androidx.compose.ui.text.font.FontWeight.Normal
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showToc = false }) { Text("关闭") }
                }
            )
        }
    }
}

private fun share(context: Context, file: OpenedFile) {
    // 有 uri 时分享文件流；来自 EXTRA_TEXT 的内存文档退化为分享纯文本
    if (file.content.isNullOrBlank()) return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/markdown"
        if (file.uri != null) {
            putExtra(Intent.EXTRA_STREAM, file.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            putExtra(Intent.EXTRA_TEXT, file.content)
        }
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
}

/** 通过系统打印框架导出 PDF：Chromium 打印引擎负责分页，用户在对话框选「保存为 PDF」 */
private fun exportPdf(context: Context, webView: WebView?, fileName: String) {
    if (webView == null) return
    // 先展开全部折叠、切换浅色主题，再交给打印引擎（preparePrint 内部为同步 DOM 操作）
    webView.evaluateJavascript("window.preparePrint && window.preparePrint()") {
        val jobName = fileName.substringBeforeLast('.', fileName).ifBlank { "document" }
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        printManager.print(jobName, restoreAfterPrintAdapter(webView, jobName), null)
    }
}

/** 包装打印适配器：打印结束（含取消）后恢复折叠状态与主题 */
private fun restoreAfterPrintAdapter(webView: WebView, jobName: String): PrintDocumentAdapter {
    val base = webView.createPrintDocumentAdapter(jobName)
    return object : PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: PrintDocumentAdapter.LayoutResultCallback,
            extras: Bundle?
        ) = base.onLayout(oldAttributes, newAttributes, cancellationSignal, callback, extras)

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: PrintDocumentAdapter.WriteResultCallback
        ) = base.onWrite(pages, destination, cancellationSignal, callback)

        override fun onFinish() {
            base.onFinish()
            webView.post {
                webView.evaluateJavascript("window.restoreAfterPrint && window.restoreAfterPrint()", null)
            }
        }
    }
}
