package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.local.LegoDatabase
import com.example.myapplication.data.local.PartEntity
import com.example.myapplication.data.remote.RebrickableClient
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    var currentTab by remember { mutableStateOf(0) } // 0 = keresés, 1 = mentett

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Felső "tab" gombsor
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { currentTab = 0 },
                modifier = Modifier.weight(1f)
            ) {
                Text("Keresés")
            }
            Button(
                onClick = { currentTab = 1 },
                modifier = Modifier.weight(1f)
            ) {
                Text("Mentett alkatrészek")
            }
        }

        // Tab tartalma
        when (currentTab) {
            0 -> PartSearchScreen()
            1 -> SavedPartsScreen()
        }
    }
}

/**
 * Partsám alapján keresés Rebrickable-en, mentés a DB-be,
 * és az eredmény kiírása.
 */
@Composable
fun PartSearchScreen() {
    val context = LocalContext.current
    val db = remember { LegoDatabase.getInstance(context) }
    val partDao = remember { db.partDao() }

    var partNumberState by remember { mutableStateOf(TextFieldValue("")) }
    var resultText by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = partNumberState,
            onValueChange = { partNumberState = it },
            label = { Text("Partsám (pl. 3001, 6558, stb.)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val partNum = partNumberState.text.trim()
                if (partNum.isBlank()) {
                    resultText = "Adj meg egy partszámot."
                    return@Button
                }

                scope.launch {
                    try {
                        resultText = "Keresés folyamatban..."

                        // 1) Próbáljuk meg először a cache-t (DB)
                        val cached = withContext(Dispatchers.IO) {
                            partDao.getPartById(partNum)
                        }

                        val part: PartEntity = if (cached != null) {
                            cached
                        } else {
                            // 2) Nincs cache -> Rebrickable API hívás
                            val api = RebrickableClient.api
                            val response = withContext(Dispatchers.IO) {
                                api.searchParts(partNum = partNum, pageSize = 1)
                            }

                            val dto = response.results.firstOrNull()
                                ?: throw IllegalArgumentException("Nem található ilyen partszám: $partNum")

                            val entity = PartEntity(
                                partId = dto.partNum,
                                name = dto.name,
                                imageUrl = dto.imageUrl
                            )

                            // 3) Mentés DB-be
                            withContext(Dispatchers.IO) {
                                partDao.insertPart(entity)
                            }

                            entity
                        }

                        // 4) Eredmény kiírása
                        resultText = buildString {
                            append("Alkatrész sikeresen mentve a DB-be.\n\n")
                            append("partId: ${part.partId}\n")
                            append("név: ${part.name}\n")
                            append("kép URL:\n${part.imageUrl ?: "-"}")
                        }
                    } catch (e: Exception) {
                        resultText = "Hiba történt:\n${e.message}"
                    }
                }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Keresés")
        }

        // Eredmény szöveg megjelenítése
        if (resultText.isNotEmpty()) {
            Text(text = resultText)
        }
    }
}

/**
 * Mentett alkatrészek listázása a "parts" táblából.
 */
@Composable
fun SavedPartsScreen() {
    val context = LocalContext.current
    val db = remember { LegoDatabase.getInstance(context) }
    val partDao = remember { db.partDao() }

    var parts by remember { mutableStateOf<List<PartEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            isLoading = true
            error = null
            parts = withContext(Dispatchers.IO) {
                partDao.getAllParts()
            }
        } catch (e: Exception) {
            error = e.message
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Mentett alkatrészek", style = MaterialTheme.typography.titleLarge)

        if (isLoading) {
            Text("Betöltés...")
        } else if (error != null) {
            Text("Hiba: $error")
        } else if (parts.isEmpty()) {
            Text("Még nincs mentett alkatrész.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(parts) { part ->
                    Column {
                        Text("partId: ${part.partId}")
                        Text("név: ${part.name}")
                        Text("kép URL: ${part.imageUrl ?: "-"}")
                    }
                }
            }
        }
    }
}
