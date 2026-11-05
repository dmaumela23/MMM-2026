package com.maumela.magnummanagement.utils

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleHelper {

    fun setLocale(languageCode: String) {
        val appLocale = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun getCurrentLocale(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (!locales.isEmpty) {
            locales.get(0)?.language ?: "en"
        } else {
            "en"
        }
    }
}