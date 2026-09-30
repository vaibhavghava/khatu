@file:OptIn(ExperimentalMaterial3Api::class)

package com.khata.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object T { const val R = "RECEIVED"; const val G = "GIVEN" }

enum class Status(val label: String, val color: Color) {
    RECEIVABLE("Receivable", Blue), PAYABLE("Payable", Amber), SETTLED("Settled", Color(0xFF6B7A7A))
}

fun r2(d: Double): Double = Math.round(d * 100) / 100.0

/** net = TOTAL GIVEN - TOTAL RECEIVED. >0 receivable, <0 payable, 0 settled. */
fun statusOf(net: Double): Status = if (net > 0) Status.RECEIVABLE else if (net < 0) Status.PAYABLE else Status.SETTLED

private val nf: NumberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN")).apply {
    minimumFractionDigits = 0; maximumFractionDigits = 2
}
fun money(d: Double): String = "₹" + nf.format(d)
fun signed(d: Double): String = if (d < 0) "-" + money(-d) else money(d)
fun plain(d: Double): String = if (d == Math.floor(d)) d.toLong().toString() else d.toString()

val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)

fun niceDate(s: String): String {
    val d = LocalDate.parse(s)
    val today = LocalDate.now()
    return when (d) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> d.format(dateFmt)
    }
}

fun buildMessage(name: String, net: Double): String = when (statusOf(net)) {
    Status.PAYABLE -> "I have to give ${money(-net)} to you."
    Status.RECEIVABLE -> "You (${name.trim()}) have to pay me ${money(net)}."
    Status.SETTLED -> "Your account is settled. No payment is currently due."
}

fun sendSms(ctx: Context, c: Customer, net: Double) {
    val phone = c.phone.filter { it.isDigit() || it == '+' }
    if (phone.isBlank()) {
        Toast.makeText(ctx, "Customer phone number is not available.", Toast.LENGTH_SHORT).show()
        return
    }
    val i = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).putExtra("sms_body", buildMessage(c.name, net))
    try { ctx.startActivity(i) } catch (e: Exception) {
        Toast.makeText(ctx, "No messaging app found.", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun Avatar(name: String, size: Dp = 44.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(name.trim().take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.4f).sp)
    }
}

@Composable
fun StatusChip(s: Status) {
    Text(
        s.label, color = s.color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(s.color.copy(alpha = 0.14f)).padding(horizontal = 9.dp, vertical = 3.dp)
    )
}

@Composable
fun KTopBar(title: String, nav: NavHostController) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } }
    )
}
