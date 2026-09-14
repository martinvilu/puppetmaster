package com.sounddeck.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sounddeck.core.model.PageConfig

@Composable
fun PageManagerDialog(
    pages: List<PageConfig>,
    activePageIndex: Int,
    onSelectPage: (Int) -> Unit,
    onReorderPage: (fromIndex: Int, toIndex: Int) -> Unit,
    onRenamePage: (pageId: String, newName: String) -> Unit,
    onDeletePage: (pageId: String) -> Unit,
    onAddNewPage: () -> Unit,
    onUpdatePageColor: (pageId: String, colorHex: String) -> Unit = { _, _ -> },
    onDismiss: () -> Unit
) {
    var editingPageId by remember { mutableStateOf<String?>(null) }
    var editedName by remember { mutableStateOf("") }
    var pageToDelete by remember { mutableStateOf<PageConfig?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14171E),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewCarousel,
                        contentDescription = "Páginas",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Gestión de Páginas",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Reordena las páginas usando las flechas, renómbralas o elimina las que no necesites.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(pages, key = { _, page -> page.id }) { index, page ->
                        val isActive = index == activePageIndex
                        val isEditing = editingPageId == page.id

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isActive) 1.5.dp else 1.dp,
                                    color = if (isActive) Color(0xFF00E5FF) else Color(0xFF2A3140),
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isActive) Color(0xFF1B2230) else Color(0xFF181C26)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                if (isEditing) {
                                    // Inline rename field
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = editedName,
                                            onValueChange = { editedName = it },
                                            singleLine = true,
                                            label = { Text("Nombre de página", fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF00E5FF),
                                                focusedLabelColor = Color(0xFF00E5FF),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            )
                                        )

                                        IconButton(
                                            onClick = {
                                                if (editedName.isNotBlank()) {
                                                    onRenamePage(page.id, editedName.trim())
                                                }
                                                editingPageId = null
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color(0xFF00E5FF).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Guardar",
                                                tint = Color(0xFF00E5FF),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { editingPageId = null },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color(0xFF2A3140), RoundedCornerShape(6.dp))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Cancelar",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Left: Page order indicator & Title
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onSelectPage(index) },
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isActive) Color(0xFF00E5FF) else Color(0xFF2E3646)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${index + 1}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isActive) Color.Black else Color.White
                                                )
                                            }

                                            Column {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = page.name,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isActive) Color(0xFF00E5FF) else Color.White,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (isActive) {
                                                        Text(
                                                            text = "ACTIVA",
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF00E5FF),
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(Color(0xFF00E5FF).copy(alpha = 0.2f))
                                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "Grid ${page.gridRows}×${page.gridCols} • ${page.pads.size} pads",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }

                                        // Actions: Reorder Up/Down, Rename, Delete
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            // Move Up
                                            IconButton(
                                                onClick = { onReorderPage(index, index - 1) },
                                                enabled = index > 0,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowUpward,
                                                    contentDescription = "Mover arriba",
                                                    tint = if (index > 0) Color.White else Color(0xFF454D5D),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            // Move Down
                                            IconButton(
                                                onClick = { onReorderPage(index, index + 1) },
                                                enabled = index < pages.size - 1,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDownward,
                                                    contentDescription = "Mover abajo",
                                                    tint = if (index < pages.size - 1) Color.White else Color(0xFF454D5D),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            // Rename
                                            IconButton(
                                                onClick = {
                                                    editingPageId = page.id
                                                    editedName = page.name
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Renombrar",
                                                    tint = Color(0xFFFFD600),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            // Delete
                                            IconButton(
                                                onClick = { pageToDelete = page },
                                                enabled = pages.size > 1,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Eliminar",
                                                    tint = if (pages.size > 1) Color(0xFFFF5252) else Color(0xFF454D5D),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Color:", fontSize = 10.sp, color = Color.Gray)
                                    listOf("#00E5FF", "#FF5252", "#FFD600", "#00E676", "#E040FB", "#FF9100", "#7C4DFF").forEach { hex ->
                                        val isCurrent = (page.color ?: "#00E5FF").equals(hex, ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(Color((0xFF000000 or hex.removePrefix("#").toLong(16)).toInt()))
                                                .border(
                                                    width = if (isCurrent) 2.dp else 0.dp,
                                                    color = if (isCurrent) Color.White else Color.Transparent,
                                                    shape = androidx.compose.foundation.shape.CircleShape
                                                )
                                                .clickable { onUpdatePageColor(page.id, hex) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Add New Page Button
                Button(
                    onClick = onAddNewPage,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E5FF).copy(alpha = 0.15f),
                        contentColor = Color(0xFF00E5FF)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Agregar página",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Agregar Nueva Página",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Listo", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
            }
        }
    )

    // Confirmation dialog for deleting a page
    if (pageToDelete != null) {
        val target = pageToDelete!!
        AlertDialog(
            onDismissRequest = { pageToDelete = null },
            containerColor = Color(0xFF1E232F),
            title = {
                Text(
                    text = "¿Eliminar página?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "Se eliminará la página '${target.name}' y todos sus ${target.pads.size} pads asociados. Esta acción no se puede deshacer.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePage(target.id)
                        pageToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pageToDelete = null }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}
