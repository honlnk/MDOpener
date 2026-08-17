package com.honlnk.md_opener.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewModelScope
import com.honlnk.md_opener.app.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun AppRoot(vm: MainViewModel) {
    val current by vm.currentFile.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val themeMode by vm.settings.themeMode.collectAsState(initial = 0)
    val fontSizeSp by vm.settings.fontSizeSp.collectAsState(initial = 17)
    val maxWidthDp by vm.settings.maxWidthDp.collectAsState(initial = 720)
    val pdfPaperSize by vm.settings.pdfPaperSize.collectAsState(initial = "a4")
    val pdfKeepBackground by vm.settings.pdfKeepBackground.collectAsState(initial = false)
    val pdfAutoOpen by vm.settings.pdfAutoOpen.collectAsState(initial = false)

    val isDark = when (themeMode) {
        0 -> isSystemInDarkTheme()
        2 -> true
        else -> false
    }

    if (showSettings) {
        SettingsScreen(
            themeMode = themeMode,
            fontSizeSp = fontSizeSp,
            maxWidthDp = maxWidthDp,
            onThemeChange = { vm.viewModelScope.launch { vm.settings.setThemeMode(it) } },
            onFontChange = { vm.viewModelScope.launch { vm.settings.setFontSize(it) } },
            onWidthChange = { vm.viewModelScope.launch { vm.settings.setMaxWidth(it) } },
            onBack = { showSettings = false }
        )
    } else if (current != null) {
        ViewerScreen(
            file = current!!,
            isDark = isDark,
            fontSizeSp = fontSizeSp,
            maxWidthDp = maxWidthDp,
            pdfPaperSize = pdfPaperSize,
            pdfKeepBackground = pdfKeepBackground,
            onPdfPaperChange = { vm.viewModelScope.launch { vm.settings.setPdfPaperSize(it) } },
            onPdfKeepBgChange = { vm.viewModelScope.launch { vm.settings.setPdfKeepBackground(it) } },
            onPdfAutoOpenChange = { vm.viewModelScope.launch { vm.settings.setPdfAutoOpen(it) } },
            onClose = vm::closeCurrent
        )
    } else {
        HomeScreen(
            onOpenUri = { vm.openUri(context, it) },
            onSettings = { showSettings = true }
        )
    }
}
