package by.iposdev.watchso.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import by.iposdev.watchso.presentation.theme.MitsoTestTheme

class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Передаем лямбду, которая закрывает текущую Activity
            AboutScreen { finish() }
        }
    }
}

@Composable
fun AboutScreen(onBackClicked: () -> Unit) { // onBackClicked теперь будет вызывать finish()
    MitsoTestTheme {
        Scaffold(
            timeText = { TimeText(modifier = Modifier.padding(top = 6.dp)) }
        ) {
            ScalingLazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
            ) {
                item {
                    Text(
                        text = "О приложении",
                        style = MaterialTheme.typography.title2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                item {
                    Card(onClick = {}) {
                        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                            Text("Разработчик:", style = MaterialTheme.typography.caption1)
                            Text("IposDev", style = MaterialTheme.typography.body1, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item {
                    Card(onClick = {}) {
                        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                            Text("Версия приложения:", style = MaterialTheme.typography.caption1)
                            Text("0.0.1 ALPHA 1", style = MaterialTheme.typography.body1, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item {
                    Card(onClick = {}) {
                        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                            Text("Тех. поддержка:", style = MaterialTheme.typography.caption1)
                            Text("TG @iposdev", style = MaterialTheme.typography.body1, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }
                item {
                    Chip(
                        onClick = onBackClicked, // Это вызовет finish()
                        label = { Text("Назад") },
                        colors = ChipDefaults.primaryChipColors(),
                        modifier = Modifier.fillMaxWidth(0.7f)
                    )
                }
            }
        }
    }
}
