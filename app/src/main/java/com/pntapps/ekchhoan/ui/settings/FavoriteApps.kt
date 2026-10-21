package com.pntapps.ekchhoan.ui.settings

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pntapps.ekchhoan.ui.common.toAppIconBitmap
import com.pntapps.ekchhoan.R
import com.pntapps.ekchhoan.data.MAX_FAVORITE_APPS
import com.pntapps.ekchhoan.system.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.OutlinedTextField

private const val ICON_PX = 96

/** Settings block: the apps pinned to Home, each removable, plus an "Add app" picker (max [MAX_FAVORITE_APPS]). */
@Composable
internal fun FavoriteAppsBlock(selected: List<String>, vm: SettingsViewModel) {
    var picking by rememberSaveable { mutableStateOf(false) }
    Column {
        Text(
            stringResource(R.string.favorite_apps_desc, MAX_FAVORITE_APPS),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        selected.forEach { pkg ->
            val label by produceState<String?>(initialValue = null, pkg) {
                value = withContext(Dispatchers.Default) { vm.appInfo(pkg)?.first }
            }
            val name = label ?: stringResource(R.string.favorite_apps_missing)
            ListItem(
                leadingContent = { AppIcon(pkg, vm::appInfo) },
                headlineContent = { Text(name, maxLines = 1) },
                trailingContent = {
                    IconButton(onClick = { vm.setFavoriteApps(selected - pkg) }) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.favorite_apps_remove, name),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )
        }
        Button(
            onClick = { picking = true },
            enabled = selected.size < MAX_FAVORITE_APPS,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Text(stringResource(R.string.favorite_apps_add), modifier = Modifier.padding(start = 8.dp))
        }
        if (selected.size >= MAX_FAVORITE_APPS) {
            Text(
                stringResource(R.string.favorite_apps_full, MAX_FAVORITE_APPS),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
    if (picking) {
        AppPickerDialog(
            initial = selected,
            loadApps = vm::installedApps,
            appInfo = vm::appInfo,
            onDismiss = { picking = false },
            onDone = {
                vm.setFavoriteApps(it)
                picking = false
            },
        )
    }
}

/**
 * Every app with a launcher icon, searchable; tick up to [MAX_FAVORITE_APPS]. Order of ticking is the order
 * on Home. Used by Settings and by onboarding's "More apps…".
 */
@Composable
internal fun AppPickerDialog(
    initial: List<String>,
    loadApps: suspend () -> List<InstalledApp>,
    appInfo: (String) -> Pair<String, Drawable>?,
    onDismiss: () -> Unit,
    onDone: (List<String>) -> Unit,
) {
    val apps by produceState<List<InstalledApp>?>(initialValue = null) { value = loadApps() }
    var chosen by remember { mutableStateOf(initial) }
    var query by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.favorite_apps_pick_title, chosen.size, MAX_FAVORITE_APPS)) },
        text = {
            val list = apps
            if (list == null) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val shown = remember(list, query) {
                    val q = query.trim()
                    if (q.isEmpty()) list else list.filter { it.label.contains(q, ignoreCase = true) }
                }
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(stringResource(R.string.favorite_apps_search)) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = if (query.isNotEmpty()) {
                            {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.favorite_apps_search_clear))
                                }
                            }
                        } else {
                            null
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    )
                    if (shown.isEmpty()) {
                        Text(
                            stringResource(R.string.favorite_apps_no_match),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    LazyColumn(Modifier.heightIn(max = 380.dp)) {
                        items(shown, key = { it.packageName }) { app ->
                            val checked = app.packageName in chosen
                            val canAdd = checked || chosen.size < MAX_FAVORITE_APPS
                            val toggle = {
                                chosen = if (checked) chosen - app.packageName else chosen + app.packageName
                            }
                            ListItem(
                                modifier = Modifier.clickable(enabled = canAdd, role = Role.Checkbox, onClick = toggle),
                                leadingContent = { AppIcon(app.packageName, appInfo) },
                                headlineContent = { Text(app.label, maxLines = 2) },
                                trailingContent = { Checkbox(checked = checked, onCheckedChange = null, enabled = canAdd) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onDone(chosen) }) { Text(stringResource(R.string.favorite_apps_done)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.set_action_cancel)) } },
    )
}

/** An installed app's icon, loaded off the main thread; a generic app icon until then or if it is gone. */
@Composable
internal fun AppIcon(packageName: String, appInfo: (String) -> Pair<String, Drawable>?) {
    val icon by produceState<ImageBitmap?>(initialValue = null, packageName) {
        value = withContext(Dispatchers.Default) {
            runCatching { appInfo(packageName)?.second?.toAppIconBitmap(ICON_PX) }.getOrNull()
        }
    }
    val bitmap = icon
    if (bitmap != null) {
        Image(bitmap, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)))
    } else {
        Icon(Icons.Rounded.Apps, contentDescription = null, modifier = Modifier.size(40.dp))
    }
}
