package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import com.example.myapplication.data.local.ListEntity
import com.example.myapplication.data.local.ListItemEntity

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
    var currentTab by remember { mutableStateOf(0) } // 0 = keresés, 1 = mentett, 2 = listák

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
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
                Text("Mentett")
            }
            Button(
                onClick = { currentTab = 2 },
                modifier = Modifier.weight(1f)
            ) {
                Text("Listák")
            }
        }

        when (currentTab) {
            0 -> PartSearchScreen()
            1 -> SavedPartsScreen()
            2 -> ListsScreen()          // <- ÚJ composable
        }
    }
}

/**
 * Partsám alapján keresés Rebrickable-en, mentés a DB-be,
 * és az eredmény kiírása.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartSearchScreen() {
    val context = LocalContext.current
    val db = remember { LegoDatabase.getInstance(context) }
    val partDao = remember { db.partDao() }
    val listDao = remember { db.listDao() }
    val listItemDao = remember { db.listItemDao() }

    var partNumberState by remember { mutableStateOf(TextFieldValue("")) }
    var resultText by remember { mutableStateOf("") }

    // Listák
    var lists by remember { mutableStateOf<List<ListEntity>>(emptyList()) }
    var selectedList by remember { mutableStateOf<ListEntity?>(null) }
    var listsError by remember { mutableStateOf<String?>(null) }

    // Dropdown állapot
    var expanded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Listák betöltése
    LaunchedEffect(Unit) {
        try {
            lists = withContext(Dispatchers.IO) {
                listDao.getAllLists()
            }
            if (lists.isNotEmpty()) {
                selectedList = lists.first()
            }
        } catch (e: Exception) {
            listsError = e.message
        }
    }

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

        // --- CÉL LISTA VÁLASZTÓ ---

        if (listsError != null) {
            Text("Hiba a listák betöltésekor: $listsError")
        } else if (lists.isEmpty()) {
            Text("Nincs még egyetlen lista sem. Hozz létre a 'Listák' fülön.")
        } else {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedList?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Cél lista") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    lists.forEach { list ->
                        DropdownMenuItem(
                            text = { Text(list.name) },
                            onClick = {
                                selectedList = list
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        // --- KERESÉS GOMB + MENTÉS ---

        Button(
            onClick = {
                val partNum = partNumberState.text.trim()
                if (partNum.isBlank()) {
                    resultText = "Adj meg egy partszámot."
                    return@Button
                }
                if (lists.isNotEmpty() && selectedList == null) {
                    resultText = "Válassz egy listát."
                    return@Button
                }

                scope.launch {
                    try {
                        resultText = "Keresés folyamatban..."

                        // 1) Cache
                        val cached = withContext(Dispatchers.IO) {
                            partDao.getPartById(partNum)
                        }

                        val part: PartEntity = if (cached != null) {
                            cached
                        } else {
                            // 2) API
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

                            withContext(Dispatchers.IO) {
                                partDao.insertPart(entity)
                            }

                            entity
                        }

                        // 3) Kapcsolat a listához
                        selectedList?.let { list ->
                            withContext(Dispatchers.IO) {
                                val item = ListItemEntity(
                                    listId = list.id,
                                    partId = part.partId,
                                    colorId = -1,
                                    quantity = 1
                                )
                                listItemDao.insertItem(item)
                            }
                        }

                        resultText = buildString {
                            append("Alkatrész mentve a DB-be és a listába.\n\n")
                            append("Lista: ${selectedList?.name ?: "-"}\n\n")
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
@Composable
fun ListsScreen() {
    val context = LocalContext.current
    val db = remember { LegoDatabase.getInstance(context) }
    val listDao = remember { db.listDao() }

    var lists by remember { mutableStateOf<List<ListEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var newListName by remember { mutableStateOf(TextFieldValue("")) }
    var selectedList by remember { mutableStateOf<ListEntity?>(null) }

    val scope = rememberCoroutineScope()

    // Listák betöltése
    LaunchedEffect(Unit) {
        try {
            isLoading = true
            error = null
            lists = withContext(Dispatchers.IO) {
                listDao.getAllLists()
            }
            selectedList = lists.firstOrNull()
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
        Text("Listák", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        // Új lista létrehozása
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newListName,
                onValueChange = { newListName = it },
                label = { Text("Új lista neve") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    val name = newListName.text.trim()
                    if (name.isBlank()) return@Button

                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                listDao.insertList(ListEntity(name = name))
                            }
                            newListName = TextFieldValue("")
                            // frissítjük a listákat
                            lists = withContext(Dispatchers.IO) {
                                listDao.getAllLists()
                            }
                            selectedList = lists.firstOrNull()
                        } catch (e: Exception) {
                            error = e.message
                        }
                    }
                }
            ) {
                Text("Hozzáadás")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> Text("Betöltés...")
            error != null -> Text("Hiba: $error")
            lists.isEmpty() -> Text("Még nincs egyetlen lista sem.")
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(lists) { list ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedList = list },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = list.name +
                                            if (selectedList?.id == list.id) " (aktuális)" else ""
                                )
                                Text("ID: ${list.id}", style = MaterialTheme.typography.bodySmall)
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                listDao.deleteListById(list.id)
                                            }
                                            lists = withContext(Dispatchers.IO) {
                                                listDao.getAllLists()
                                            }
                                            // ha a törölt lista volt kiválasztva, válasszunk másikat
                                            selectedList = lists.firstOrNull()
                                        } catch (e: Exception) {
                                            error = e.message
                                        }
                                    }
                                }
                            ) {
                                Text("Törlés")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Kiválasztott lista részletei
        ListDetailSection(selectedList = selectedList)
    }
}

@Composable
fun ListDetailSection(selectedList: ListEntity?) {
    val context = LocalContext.current
    val db = remember { LegoDatabase.getInstance(context) }
    val listItemDao = remember { db.listItemDao() }
    val partDao = remember { db.partDao() }

    var itemsWithPart by remember {
        mutableStateOf<List<Pair<ListItemEntity, PartEntity?>>>(emptyList())
    }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Ha változik a kiválasztott lista, újratöltjük a tartalmát
    LaunchedEffect(selectedList?.id) {
        if (selectedList == null) {
            itemsWithPart = emptyList()
            return@LaunchedEffect
        }

        try {
            isLoading = true
            error = null

            val listItems = withContext(Dispatchers.IO) {
                listItemDao.getItemsForList(selectedList.id)
            }

            val pairs = withContext(Dispatchers.IO) {
                listItems.map { item ->
                    val part = partDao.getPartById(item.partId)
                    item to part
                }
            }

            itemsWithPart = pairs
        } catch (e: Exception) {
            error = e.message
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            text = "Lista részletei",
            style = MaterialTheme.typography.titleMedium
        )

        if (selectedList == null) {
            Text("Válassz egy listát a fenti listából.")
            return
        }

        Text("Aktuális lista: ${selectedList.name}")

        when {
            isLoading -> Text("Betöltés...")
            error != null -> Text("Hiba: $error")
            itemsWithPart.isEmpty() -> Text("Ebben a listában még nincs egyetlen alkatrész sem.")
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(itemsWithPart) { (item, part) ->
                        Column {
                            Text("partId: ${item.partId}")
                            Text("név: ${part?.name ?: "ismeretlen (nincs a parts táblában)"}")
                            Text("mennyiség: ${item.quantity}")
                        }
                    }
                }
            }
        }
    }
}

