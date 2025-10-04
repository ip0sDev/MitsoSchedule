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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material3.Text
import by.iposdev.watchso.presentation.theme.MitsoTestTheme

class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AboutScreen { finish() }
        }
    }
}

@Composable
fun AboutScreen(onBackClicked: () -> Unit) {
    MitsoTestTheme {
        Scaffold(
            timeText = { TimeText(modifier = Modifier.padding(top = 6.dp)) }
        ) {
            ScalingLazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top)
            ) {
                item {
                    Text(
                        text = "О приложении",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                item {
                    Card(onClick = {}) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Text("Разработчик:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("IposDev", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item {
                    Card(onClick = {}) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Text("Версия приложения:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("1.1.0", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item {
                    Card(onClick = {}) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Text("Тех. поддержка:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("TG @iposdev", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }
                item {
                    Chip(
                        onClick = onBackClicked,
                        label = { Text("Назад") },
                        icon = { Icon(Icons.Default.ArrowBack, contentDescription = "Назад", modifier = Modifier.size(ChipDefaults.IconSize)) },
                        colors = ChipDefaults.secondaryChipColors(),
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                }
            }
        }
    }
}
