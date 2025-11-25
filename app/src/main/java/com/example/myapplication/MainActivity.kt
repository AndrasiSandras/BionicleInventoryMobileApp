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
import com.example.myapplication.data.local.LegoDatabase
import com.example.myapplication.data.local.ListEntity
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
                    TestRoomScreen()
                }
            }
        }
    }
}

@Composable
fun TestRoomScreen() {
    val context = LocalContext.current
    val db = remember { LegoDatabase.getInstance(context) }

    var text by remember { mutableStateOf("Betöltés...") }

    LaunchedEffect(Unit) {
        val dao = db.listDao()

        dao.deleteAll()
        dao.insertList(ListEntity(name = "Teszt lista"))
        val lists = dao.getAllLists()

        text = buildString {
            append("DB-ben lévő listák száma: ${lists.size}\n")
            append("Első lista neve: ${lists.firstOrNull()?.name ?: "nincs"}")
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text)
    }
}
