package com.nc.bangingbulls.Home.Stocks.StockFiles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nc.bangingbulls.Home.Stocks.StockFiles.StocksViewModel

@Composable
fun StocksScreen(navController: NavController, stocksViewModel: StocksViewModel) {
    val stocks by stocksViewModel.stocks.collectAsState()
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        items(stocks) { stock ->
            var myQty by remember(stock.id) { mutableStateOf(0L) }
            LaunchedEffect(stock.id, uid) {
                if (uid != null) {
                    FirebaseFirestore.getInstance()
                        .collection("users").document(uid)
                        .collection("holdings").document(stock.id)
                        .addSnapshotListener { d, _ ->
                            myQty = d?.getLong("qty") ?: 0L
                        }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    .clickable { navController.navigate("stock/${stock.id}") }
            ) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(stock.name, fontWeight = FontWeight.Bold)
                        Text(stock.symbol)
                        Text("Price: ${"%,.2f".format(stock.price)}")
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Holding: $myQty")
                        Text("Supply: ${stock.availableSupply}/${stock.totalSupply}")
                    }
                }
            }
        }
    }
}

