package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.data.local.ColorEntity
import com.example.myapplication.data.local.LegoDatabase
import com.example.myapplication.data.remote.RebrickableClient
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TestColorsDbScreen()
                }
            }
        }
    }
}

@Composable
fun TestColorsDbScreen() {
    val context = LocalContext.current
    val db = remember { LegoDatabase.getInstance(context) }

    var text by remember { mutableStateOf("Színek betöltése...") }

    LaunchedEffect(Unit) {
        try {
            val api = RebrickableClient.api
            // API-ból 20 színt kérünk
            val response = api.getColors(pageSize = 20)

            val colorDao = db.colorDao()

            // Régiek törlése
            colorDao.deleteAll()

            // API DTO -> Room Entity
            val entities = response.results.map { dto ->
                ColorEntity(
                    id = dto.id,
                    name = dto.name,
                    rgb = dto.rgb
                )
            }

            // Beszúrás DB-be
            colorDao.insertAll(entities)

            // Kiolvasás DB-ből
            val colorsFromDb = colorDao.getAllColors()

            text = buildString {
                append("DB-ben lévő színek száma: ${colorsFromDb.size}\n\n")
                colorsFromDb.take(10).forEach { color ->
                    append("• ${color.id}: ${color.name} (${color.rgb})\n")
                }
            }
        } catch (e: Exception) {
            text = "Hiba történt:\n${e.message}"
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text)
    }
}
