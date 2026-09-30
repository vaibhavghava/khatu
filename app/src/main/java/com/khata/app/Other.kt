@file:OptIn(ExperimentalMaterial3Api::class)

package com.khata.app

import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalTime

// ---------------------------------------------------------------- Calculator
object Calc {
    fun eval(s: String): String? = try {
        val p = P(s)
        val v = p.expr()
        if (p.i < s.length) null else v.setScale(10, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    } catch (e: Exception) { null }

    private class P(val s: String) {
        var i = 0
        fun expr(): BigDecimal {
            var v = term()
            while (i < s.length && (s[i] == '+' || s[i] == '-')) {
                val op = s[i++]; val r = term()
                v = if (op == '+') v.add(r) else v.subtract(r)
            }
            return v
        }
        fun term(): BigDecimal {
            var v = factor()
            while (i < s.length && (s[i] == '×' || s[i] == '÷')) {
                val op = s[i++]; val r = factor()
                v = if (op == '×') v.multiply(r) else { if (r.signum() == 0) throw ArithmeticException(); v.divide(r, MathContext.DECIMAL64) }
            }
            return v
        }
        fun factor(): BigDecimal {
            var neg = false
            if (i < s.length && s[i] == '-') { neg = true; i++ }
            val st = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            var v = BigDecimal(s.substring(st, i))
            while (i < s.length && s[i] == '%') { v = v.divide(BigDecimal(100)); i++ }
            return if (neg) v.negate() else v
        }
    }
}

@Composable
fun CalculatorScreen() {
    var expr by rememberSaveable { mutableStateOf("") }
    var hist by rememberSaveable { mutableStateOf("") }
    var fresh by rememberSaveable { mutableStateOf(false) }

    fun press(k: String) {
        when (k) {
            "C" -> { expr = ""; hist = ""; fresh = false }
            "⌫" -> if (fresh) { expr = ""; hist = ""; fresh = false } else expr = expr.dropLast(1)
            "=" -> if (expr.isNotEmpty()) {
                val r = Calc.eval(expr)
                if (r == null) hist = "Invalid expression" else { hist = "$expr ="; expr = r; fresh = true }
            }
            "+", "-", "×", "÷" -> {
                fresh = false
                if (expr.isEmpty()) { if (k == "-") expr = "-" }
                else if (expr.last() in "+-×÷") { if (expr.length > 1) expr = expr.dropLast(1) + k }
                else expr += k
            }
            "%" -> { fresh = false; if (expr.isNotEmpty() && (expr.last().isDigit() || expr.last() == '%')) expr += "%" }
            "." -> if (fresh) { expr = "0."; hist = ""; fresh = false } else if (expr.lastOrNull() != '%') {
                val seg = expr.takeLastWhile { it.isDigit() || it == '.' }
                if (!seg.contains('.')) expr += if (seg.isEmpty()) "0." else "."
            }
            else -> if (fresh) { expr = k; hist = ""; fresh = false } else if (expr.lastOrNull() != '%') expr += k
        }
    }

    val rows = listOf(listOf("C", "⌫", "%", "÷"), listOf("7", "8", "9", "×"), listOf("4", "5", "6", "-"), listOf("1", "2", "3", "+"), listOf("0", ".", "="))
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
        Text("Calculator", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Column(Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.End) {
            Text(hist, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
            Text(
                expr.ifEmpty { "0" }.replace("-", "−"), fontWeight = FontWeight.Bold, maxLines = 2, textAlign = TextAlign.End,
                fontSize = if (expr.length > 12) 32.sp else if (expr.length > 8) 44.sp else 56.sp
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { k ->
                        val op = k in listOf("÷", "×", "-", "+")
                        val bg = when {
                            k == "=" -> MaterialTheme.colorScheme.primary
                            op -> MaterialTheme.colorScheme.primaryContainer
                            k == "C" || k == "⌫" || k == "%" -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val fg = when {
                            k == "=" -> MaterialTheme.colorScheme.onPrimary
                            op -> MaterialTheme.colorScheme.onPrimaryContainer
                            k == "C" -> Red
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        Surface(
                            onClick = { press(k) }, color = bg, shape = RoundedCornerShape(20.dp), shadowElevation = 1.dp,
                            modifier = Modifier.weight(if (k == "0") 2f else 1f).height(66.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(if (k == "-") "−" else k, color = fg, fontSize = 26.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- More
@Composable
fun MoreScreen(nav: NavHostController) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("More", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        MoreRow(Icons.Default.List, "All Transactions", "Every entry across all customers") { nav.navigate("all_txns") }
        MoreRow(Icons.Default.Settings, "App Settings", "Theme and preferences") { nav.navigate("settings") }
        MoreRow(Icons.Default.Info, "About", "About Khata") { nav.navigate("about") }
    }
}

@Composable
private fun MoreRow(icon: ImageVector, title: String, sub: String, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------------------------------------------------------------- All transactions
@Composable
fun AllTxnsScreen(vm: KhataVM, nav: NavHostController) {
    val txns by vm.txns.collectAsState()
    val bals by vm.balances.collectAsState()
    val names = bals.associate { it.customer.id to it.customer.name }
    Scaffold(topBar = { KTopBar("All Transactions", nav) }) { pad ->
        if (txns.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No transactions yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(txns, key = { it.id }) { t ->
                val rec = t.type == T.R
                val col = if (rec) Green else Red
                ElevatedCard(onClick = { nav.navigate("ledger/${t.customerId}") }, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(names[t.customerId] ?: "Customer", fontWeight = FontWeight.SemiBold)
                            if (t.note.isNotBlank()) Text(t.note, style = MaterialTheme.typography.bodyMedium)
                            Text("${niceDate(t.date)} · ${LocalTime.parse(t.time).format(timeFmt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(money(t.amount), color = col, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(if (rec) "Received" else "Given", color = col, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Theme picker / Settings / About
@Composable
fun ThemePicker(current: String, onSelect: (String) -> Unit) {
    Column {
        listOf("LIGHT" to "Light", "DARK" to "Dark", "SYSTEM" to "System Default").forEach { (v, label) ->
            Row(Modifier.fillMaxWidth().clickable { onSelect(v) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = current == v, onClick = { onSelect(v) })
                Text(label)
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: KhataVM, nav: NavHostController) {
    val theme by vm.theme.collectAsState()
    Scaffold(topBar = { KTopBar("App Settings", nav) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Theme", fontWeight = FontWeight.SemiBold)
                    ThemePicker(theme ?: "SYSTEM") { vm.setTheme(it) }
                }
            }
            Text("All data is stored only on this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AboutScreen(nav: NavHostController) {
    Scaffold(topBar = { KTopBar("About", nav) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Avatar("K", 88.dp)
            Text("Khata", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Version 1.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text("A simple, offline ledger for tracking money given and received. No account, no cloud — your data stays on your phone.", textAlign = TextAlign.Center)
        }
    }
}

// ---------------------------------------------------------------- Profile
@Composable
fun ProfileScreen(vm: KhataVM) {
    val ctx = LocalContext.current
    val profile by vm.profile.collectAsState()
    val theme by vm.theme.collectAsState()
    var name by remember(profile) { mutableStateOf(profile?.name ?: "") }
    var biz by remember(profile) { mutableStateOf(profile?.businessName ?: "") }
    var phone by remember(profile) { mutableStateOf(profile?.phone ?: "") }
    var addr by remember(profile) { mutableStateOf(profile?.address ?: "") }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Avatar(name.ifBlank { "K" }, 84.dp) }
        OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(biz, { biz = it }, label = { Text("Business Name") }, singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(phone, { phone = it }, label = { Text("Phone") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(addr, { addr = it }, label = { Text("Address") }, minLines = 2, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
        Button(
            onClick = { vm.saveProfile(Profile(1, name.trim(), biz.trim(), phone.trim(), addr.trim())); Toast.makeText(ctx, "Profile saved", Toast.LENGTH_SHORT).show() },
            shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(50.dp)
        ) { Text("Save Profile") }
        ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Theme", fontWeight = FontWeight.SemiBold)
                ThemePicker(theme ?: "SYSTEM") { vm.setTheme(it) }
            }
        }
    }
}
