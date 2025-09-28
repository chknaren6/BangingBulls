package com.nc.bangingbulls.Home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nc.bangingbulls.Home.Stocks.StockFiles.VM.UserPortfolioViewModel
import com.nc.bangingbulls.Home.Stocks.StockFiles.M.addOrIncreaseHolding
import com.nc.bangingbulls.Home.Stocks.StockFiles.M.deleteHolding
import kotlinx.coroutines.launch

@Composable
fun HomePortfolioSection(uid: String) {
    val vm = remember { UserPortfolioViewModel() }
    LaunchedEffect(uid) { vm.start(uid) }

    val coins by vm.coins.collectAsState()
    val lines by vm.lines.collectAsState()
    val net by vm.netWorth.collectAsState()

    Text("Your Net Worth: ${"%.2f".format(net)}", fontWeight = FontWeight.Bold)
    Text("Coins: ${"%.2f".format(coins)}")

    Spacer(Modifier.height(8.dp))

    LazyColumn(modifier = Modifier.fillMaxWidth().height(240.dp)) {
        items(lines) { line ->
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("${line.name} (${line.symbol})", fontWeight = FontWeight.Bold)
                    Text("Qty: ${line.qty}  Avg: ${"%.2f".format(line.avgPrice)}  Now: ${"%.2f".format(line.currentPrice)}")
                    Text("Value: ${"%.2f".format(line.value)}")
                }
            }
        }
    }
}

@Composable
fun HoldingAdminControls(uid: String, stockId: String) {
    val scope = rememberCoroutineScope()
    var qtyText by remember { mutableStateOf("1") }

    Row {
        OutlinedTextField(
            value = qtyText,
            onValueChange = { qtyText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Qty") }
        )
        Spacer(Modifier.width(8.dp))
        Button(onClick = {
            val q = qtyText.toLongOrNull() ?: 0L
            scope.launch { addOrIncreaseHolding(uid, stockId, q, avgPrice = null) }
        }) { Text("Add/Inc") }
        Spacer(Modifier.width(8.dp))
        Button(onClick = {
            scope.launch { deleteHolding(uid, stockId) }
        }) { Text("Delete") }
    }
}

