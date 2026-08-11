package com.honlnk.md_opener.app.core

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

object UriReader {

    /** 通过 ContentResolver 读取文本（兼容 content:// 与 file:// 以及 scoped storage） */
    fun read(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
        } catch (e: Exception) {
            null
        }
    }

    /** 取得文件显示名 */
    fun displayName(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            val proj = arrayOf(OpenableColumns.DISPLAY_NAME)
            try {
                context.contentResolver.query(uri, proj, null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) return c.getString(idx)
                    }
                }
            } catch (_: Exception) {
            }
        }
        return uri.lastPathSegment
    }
}
