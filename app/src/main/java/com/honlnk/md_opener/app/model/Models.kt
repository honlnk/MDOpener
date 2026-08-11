package com.honlnk.md_opener.app.model

import android.net.Uri

data class OpenedFile(
    val uri: Uri,
    val name: String,
    val content: String?,
    val loading: Boolean = false,
    val error: Boolean = false
)

data class RecentItem(
    val uri: String,
    val name: String,
    val lastOpened: Long
)

data class TocItem(
    val id: String,
    val text: String,
    val level: Int
)
