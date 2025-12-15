package com.example.voicenotes

import android.content.Context
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
//import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.reflect.TypeToken
import com.google.gson.Gson
//import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotesStorage.init(this)

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


data class Note(
    val id: Long,
    val text: String,
    val date: String
)

object NotesStorage {

    private const val PREFS_NAME = "voice_notes_prefs"
    private const val NOTES_KEY = "notes"

    private val notes = mutableStateListOf<Note>()
    private val gson = Gson()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(NOTES_KEY, null)

        if (!json.isNullOrEmpty()) {
            val type = object : TypeToken<List<Note>>() {}.type
            val savedNotes: List<Note> = gson.fromJson(json, type)
            notes.clear()
            notes.addAll(savedNotes)
        }
    }

    private fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(NOTES_KEY, gson.toJson(notes))
            .apply()
    }

    fun getNotes(): List<Note> = notes

    fun addNote(context: Context, text: String) {
        notes.add(
            0,
            Note(
                id = System.currentTimeMillis(),
                text = text,
                date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
            )
        )
        save(context)
    }

    fun deleteNote(context: Context, note: Note) {
        notes.remove(note)
        save(context)
    }

    fun updateNote(context: Context, noteId: Long, newText: String) {
        val index = notes.indexOfFirst { it.id == noteId }
        if (index != -1) {
            notes[index] = notes[index].copy(
                text = newText,
                date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
            )
            save(context)
        }
    }
}

@Composable
fun VoiceNotesApp() {
    val context = LocalContext.current
    val notes = remember { NotesStorage.getNotes() }

    var debugText by remember { mutableStateOf("Нажмите кнопку чтобы начать") }

    var showEditDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var editText by remember { mutableStateOf("") }

    var showCreateDialog by remember { mutableStateOf(false) }
    var newNoteText by remember { mutableStateOf("") }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == ComponentActivity.RESULT_OK) {
            val text = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()

            if (!text.isNullOrBlank()) {
                debugText = "Распознано: $text"
                NotesStorage.addNote(context, text)
            } else {
                debugText = "Речь не распознана"
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startSpeechToText(context, speechLauncher)
    }


    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Редактировать заметку") },
            text = {
                TextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    editingNote?.let {
                        NotesStorage.updateNote(context, it.id, editText)
                    }
                    showEditDialog = false
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Новая заметка") },
            text = {
                TextField(
                    value = newNoteText,
                    onValueChange = { newNoteText = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    NotesStorage.addNote(context, newNoteText)
                    newNoteText = ""
                    showCreateDialog = false
                }) { Text("Создать") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            "Голосовые заметки",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(8.dp))
        Text(debugText)
        Spacer(Modifier.height(16.dp))

        Row {
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (granted) {
                        startSpeechToText(context, speechLauncher)
                    } else {
                        permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                    }
                }
            ) {
                Text("🎤 Голос")
            }

            Spacer(Modifier.width(12.dp))

            Button(
                modifier = Modifier.weight(1f),
                onClick = { showCreateDialog = true }
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Текст")
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn {
            items(notes) { note ->
                NoteItem(
                    note = note,
                    onEdit = {
                        editingNote = note
                        editText = note.text
                        showEditDialog = true
                    },
                    onDelete = {
                        NotesStorage.deleteNote(context, note)
                    }
                )
            }
        }
    }
}

@Composable
fun NoteItem(
    note: Note,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(note.text)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(note.date, style = MaterialTheme.typography.bodySmall)
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, null)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, null)
                    }
                }
            }
        }
    }
}


private fun startSpeechToText(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите...")
    }
    launcher.launch(intent)
}


@Composable
fun VoiceNotesTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
