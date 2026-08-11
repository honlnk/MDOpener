package com.honlnk.md_opener.app

import android.app.Application
import com.honlnk.md_opener.app.core.RecentStore
import com.honlnk.md_opener.app.core.SettingsRepository

class MarkdownOpenerApp : Application() {
    val recentStore: RecentStore by lazy { RecentStore(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
}
