package com.nc.bangingbulls.Home.Stocks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nc.bangingbulls.stocks.StocksViewModel
@Composable
fun AdminStockScreen(
    stocksViewModel: StocksViewModel,
    navController: NavController
) {
    var name by remember { mutableStateOf("") }
    var symbol by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var totalSupply by remember { mutableStateOf("") }
    var availableSupply by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        TextField(value = name, onValueChange = { name = it }, label = { Text("Stock Name") })
        TextField(value = symbol, onValueChange = { symbol = it }, label = { Text("Symbol") })
        TextField(value = price, onValueChange = { price = it }, label = { Text("Price") })
        TextField(value = totalSupply, onValueChange = { totalSupply = it }, label = { Text("Total Supply") })
        TextField(value = availableSupply, onValueChange = { availableSupply = it }, label = { Text("Available Supply") })

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            val stock = Stock(
                id = "",
                name = name,
                symbol = symbol,
                price = price.toDoubleOrNull() ?: 0.0,
                totalSupply = totalSupply.toLongOrNull() ?: 0L,
                availableSupply = availableSupply.toLongOrNull() ?: 0L
            )

            stocksViewModel.addStock(stock)
            navController.navigate("HomeScreen") {
                popUpTo("AdminStockScreen") { inclusive = true }
                launchSingleTop = true
            }
        }) {
            Text("Create Stock")
        }
    }
}

