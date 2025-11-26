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
import androidx.compose.material3.OutlinedButton
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
import com.example.myapplication.data.local.ColorEntity
import com.example.myapplication.data.local.ListEntity
import com.example.myapplication.data.local.ListItemEntity
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.myapplication.data.local.PartColorImageEntity

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

data class ListItemWithDetails(
    val item: ListItemEntity,
    val part: PartEntity?,
    val color: ColorEntity?,
    val imageUrl: String?
)

@Composable
fun ListItemRow(
    detail: ListItemWithDetails,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!detail.imageUrl.isNullOrEmpty()) {
            AsyncImage(
                model = detail.imageUrl,
                contentDescription = detail.part?.name,
                modifier = Modifier.size(64.dp),
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text("partId: ${detail.item.partId}")
            Text("név: ${detail.part?.name ?: "ismeretlen (nincs a parts táblában)"}")
            Text("szín: ${detail.color?.name ?: "nincs megadva"}")
            Text("mennyiség: ${detail.item.quantity}")
        }

        Column {
            Row {
                Button(onClick = onDecrease) {
                    Text("-")
                }
                Spacer(Modifier.width(4.dp))
                Button(onClick = onIncrease) {
                    Text("+")
                }
            }
            Spacer(Modifier.height(4.dp))
            OutlinedButton(onClick = onDelete) {
                Text("Törlés")
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
    val colorDao = remember { db.colorDao() }
    val partColorImageDao = remember { db.partColorImageDao() }

    var partNumberState by remember { mutableStateOf(TextFieldValue("")) }
    var quantityState by remember { mutableStateOf(TextFieldValue("1")) }
    var resultText by remember { mutableStateOf("") }

    // aktuálisan betöltött part (keresés után)
    var currentPart by remember { mutableStateOf<PartEntity?>(null) }

    // Listák
    var lists by remember { mutableStateOf<List<ListEntity>>(emptyList()) }
    var selectedList by remember { mutableStateOf<ListEntity?>(null) }
    var listsError by remember { mutableStateOf<String?>(null) }

    // Színek
    var allColors by remember { mutableStateOf<List<ColorEntity>>(emptyList()) }      // teljes színlista DB-ből
    var availableColors by remember { mutableStateOf<List<ColorEntity>>(emptyList()) } // adott part-hoz engedélyezett színek
    var selectedColor by remember { mutableStateOf<ColorEntity?>(null) }
    var colorsError by remember { mutableStateOf<String?>(null) }

    // Dropdown állapotok
    var listExpanded by remember { mutableStateOf(false) }
    var colorExpanded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Listák + globális színlista betöltése
    LaunchedEffect(Unit) {
        try {
            // listák
            lists = withContext(Dispatchers.IO) {
                listDao.getAllLists()
            }
            selectedList = lists.firstOrNull()

            // színek
            var storedColors = withContext(Dispatchers.IO) {
                colorDao.getAllColors()
            }

            if (storedColors.isEmpty()) {
                val api = RebrickableClient.api
                val response = withContext(Dispatchers.IO) {
                    api.getColors(pageSize = 1000)
                }
                val entities = response.results.map { dto ->
                    ColorEntity(
                        id = dto.id,
                        name = dto.name,
                        rgb = dto.rgb
                    )
                }
                withContext(Dispatchers.IO) {
                    colorDao.insertColors(entities)
                    storedColors = colorDao.getAllColors()
                }
            }

            allColors = storedColors
        } catch (e: Exception) {
            colorsError = e.message
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Partsám
        OutlinedTextField(
            value = partNumberState,
            onValueChange = { partNumberState = it },
            label = { Text("Partsám (pl. 3001, 6558, stb.)") },
            modifier = Modifier.fillMaxWidth()
        )

        // Darabszám
        OutlinedTextField(
            value = quantityState,
            onValueChange = { quantityState = it },
            label = { Text("Darabszám") },
            modifier = Modifier.fillMaxWidth()
        )

        // Cél lista választó
        if (listsError != null) {
            Text("Hiba a listák betöltésekor: $listsError")
        } else if (lists.isEmpty()) {
            Text("Nincs még egyetlen lista sem. Hozz létre a 'Listák' fülön.")
        } else {
            ExposedDropdownMenuBox(
                expanded = listExpanded,
                onExpandedChange = { listExpanded = !listExpanded }
            ) {
                OutlinedTextField(
                    value = selectedList?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Cél lista") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = listExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = listExpanded,
                    onDismissRequest = { listExpanded = false }
                ) {
                    lists.forEach { list ->
                        DropdownMenuItem(
                            text = { Text(list.name) },
                            onClick = {
                                selectedList = list
                                listExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Szín választó (csak az elérhető színek, ha már volt keresés)
        val dropdownColors =
            if (availableColors.isNotEmpty()) availableColors else allColors

        if (colorsError != null) {
            Text("Hiba a színek betöltésekor: $colorsError")
        } else if (dropdownColors.isEmpty()) {
            Text("Nincsenek színek betöltve.")
        } else {
            ExposedDropdownMenuBox(
                expanded = colorExpanded,
                onExpandedChange = { colorExpanded = !colorExpanded }
            ) {
                OutlinedTextField(
                    value = selectedColor?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Szín") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = colorExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = colorExpanded,
                    onDismissRequest = { colorExpanded = false }
                ) {
                    dropdownColors.forEach { color ->
                        DropdownMenuItem(
                            text = { Text("${color.id}: ${color.name}") },
                            onClick = {
                                selectedColor = color
                                colorExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Gombok: Keresés / Hozzáadás
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1) KERESÉS – csak part + érvényes színek, NEM ad hozzá listához
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

                            // part a DB-ből vagy API-ról
                            val part = withContext(Dispatchers.IO) {
                                partDao.getPartById(partNum)
                            } ?: run {
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

                            currentPart = part

                            // part-hoz érvényes színek
                            val api = RebrickableClient.api
                            val partColors = withContext(Dispatchers.IO) {
                                api.getPartColors(part.partId).results
                            }
                            val allowedColorIds = partColors.map { it.colorId }.toSet()

                            val filteredColors = allColors.filter { it.id in allowedColorIds }

                            if (filteredColors.isNotEmpty()) {
                                availableColors = filteredColors
                                selectedColor =
                                    if (selectedColor != null && selectedColor!!.id in allowedColorIds)
                                        selectedColor
                                    else
                                        filteredColors.first()
                            } else {
                                // nincs kifejezett színlista ehhez a part-hoz
                                availableColors = emptyList()
                                selectedColor = null
                            }

                            resultText =
                                "Alkatrész betöltve. Válassz színt és darabszámot, majd nyomd meg a 'Hozzáadás' gombot."
                        } catch (e: Exception) {
                            resultText = "Hiba keresés közben:\n${e.message}"
                            currentPart = null
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Keresés")
            }

            // 2) HOZZÁADÁS – csak ListItemEntity beszúrás
            Button(
                onClick = {
                    val part = currentPart
                    if (part == null) {
                        resultText = "Előbb keresd meg az alkatrészt."
                        return@Button
                    }

                    if (lists.isNotEmpty() && selectedList == null) {
                        resultText = "Válassz egy listát."
                        return@Button
                    }

                    val qty = quantityState.text.trim().toIntOrNull()
                    if (qty == null || qty <= 0) {
                        resultText = "A darabszám pozitív egész szám legyen."
                        return@Button
                    }

                    if (availableColors.isNotEmpty() && selectedColor == null) {
                        resultText = "Válassz egy színt."
                        return@Button
                    }

                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                val colorId = selectedColor?.id ?: -1

                                // Színhez tartozó kép URL lekérése (cache + API)
                                var coloredImageUrl: String? = null
                                if (colorId >= 0) {
                                    // 1) megpróbáljuk a saját táblából
                                    coloredImageUrl = partColorImageDao
                                        .getImage(part.partId, colorId)
                                        ?.imageUrl

                                    // 2) ha nincs elmentve, lehúzzuk az API-ról és elmentjük
                                    if (coloredImageUrl == null) {
                                        val api = RebrickableClient.api
                                        val detail = api.getPartColorDetail(part.partId, colorId)
                                        val urlFromApi = detail.partImgUrl
                                        if (urlFromApi != null) {
                                            partColorImageDao.insertImage(
                                                PartColorImageEntity(
                                                    partId = part.partId,
                                                    colorId = colorId,
                                                    imageUrl = urlFromApi
                                                )
                                            )
                                            coloredImageUrl = urlFromApi
                                        }
                                    }
                                }

                                // ListItem mentése (ugyanúgy, mint eddig)
                                val item = ListItemEntity(
                                    listId = selectedList!!.id,
                                    partId = part.partId,
                                    colorId = colorId,
                                    quantity = qty
                                )
                                listItemDao.insertItem(item)
                            }

                            resultText = buildString {
                                append("Alkatrész hozzáadva a listához.\n\n")
                                append("Lista: ${selectedList?.name ?: "-"}\n")
                                append("Darabszám: $qty\n")
                                append("Szín: ${selectedColor?.name ?: "-"}\n\n")
                                append("partId: ${part.partId}\n")
                                append("név: ${part.name}\n")
                                append("kép URL:\n${part.imageUrl ?: "-"}")
                            }
                        } catch (e: Exception) {
                            resultText = "Hiba mentés közben:\n${e.message}"
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Hozzáadás")
            }
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!part.imageUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = part.imageUrl,
                                contentDescription = part.name,
                                modifier = Modifier.size(64.dp),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("partId: ${part.partId}")
                            Text("név: ${part.name}")
                            if (part.imageUrl != null) {
                                Text("kép: van kép")
                            } else {
                                Text("kép: nincs URL")
                            }
                        }
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
    val colorDao = remember { db.colorDao() }
    val partColorImageDao = remember { db.partColorImageDao() }

    var itemsWithDetails by remember {
        mutableStateOf<List<ListItemWithDetails>>(emptyList())
    }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    // közös betöltő függvény – ezt hívjuk LaunchedEffectből és módosítás után is
    suspend fun reloadDetails(list: ListEntity) {
        isLoading = true
        error = null
        try {
            val listItems = withContext(Dispatchers.IO) {
                listItemDao.getItemsForList(list.id)
            }

            val details = withContext(Dispatchers.IO) {
                listItems.map { item ->
                    val part = partDao.getPartById(item.partId)
                    val color = if (item.colorId >= 0) {
                        colorDao.getColorById(item.colorId)
                    } else {
                        null
                    }

                    val coloredImage = if (item.colorId >= 0) {
                        partColorImageDao.getImage(item.partId, item.colorId)
                    } else {
                        null
                    }

                    val imgUrl = coloredImage?.imageUrl ?: part?.imageUrl

                    ListItemWithDetails(
                        item = item,
                        part = part,
                        color = color,
                        imageUrl = imgUrl
                    )
                }
            }

            itemsWithDetails = details
        } catch (e: Exception) {
            error = e.message
        } finally {
            isLoading = false
        }
    }

    // amikor másik listát választasz, töltsük újra annak az adatait
    LaunchedEffect(selectedList?.id) {
        if (selectedList != null) {
            reloadDetails(selectedList)
        } else {
            itemsWithDetails = emptyList()
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
            itemsWithDetails.isEmpty() -> Text("Ebben a listában még nincs egyetlen alkatrész sem.")
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(itemsWithDetails) { detail ->
                        ListItemRow(
                            detail = detail,
                            onIncrease = {
                                scope.launch {
                                    val newQty = detail.item.quantity + 1
                                    listItemDao.updateQuantity(detail.item.id, newQty)
                                    reloadDetails(selectedList)
                                }
                            },
                            onDecrease = {
                                scope.launch {
                                    val newQty = (detail.item.quantity - 1).coerceAtLeast(1)
                                    listItemDao.updateQuantity(detail.item.id, newQty)
                                    reloadDetails(selectedList)
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    listItemDao.deleteItem(detail.item)
                                    reloadDetails(selectedList)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

