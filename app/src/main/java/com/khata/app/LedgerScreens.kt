@file:OptIn(ExperimentalMaterial3Api::class)

package com.khata.app

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.math.abs

// ---------------------------------------------------------------- Customers
@Composable
fun CustomersScreen(vm: KhataVM, nav: NavHostController) {
    val bals by vm.balances.collectAsState()
    val profile by vm.profile.collectAsState()
    var q by remember { mutableStateOf("") }
    val rec = bals.filter { it.net > 0 }.sumOf { it.net }
    val pay = bals.filter { it.net < 0 }.sumOf { -it.net }
    val overall = r2(rec - pay)
    val shown = bals.filter { q.isBlank() || it.customer.name.contains(q, true) || it.customer.phone.contains(q) }
    val biz = profile?.businessName.orEmpty()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate("customer_form") }, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                Icon(Icons.Default.Add, "Add customer")
            }
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
            item {
                Column(Modifier.statusBarsPadding().padding(16.dp)) {
                    Text("Khata", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    if (biz.isNotBlank()) Text(biz, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF0B6E6E), Color(0xFF0F3D5E)))).padding(20.dp)
                    ) {
                        Column {
                            Text("Overall Balance", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                            Text(signed(overall), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            Text(
                                when { overall > 0 -> "Net receivable"; overall < 0 -> "Net payable"; else -> "All settled" },
                                color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp
                            )
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                DashStat("Total Receivable", money(rec), Modifier.weight(1f))
                                DashStat("Total Payable", money(pay), Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = q, onValueChange = { q = it }, singleLine = true,
                    placeholder = { Text("Search customers") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = { if (q.isNotEmpty()) IconButton(onClick = { q = "" }) { Icon(Icons.Default.Close, "Clear") } },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            items(shown, key = { it.customer.id }) { b ->
                val st = statusOf(b.net)
                ElevatedCard(
                    onClick = { nav.navigate("ledger/${b.customer.id}") },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(b.customer.name)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(b.customer.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (b.customer.phone.isNotBlank()) Text(b.customer.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(money(abs(b.net)), fontWeight = FontWeight.Bold, color = st.color, fontSize = 17.sp)
                            Spacer(Modifier.height(4.dp))
                            StatusChip(st)
                        }
                    }
                }
            }
            if (shown.isEmpty()) item {
                Text(
                    if (bals.isEmpty()) "No customers yet.\nTap + to add your first customer." else "No customers match your search.",
                    textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(40.dp)
                )
            }
        }
    }
}

@Composable
private fun DashStat(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.14f)).padding(12.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1)
    }
}

// ---------------------------------------------------------------- Add / Edit customer
@Composable
fun CustomerFormScreen(vm: KhataVM, nav: NavHostController, id: Long) {
    val bals by vm.balances.collectAsState()
    val existing = bals.firstOrNull { it.customer.id == id }?.customer
    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }
    var phone by remember(existing?.id) { mutableStateOf(existing?.phone ?: "") }
    var note by remember(existing?.id) { mutableStateOf(existing?.note ?: "") }
    var err by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { KTopBar(if (id > 0) "Edit Customer" else "New Customer", nav) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = name, onValueChange = { name = it; err = null }, label = { Text("Name *") }, singleLine = true,
                isError = err != null, supportingText = { err?.let { Text(it) } }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone, onValueChange = { phone = it }, label = { Text("Phone (optional)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = note, onValueChange = { note = it }, label = { Text("Note (optional)") }, minLines = 2,
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (name.isBlank()) err = "Customer name is required" else {
                        val c = (existing ?: Customer(name = "")).copy(name = name.trim(), phone = phone.trim(), note = note.trim())
                        vm.saveCustomer(c) { newId ->
                            if (existing == null) nav.navigate("ledger/$newId") { popUpTo("customers") } else nav.popBackStack()
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Save", fontSize = 16.sp) }
        }
    }
}

// ---------------------------------------------------------------- Ledger
@Composable
fun LedgerScreen(vm: KhataVM, nav: NavHostController, id: Long) {
    val ctx = LocalContext.current
    val bals by vm.balances.collectAsState()
    val all by vm.txns.collectAsState()
    var editor by remember { mutableStateOf<Pair<String, Txn?>?>(null) }
    var picked by remember { mutableStateOf<Txn?>(null) }
    var delTxn by remember { mutableStateOf<Txn?>(null) }
    var delCust by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }

    val b = bals.firstOrNull { it.customer.id == id }
    if (b == null) { Box(Modifier.fillMaxSize()); return }
    val c = b.customer
    val list = all.filter { it.customerId == id }
    val st = statusOf(b.net)

    Scaffold(topBar = {
        TopAppBar(
            title = {
                Column {
                    Text(c.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (c.phone.isNotBlank()) Text(c.phone, style = MaterialTheme.typography.bodySmall)
                }
            },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } },
            actions = {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Menu") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Edit customer") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { menu = false; nav.navigate("customer_form?id=$id") })
                    DropdownMenuItem(text = { Text("Delete customer") }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { menu = false; delCust = true })
                }
            }
        )
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            ElevatedCard(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Current Balance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(money(abs(b.net)), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = st.color)
                        }
                        StatusChip(st)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MiniStat("Total Received", money(b.received), Green, Modifier.weight(1f))
                        MiniStat("Total Given", money(b.given), Red, Modifier.weight(1f))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { editor = T.R to null }, colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.White), shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(4.dp), modifier = Modifier.weight(1f).height(48.dp)) { Text("Received") }
                Button(onClick = { editor = T.G to null }, colors = ButtonDefaults.buttonColors(containerColor = Red, contentColor = Color.White), shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(4.dp), modifier = Modifier.weight(1f).height(48.dp)) { Text("Given") }
                OutlinedButton(onClick = { sendSms(ctx, c, b.net) }, shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(4.dp), modifier = Modifier.weight(1f).height(48.dp)) { Text("Message") }
            }
            if (list.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No transactions yet.\nTap Received or Given to add one.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val groups = list.groupBy { it.date }
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), reverseLayout = true, contentPadding = PaddingValues(16.dp, 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    groups.forEach { (d, txs) ->
                        items(txs, key = { it.id }) { t -> Bubble(t) { picked = t } }
                        item(key = "h$d") {
                            Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    niceDate(d), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    picked?.let { t ->
        AlertDialog(
            onDismissRequest = { picked = null },
            title = { Text((if (t.type == T.R) "Received " else "Given ") + money(t.amount)) },
            text = { Text("${niceDate(t.date)} · ${LocalTime.parse(t.time).format(timeFmt)}" + if (t.note.isNotBlank()) "\n${t.note}" else "") },
            confirmButton = { TextButton(onClick = { picked = null; editor = t.type to t }) { Text("Edit") } },
            dismissButton = { TextButton(onClick = { picked = null; delTxn = t }) { Text("Delete", color = Red) } }
        )
    }
    editor?.let { (type, txn) ->
        TxnDialog(id, type, txn, onDismiss = { editor = null }, onSave = { vm.saveTxn(it); editor = null })
    }
    delTxn?.let { t ->
        AlertDialog(
            onDismissRequest = { delTxn = null },
            title = { Text("Delete transaction?") },
            text = { Text("This will remove ${money(t.amount)} and update the balance.") },
            confirmButton = { TextButton(onClick = { vm.deleteTxn(t); delTxn = null }) { Text("Delete", color = Red) } },
            dismissButton = { TextButton(onClick = { delTxn = null }) { Text("Cancel") } }
        )
    }
    if (delCust) {
        AlertDialog(
            onDismissRequest = { delCust = false },
            title = { Text("Delete customer?") },
            text = { Text("Delete ${c.name} and all their transactions? This cannot be undone.") },
            confirmButton = { TextButton(onClick = { delCust = false; vm.deleteCustomer(c); nav.popBackStack() }) { Text("Delete", color = Red) } },
            dismissButton = { TextButton(onClick = { delCust = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.10f)).padding(10.dp)) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold, color = color, fontSize = 16.sp, maxLines = 1)
    }
}

@Composable
private fun Bubble(t: Txn, onClick: () -> Unit) {
    val rec = t.type == T.R
    val col = if (rec) Green else Red
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (rec) Arrangement.Start else Arrangement.End) {
        Surface(
            onClick = onClick, color = col.copy(alpha = 0.12f),
            shape = RoundedCornerShape(16.dp, 16.dp, if (rec) 16.dp else 4.dp, if (rec) 4.dp else 16.dp),
            border = BorderStroke(1.dp, col.copy(alpha = 0.35f)),
            modifier = Modifier.widthIn(min = 140.dp, max = 290.dp)
        ) {
            Column(Modifier.padding(12.dp, 8.dp)) {
                Text(if (rec) "Received" else "Given", color = col, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                Text(money(t.amount), color = col, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (t.note.isNotBlank()) Text(t.note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(LocalTime.parse(t.time).format(timeFmt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.End))
            }
        }
    }
}

// ---------------------------------------------------------------- Add / Edit transaction dialog
@Composable
fun TxnDialog(customerId: Long, type: String, existing: Txn?, onDismiss: () -> Unit, onSave: (Txn) -> Unit) {
    var amount by remember { mutableStateOf(existing?.let { plain(it.amount) } ?: "") }
    var date by remember { mutableStateOf(existing?.let { LocalDate.parse(it.date) } ?: LocalDate.now()) }
    var time by remember { mutableStateOf(existing?.let { LocalTime.parse(it.time) } ?: LocalTime.now().withSecond(0).withNano(0)) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var err by remember { mutableStateOf<String?>(null) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    val rec = type == T.R
    val col = if (rec) Green else Red

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(if (rec) "Received" else "Given", color = col, fontWeight = FontWeight.Bold)
                Text(if (rec) "Customer gave money to me" else "I gave money to customer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amount, onValueChange = { v -> if (v.all { it.isDigit() || it == '.' }) { amount = v; err = null } },
                    label = { Text("Amount *") }, prefix = { Text("₹") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = err != null, supportingText = { err?.let { Text(it) } }, modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showDate = true }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 6.dp)) {
                        Icon(Icons.Default.DateRange, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(date.format(dateFmt), fontSize = 13.sp, maxLines = 1)
                    }
                    OutlinedButton(onClick = { showTime = true }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 6.dp)) {
                        Text(time.format(timeFmt), fontSize = 13.sp, maxLines = 1)
                    }
                }
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note (optional)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                val a = amount.toDoubleOrNull()
                if (a == null || a <= 0) err = "Enter an amount greater than 0" else
                    onSave(Txn(id = existing?.id ?: 0, customerId = customerId, type = type, amount = r2(a), date = date.toString(), time = time.toString(), note = note.trim()))
            }, colors = ButtonDefaults.buttonColors(containerColor = col, contentColor = Color.White)) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showDate) {
        val ds = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = { TextButton(onClick = { ds.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }; showDate = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } }
        ) { DatePicker(state = ds) }
    }
    if (showTime) {
        val ts = rememberTimePickerState(time.hour, time.minute, false)
        AlertDialog(
            onDismissRequest = { showTime = false },
            text = { TimePicker(state = ts) },
            confirmButton = { TextButton(onClick = { time = LocalTime.of(ts.hour, ts.minute); showTime = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Cancel") } }
        )
    }
}
