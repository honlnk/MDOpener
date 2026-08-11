package com.honlnk.md_opener.app

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.honlnk.md_opener.app.core.SettingsRepository
import com.honlnk.md_opener.app.core.UriReader
import com.honlnk.md_opener.app.model.OpenedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MarkdownOpenerApp
    val settings: SettingsRepository = app.settingsRepository

    val currentFile = MutableStateFlow<OpenedFile?>(null)

    fun openUri(context: Context, uri: Uri) {
        val name = UriReader.displayName(context, uri) ?: uri.lastPathSegment ?: "document.md"
        currentFile.value = OpenedFile(uri, name, null, loading = true)
        viewModelScope.launch(Dispatchers.IO) {
            val content = UriReader.read(context, uri)
            withContext(Dispatchers.Main) {
                if (content != null) {
                    currentFile.value = OpenedFile(uri, name, content, loading = false)
                } else {
                    currentFile.value = OpenedFile(uri, name, null, loading = false, error = true)
                }
            }
        }
    }

    fun closeCurrent() {
        currentFile.value = null
    }
}
