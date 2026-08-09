package com.satya.calorietracker.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.satya.calorietracker.data.backup.ImportMode
import com.satya.calorietracker.ui.components.AppCard
import com.satya.calorietracker.ui.components.InfoBanner
import com.satya.calorietracker.ui.components.LoadingLine
import com.satya.calorietracker.ui.components.SectionHeader
import com.satya.calorietracker.ui.components.SettingRow
import java.time.LocalDate

@Composable
fun DataSettingsScreen(
    busy: Boolean,
    result: DataOpResult?,
    onExportJson: (android.net.Uri) -> Unit,
    onExportCsv: (android.net.Uri) -> Unit,
    onImport: (android.net.Uri, ImportMode) -> Unit,
    onClearAll: () -> Unit,
    onPruneCache: () -> Unit,
    onDismissResult: () -> Unit,
    onBack: () -> Unit
) {
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now().toString() }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(onExportJson) }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let(onExportCsv) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> pendingImportUri = uri }

    SettingsDetailScaffold(title = "Data", onBack = onBack) { modifier ->
        Column(modifier) {
            if (busy) {
                LoadingLine()
                Spacer(Modifier.height(12.dp))
            }

            result?.let { r ->
                val message = when (r) {
                    is DataOpResult.Exported -> "Saved your ${r.format} backup. It's a plain file — keep it somewhere you'll find it."
                    is DataOpResult.Imported -> r.summary
                    is DataOpResult.Failed -> r.message
                    DataOpResult.Cleared -> "All local data cleared."
                }
                InfoBanner(
                    message = message,
                    container = if (r is DataOpResult.Failed) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.secondaryContainer,
                    onContainer = if (r is DataOpResult.Failed) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onSecondaryContainer,
                    actionLabel = "OK",
                    onAction = onDismissResult
                )
                Spacer(Modifier.height(16.dp))
            }

            Text(
                "There's no cloud backup because there's no account. Export gives you a file you control; import puts it back.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(18.dp))
            SectionHeader("Export")
            AppCard {
                Column {
                    SettingRow(
                        title = "Export as JSON",
                        subtitle = "Complete backup — this is the one you can import back",
                        icon = Icons.Outlined.Download,
                        onClick = { exportJsonLauncher.launch("calorie-tracker-backup-$today.json") }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(
                        title = "Export as CSV",
                        subtitle = "For spreadsheets — food log, weight, water and your foods",
                        icon = Icons.Outlined.Download,
                        onClick = { exportCsvLauncher.launch("calorie-tracker-$today.csv") }
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionHeader("Import")
            AppCard {
                SettingRow(
                    title = "Import a backup",
                    subtitle = "Restore from a JSON file this app exported",
                    icon = Icons.Outlined.Upload,
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
                )
            }

            Spacer(Modifier.height(18.dp))
            SectionHeader("Maintenance")
            AppCard {
                Column {
                    SettingRow(
                        title = "Clean up cached foods",
                        subtitle = "Removes downloaded foods you never used. Your own foods are untouched.",
                        icon = Icons.Outlined.CleaningServices,
                        onClick = onPruneCache
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            OutlinedButton(
                onClick = { showClearConfirm = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear all data", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "This deletes your entire diary, weight history, water logs, custom foods and recipes from this phone. It cannot be undone — export first.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(30.dp))
        }
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("How should we import?") },
            text = {
                Column {
                    Text(
                        "Merge keeps what's already here and adds the backup on top. Replace clears your diary, weight and water first.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "If the backup overlaps with days you've already logged, merge will create duplicates.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onImport(uri, ImportMode.MERGE)
                    pendingImportUri = null
                }) { Text("Merge") }
            },
            dismissButton = {
                TextButton(onClick = {
                    onImport(uri, ImportMode.REPLACE)
                    pendingImportUri = null
                }) { Text("Replace") }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Delete everything?") },
            text = {
                Text(
                    "Your whole history goes. There is no cloud copy and no undo. Export a backup first if there's any doubt.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    onClearAll()
                }) {
                    Text("Delete everything", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Keep my data") }
            }
        )
    }
}
