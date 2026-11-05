@file:OptIn(ExperimentalMaterial3Api::class)

package com.maumela.magnummanagement.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maumela.magnummanagement.BuildConfig
import com.maumela.magnummanagement.R
import com.maumela.magnummanagement.data.model.Theme
import com.maumela.magnummanagement.notifications.FcmRegistrar
import com.maumela.magnummanagement.ui.components.LanguagePickerCard
import com.maumela.magnummanagement.ui.components.MessageCard
import com.maumela.magnummanagement.ui.components.MmmButton
import com.maumela.magnummanagement.ui.components.MmmButtonStyle
import com.maumela.magnummanagement.ui.components.MmmScaffold
import com.maumela.magnummanagement.ui.components.MmmTextField
import com.maumela.magnummanagement.ui.components.MmmTopBar
import com.maumela.magnummanagement.utils.Formatters
import com.maumela.magnummanagement.viewmodel.ProfileViewModel
import com.maumela.magnummanagement.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

/** Profile details, password, notification preference and theme. Changes are saved on the device and sent to the API. */
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    profileViewModel: ProfileViewModel,
    fcm: FcmRegistrar,
    onBack: () -> Unit,
) {
    val settings by settingsViewModel.state.collectAsStateWithLifecycle()
    val profile by profileViewModel.state.collectAsStateWithLifecycle()
    val user by profileViewModel.user.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var fullName by rememberSaveable(user?.id) { mutableStateOf(user?.fullName.orEmpty()) }
    var email by rememberSaveable(user?.id) { mutableStateOf(user?.email.orEmpty()) }
    var phone by rememberSaveable(user?.id) { mutableStateOf(user?.phone.orEmpty()) }
    var currentPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    val msgProfileUpdated = stringResource(R.string.msg_profile_updated)
    val msgPasswordUpdated = stringResource(R.string.msg_password_updated)
    val msgNotificationsBlocked = stringResource(R.string.msg_notifications_blocked)

    fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    var permissionGranted by remember { mutableStateOf(hasNotificationPermission()) }
    val firebaseAvailable = remember { fcm.isAvailable() }

    fun enableNotifications() {
        scope.launch {
            val token = fcm.fetchToken()
            settingsViewModel.setNotificationsEnabled(true, token)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
        if (granted) {
            enableNotifications()
        } else {
            scope.launch { snackbar.showSnackbar(msgNotificationsBlocked) }
        }
    }

    fun onToggleNotifications(enabled: Boolean) {
        if (enabled) {
            if (!permissionGranted && Build.VERSION.SDK_INT >= 33) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                enableNotifications()
            }
        } else {
            scope.launch {
                fcm.deleteToken()
                settingsViewModel.setNotificationsEnabled(false)
            }
        }
    }

    LaunchedEffect(profile.saved) {
        if (profile.saved) {
            profileViewModel.clearMessages()
            snackbar.showSnackbar(msgProfileUpdated)
        }
    }
    LaunchedEffect(profile.passwordChanged) {
        if (profile.passwordChanged) {
            currentPassword = ""
            newPassword = ""
            confirmPassword = ""
            profileViewModel.clearMessages()
            snackbar.showSnackbar(msgPasswordUpdated)
        }
    }
    LaunchedEffect(settings.errorMessage) {
        val message = settings.errorMessage
        if (message != null) {
            settingsViewModel.clearError()
            snackbar.showSnackbar("$message (your choice is saved on this device)")
        }
    }

    val profileError = profile.errorMessage
    val profileErrors = profile.errors
    val passwordErrors = profile.passwordErrors

    MmmScaffold(
        topBar = { MmmTopBar(title = stringResource(R.string.settings_title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Language Selection Card
            Text(stringResource(R.string.section_language), style = MaterialTheme.typography.titleLarge)
            LanguagePickerCard(modifier = Modifier.fillMaxWidth())

            HorizontalDivider()
            Text(stringResource(R.string.section_profile), style = MaterialTheme.typography.titleLarge)
            if (profileError != null) MessageCard(profileError, isError = true, modifier = Modifier.fillMaxWidth())
            MmmTextField(
                value = fullName, onValueChange = { fullName = it; profileViewModel.clearValidationErrors() },
                label = stringResource(R.string.label_full_name), error = profileErrors.fullName, capitalization = KeyboardCapitalization.Words,
                modifier = Modifier.fillMaxWidth(),
            )
            MmmTextField(
                value = phone, onValueChange = { phone = it; profileViewModel.clearValidationErrors() },
                label = stringResource(R.string.label_phone_number), error = profileErrors.phone, keyboardType = KeyboardType.Phone,
                modifier = Modifier.fillMaxWidth(),
            )
            MmmTextField(
                value = email, onValueChange = { email = it; profileViewModel.clearValidationErrors() },
                label = stringResource(R.string.label_email), error = profileErrors.email, keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done, modifier = Modifier.fillMaxWidth(),
            )
            MmmButton(
                text = stringResource(R.string.btn_save_changes),
                onClick = { profileViewModel.saveProfile(fullName, email, phone) },
                loading = profile.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()
            Text(stringResource(R.string.section_change_password), style = MaterialTheme.typography.titleLarge)
            MmmTextField(
                value = currentPassword, onValueChange = { currentPassword = it; profileViewModel.clearValidationErrors() },
                label = stringResource(R.string.label_current_password), error = passwordErrors.current, isPassword = true, modifier = Modifier.fillMaxWidth(),
            )
            MmmTextField(
                value = newPassword, onValueChange = { newPassword = it; profileViewModel.clearValidationErrors() },
                label = stringResource(R.string.label_new_password), error = passwordErrors.new, helper = stringResource(R.string.helper_password_req),
                isPassword = true, modifier = Modifier.fillMaxWidth(),
            )
            MmmTextField(
                value = confirmPassword, onValueChange = { confirmPassword = it; profileViewModel.clearValidationErrors() },
                label = stringResource(R.string.label_confirm_new_password), error = passwordErrors.confirm, isPassword = true,
                imeAction = ImeAction.Done, modifier = Modifier.fillMaxWidth(),
            )
            MmmButton(
                text = stringResource(R.string.btn_update_password),
                onClick = { profileViewModel.changePassword(currentPassword, newPassword, confirmPassword) },
                style = MmmButtonStyle.Secondary,
                loading = profile.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()
            Text(stringResource(R.string.section_notifications), style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = settings.notificationsEnabled,
                        role = Role.Switch,
                        onValueChange = { onToggleNotifications(it) },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.label_push_notifications), modifier = Modifier.weight(1f))
                Switch(checked = settings.notificationsEnabled, onCheckedChange = null)
            }
            if (!firebaseAvailable) {
                Text(
                    stringResource(R.string.msg_firebase_not_configured),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (settings.notificationsEnabled && !permissionGranted) {
                Text(
                    stringResource(R.string.msg_permission_needed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MmmButton(
                    stringResource(R.string.btn_allow_notifications),
                    onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    style = MmmButtonStyle.Secondary,
                )
            }
            if (BuildConfig.DEBUG) {
                MmmButton(
                    text = stringResource(R.string.btn_copy_token),
                    style = MmmButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            val token = fcm.fetchToken()
                            if (token == null) {
                                snackbar.showSnackbar("No token: Firebase is not configured or the token could not be fetched.")
                            } else {
                                Log.d("MMM-FCM", "Device token: $token")
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("FCM token", token))
                                snackbar.showSnackbar("Token copied. Paste it into the Firebase Console test message.")
                            }
                        }
                    },
                )
            }

            HorizontalDivider()
            Text(stringResource(R.string.section_appearance), style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Theme.entries.forEach { option ->
                    FilterChip(
                        selected = settings.theme == option,
                        onClick = { settingsViewModel.setTheme(option) },
                        label = { Text(Formatters.enumLabel(option.name)) },
                    )
                }
            }
            Text(
                stringResource(R.string.msg_theme_saved),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}