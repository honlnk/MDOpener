package com.honlnk.md_opener.app

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.honlnk.md_opener.app.core.RecentStore
import com.honlnk.md_opener.app.core.SettingsRepository
import com.honlnk.md_opener.app.core.UriReader
import com.honlnk.md_opener.app.model.OpenedFile
import com.honlnk.md_opener.app.model.RecentItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MarkdownOpenerApp
    private val recentStore: RecentStore = app.recentStore
    val settings: SettingsRepository = app.settingsRepository

    val recentFiles: StateFlow<List<RecentItem>> = recentStore.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentFile = MutableStateFlow<OpenedFile?>(null)

    fun openUri(context: Context, uri: Uri) {
        val name = UriReader.displayName(context, uri) ?: uri.lastPathSegment ?: "document.md"
        currentFile.value = OpenedFile(uri, name, null, loading = true)
        viewModelScope.launch(Dispatchers.IO) {
            val content = UriReader.read(context, uri)
            UriReader.tryPersist(context, uri)
            withContext(Dispatchers.Main) {
                if (content != null) {
                    recentStore.add(uri.toString(), name)
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

    fun removeRecent(uri: String) {
        viewModelScope.launch { recentStore.remove(uri) }
    }

    fun clearRecent() {
        viewModelScope.launch { recentStore.clear() }
    }
}
