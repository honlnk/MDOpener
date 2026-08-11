package com.honlnk.md_opener.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
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
                    IconButton(onClick = { shareFile(context, file.uri) }) {
                        Icon(Icons.Filled.Share, stringResource(R.string.share))
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

private fun shareFile(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/markdown"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
}
