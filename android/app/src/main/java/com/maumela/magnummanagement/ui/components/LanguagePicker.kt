package com.maumela.magnummanagement.ui.components

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maumela.magnummanagement.utils.LocaleHelper

data class LanguageOption(val code: String, val nativeName: String, val englishName: String)

@Composable
fun LanguagePickerCard(modifier: Modifier = Modifier) {
    val languages = listOf(
        LanguageOption("en", "English", "English"),
        LanguageOption("zu", "isiZulu", "Zulu"),
        LanguageOption("af", "Afrikaans", "Afrikaans")
    )

    var selectedLanguage by remember { mutableStateOf(LocaleHelper.getCurrentLocale()) }

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
                            selectedLanguage = lang.code
                            LocaleHelper.setLocale(lang.code)
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedLanguage == lang.code,
                        onClick = {
                            selectedLanguage = lang.code
                            LocaleHelper.setLocale(lang.code)
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
