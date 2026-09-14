package com.sounddeck.ui.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.sounddeck.core.model.BackupEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExportImportDialog(
    onDismiss: () -> Unit,
    onExportPackage: () -> Unit,
    onImportPackage: (Uri) -> Unit,
    onRestoreDefault: () -> Unit,
    exportStatusMessage: String?,
    backups: List<BackupEntry> = emptyList(),
    onCreateBackup: () -> Unit = {},
    onRestoreBackup: (String) -> Unit = {},
    onDeleteBackup: (String) -> Unit = {}
) {
    val timeFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val zipPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportPackage(uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF)
                )
                Text(
                    text = "SoundDeck Packages (.zip)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Export and import standalone soundboard packages containing manifest.json and all audio/icon assets.",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Export Button Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF263238)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Backup / Export Board",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Packs all pages, buttons, audio files, and settings into a .zip file in your app directory.",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onExportPackage,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Export SoundDeck_Export.zip", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Import Button Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF263238)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Restore / Import Package",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Select a .zip or .sbpack file to restore configuration and media assets.",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { zipPicker.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Select ZIP to Import", color = Color(0xFF00E5FF))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Backups Card (E6)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2430)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Copias de Seguridad (E6)",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF),
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Automáticas diarias y manuales",
                                    fontSize = 10.sp,
                                    color = Color.LightGray
                                )
                            }
                            Button(
                                onClick = onCreateBackup,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("+ Nueva Copia", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (backups.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            backups.take(5).forEach { b ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = b.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${timeFormatter.format(Date(b.timestamp))} • ${(b.sizeBytes / 1024)} KB",
                                            fontSize = 9.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(
                                            onClick = { onRestoreBackup(b.filePath) },
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                                        ) {
                                            Text("Restaurar", color = Color(0xFF00E5FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        IconButton(
                                            onClick = { onDeleteBackup(b.filePath) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Borrar",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Reset Template Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1E26)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Reset Default Template", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Restores default pages and sound library", fontSize = 10.sp, color = Color.Gray)
                        }
                        TextButton(onClick = onRestoreDefault) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Reset", color = Color(0xFFFF5252), fontSize = 11.sp)
                        }
                    }
                }

                if (!exportStatusMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = exportStatusMessage,
                        fontSize = 11.sp,
                        color = Color(0xFF00E5FF)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F))
            ) {
                Text("Close", color = Color.White)
            }
        },
        containerColor = Color(0xFF161920)
    )
}
