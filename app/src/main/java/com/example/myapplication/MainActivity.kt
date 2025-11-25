package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.local.LegoDatabase
import com.example.myapplication.data.local.PartEntity
import com.example.myapplication.data.remote.RebrickableClient
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PartSearchScreen()
                }
            }
        }
    }
}

@Composable
fun PartSearchScreen() {
    val context = LocalContext.current
    val db = remember { LegoDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()

    var partNum by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("Adj meg egy LEGO partszámot és nyomd meg a Keresés gombot.") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        OutlinedTextField(
            value = partNum,
            onValueChange = { partNum = it },
            label = { Text("Partsám (pl. 3001, 6558, stb.)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Ascii
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (partNum.isBlank()) {
                    statusText = "Először írj be egy partszámot."
                    return@Button
                }

                scope.launch {
                    try {
                        statusText = "Keresés folyamatban..."

                        val api = RebrickableClient.api
                        val response = api.searchParts(partNum = partNum, pageSize = 1)

                        val dto = response.results.firstOrNull()
                        if (dto == null) {
                            statusText = "Nem találtam alkatrészt ezzel a számmal: $partNum"
                            return@launch
                        }

                        val partDao = db.partDao()

                        // API -> Entity
                        val entity = PartEntity(
                            partId = dto.partNum,
                            name = dto.name,
                            imageUrl = dto.imageUrl
                        )

                        // mentés DB-be
                        partDao.insertPart(entity)

                        // visszaolvasás DB-ből (bizonyításképp)
                        val fromDb = partDao.getPartById(dto.partNum)

                        statusText = buildString {
                            append("Alkatrész sikeresen mentve a DB-be.\n\n")
                            append("partId: ${fromDb?.partId}\n")
                            append("név: ${fromDb?.name}\n")
                            append("kép URL: ${fromDb?.imageUrl ?: "nincs"}")
                        }
                    } catch (e: Exception) {
                        statusText = "Hiba történt:\n${e.message}"
                    }
                }
            }
        ) {
            Text("Keresés")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
