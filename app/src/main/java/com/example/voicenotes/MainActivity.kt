package com.example.voicenotes

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VoiceNotesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VoiceNotesApp()
                }
            }
        }
    }
}

object NotesStorage {
    private val notes = mutableStateListOf<Note>()

    fun addNote(text: String) {
        notes.add(0, Note(
            id = System.currentTimeMillis(),
            text = text,
            date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        ))
    }

    fun getNotes(): List<Note> = notes

    fun deleteNote(note: Note) {
        notes.remove(note)
    }

    fun updateNote(noteId: Long, newText: String) {
        val iterator = notes.iterator()
        var index = 0
        var found = false

        while (iterator.hasNext()) {
            val note = iterator.next()
            if (note.id == noteId) {
                found = true
                break
            }
            index++
        }

        if (found) {
            val oldNote = notes[index]
            notes[index] = oldNote.copy(
                text = newText,
                date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
            )
            val updatedNote = notes.removeAt(index)
            notes.add(0, updatedNote)
        }
    }
}

data class Note(
    val id: Long,
    val text: String,
    val date: String
)

@Composable
fun VoiceNotesApp() {
    val context = LocalContext.current
    var debugText by remember { mutableStateOf("Нажмите кнопку чтобы начать") }
    val notes = remember { NotesStorage.getNotes() }

    var showEditDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var editText by remember { mutableStateOf("") }

    var showCreateDialog by remember { mutableStateOf(false) }
    var newNoteText by remember { mutableStateOf("") }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            ComponentActivity.RESULT_OK -> {
                val data = result.data
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (results != null && results.isNotEmpty()) {
                    val spokenText = results[0]
                    debugText = "Распознано: $spokenText"
                    Toast.makeText(context, "Распознано: $spokenText", Toast.LENGTH_LONG).show()
                    NotesStorage.addNote(spokenText)
                } else {
                    debugText = "Речь не распознана"
                    Toast.makeText(context, "Речь не распознана", Toast.LENGTH_SHORT).show()
                }
            }
            ComponentActivity.RESULT_CANCELED -> {
                debugText = "Распознавание отменено"
                Toast.makeText(context, "Распознавание отменено", Toast.LENGTH_SHORT).show()
            }
            else -> {
                debugText = "Ошибка распознавания"
                Toast.makeText(context, "Ошибка распознавания", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startSpeechToText(context, speechLauncher)
        } else {
            Toast.makeText(context, "Разрешение на микрофон отклонено", Toast.LENGTH_LONG).show()
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = {
                showEditDialog = false
                editingNote = null
                editText = ""
            },
            title = { Text("Редактировать заметку") },
            text = {
                Column {
                    TextField(
                        value = editText,
                        onValueChange = { editText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Введите текст заметки...") },
                        singleLine = false,
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editText.isNotBlank()) {
                            editingNote?.let { note ->
                                NotesStorage.updateNote(note.id, editText)
                                Toast.makeText(context, "Заметка обновлена", Toast.LENGTH_SHORT).show()
                            }
                            showEditDialog = false
                            editingNote = null
                            editText = ""
                        } else {
                            Toast.makeText(context, "Заметка не может быть пустой", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEditDialog = false
                        editingNote = null
                        editText = ""
                    }
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                newNoteText = ""
            },
            title = { Text("Новая заметка") },
            text = {
                Column {
                    TextField(
                        value = newNoteText,
                        onValueChange = { newNoteText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Введите текст заметки...") },
                        singleLine = false,
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newNoteText.isNotBlank()) {
                            NotesStorage.addNote(newNoteText)
                            Toast.makeText(context, "Заметка создана", Toast.LENGTH_SHORT).show()
                            showCreateDialog = false
                            newNoteText = ""
                        } else {
                            Toast.makeText(context, "Заметка не может быть пустой", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                        newNoteText = ""
                    }
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Голосовые заметки",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = debugText,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        startSpeechToText(context, speechLauncher)
                    } else {
                        permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("🎤 Голосовая заметка")
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = {
                    showCreateDialog = true
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Текстовая")
            }
        }

        if (notes.isNotEmpty()) {
            Text(
                text = "Ваши заметки:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(notes) { note ->
                    NoteItem(
                        note = note,
                        onEditClick = {
                            editingNote = note
                            editText = note.text
                            showEditDialog = true
                        },
                        onDeleteClick = {
                            NotesStorage.deleteNote(note)
                            Toast.makeText(context, "Заметка удалена", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        } else {
            Text(
                text = "Заметок пока нет",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun NoteItem(
    note: Note,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = note.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = note.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row {
                    IconButton(
                        onClick = onEditClick
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Редактировать",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onDeleteClick
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

private fun startSpeechToText(
    context: android.content.Context,
    speechLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите сейчас...")
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    if (speechIntent.resolveActivity(context.packageManager) != null) {
        try {
            speechLauncher.launch(speechIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка запуска: ${e.message}", Toast.LENGTH_LONG).show()
        }
    } else {
        Toast.makeText(context, "Распознавание речи не поддерживается", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun VoiceNotesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        content = content
    )
}