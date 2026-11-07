package com.maumela.magnummanagement.ui.components

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.maumela.magnummanagement.viewmodel.SettingsViewModel

data class LanguageOption(val code: String, val nativeName: String, val englishName: String)

@Composable
fun LanguagePickerCard(
    settingsViewModel: SettingsViewModel,
    currentLanguage: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val languages = listOf(
        LanguageOption("en", "English", "English"),
        LanguageOption("zu", "isiZulu", "Zulu"),
        LanguageOption("af", "Afrikaans", "Afrikaans")
    )

    fun changeLanguage(code: String) {
        settingsViewModel.setLanguage(code)
        // Instantly recreate activity to redraw all string resources in the new locale
        (context as? Activity)?.recreate()
    }

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "App Language / Ulimi Lwe-App / App-taal",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            languages.forEach { lang ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            changeLanguage(lang.code)
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentLanguage == lang.code,
                        onClick = {
                            changeLanguage(lang.code)
                        }
                    )
                    Text(
                        text = "${lang.nativeName} (${lang.englishName})",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}