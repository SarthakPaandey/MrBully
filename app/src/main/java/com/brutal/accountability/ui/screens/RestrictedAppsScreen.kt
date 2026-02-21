package com.brutal.accountability.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brutal.accountability.InstalledApp
import com.brutal.accountability.data.RestrictedAppEntity
import com.brutal.accountability.ui.components.AnimatedScreen
import com.brutal.accountability.ui.components.BrutalButton
import com.brutal.accountability.ui.components.BrutalTextField
import com.brutal.accountability.ui.components.SectionHeader
import com.brutal.accountability.ui.theme.BrutalRed
import com.brutal.accountability.ui.theme.BrutalRedSubtle
import com.brutal.accountability.ui.theme.CardSurface
import com.brutal.accountability.ui.theme.DividerDark
import com.brutal.accountability.ui.theme.StatusGreen
import com.brutal.accountability.ui.theme.TextMuted
import com.brutal.accountability.ui.theme.TextPrimary
import com.brutal.accountability.ui.theme.TextSecondary

@Composable
fun RestrictedAppsScreen(
    initiallySelected: List<RestrictedAppEntity>,
    onSave: (List<RestrictedAppEntity>) -> Unit
) {
    val context = LocalContext.current
    val selected = remember {
        mutableStateListOf<String>().apply { addAll(initiallySelected.map { it.packageName }) }
    }
    
    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val loadedApps = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            context.packageManager.queryIntentActivities(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
                0
            ).map {
                InstalledApp(
                    packageName = it.activityInfo.packageName,
                    label = it.loadLabel(context.packageManager).toString()
                )
            }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
        }
        apps = loadedApps
        isLoading = false
    }

    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(searchQuery, apps) {
        if (searchQuery.isBlank()) apps
        else apps.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    AnimatedScreen {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader(
                title = "Restrict Apps",
                icon = Icons.Default.Block
            )
            Text(
                "Select apps that trigger intervention mode. ${selected.size} selected.",
                style = MaterialTheme.typography.bodyMedium
            )

            BrutalTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "Search apps..."
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, false)
            ) {
                items(filteredApps) { app ->
                    val isChecked = selected.contains(app.packageName)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isChecked) selected.remove(app.packageName)
                                else selected.add(app.packageName)
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isChecked) BrutalRedSubtle else CardSurface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    app.label,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isChecked) TextPrimary else TextSecondary
                                )
                                Text(
                                    app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                            if (isChecked) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = BrutalRed
                                )
                            } else {
                                Checkbox(
                                    checked = false,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        uncheckedColor = DividerDark
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            BrutalButton(
                text = "SAVE RESTRICTED APPS (${selected.size})",
                onClick = {
                    val payload = apps.filter { selected.contains(it.packageName) }
                        .map { RestrictedAppEntity(it.packageName, it.label) }
                    onSave(payload)
                }
            )
        }
    }
}
