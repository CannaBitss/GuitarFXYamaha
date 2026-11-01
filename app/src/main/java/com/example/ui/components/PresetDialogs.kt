package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SavePresetDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, desc: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "SAVE GUITAR PRESET",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD54F)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Preset Name") },
                    placeholder = { Text("e.g. My THR Solo") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preset_name_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("e.g. High gain lead tone with REV-X reverb") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), description.trim())
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                modifier = Modifier.testTag("save_preset_confirm")
            ) {
                Text("SAVE", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.LightGray)
            }
        },
        containerColor = Color(0xFF1B1E24)
    )
}

@Composable
fun PresetJsonDialog(
    jsonContent: String,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    var editableJson by remember { mutableStateOf(jsonContent) }
    var isCopied by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "PRESET JSON (IMPORT / EXPORT)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Copy this JSON to share your tone, or paste a new preset JSON below:",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editableJson,
                    onValueChange = { editableJson = it },
                    maxLines = 10,
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(editableJson))
                        isCopied = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isCopied) "COPIED TO CLIPBOARD!" else "COPY TO CLIPBOARD")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(editableJson) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Text("APPLY IMPORT", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = Color.LightGray)
            }
        },
        containerColor = Color(0xFF1B1E24)
    )
}
