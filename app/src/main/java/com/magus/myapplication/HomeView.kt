package com.magus.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeView(viewModel: HomeViewModel) {
    HomeScreen(
        canTranscribe = viewModel.canTranscribe,
        isLoading = viewModel.isLoading,
        messageLog = viewModel.dataLog,
        onTranscribeSampleTapped = viewModel::transcribeSample
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    canTranscribe: Boolean,
    isLoading: Boolean,
    messageLog: String,
    onTranscribeSampleTapped: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Whisper语音转录") }
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Whisper语音转录Demo",
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                TranscribeSampleButton(
                    enabled = canTranscribe && !isLoading,
                    isLoading = isLoading,
                    onClick = onTranscribeSampleTapped
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LogDisplay(messageLog)
            }
        }
    }
}

@Composable
private fun LogDisplay(log: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        SelectionContainer {
            Text(
                text = log,
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}

@Composable
private fun TranscribeSampleButton(enabled: Boolean, isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = if (isLoading) "正在转录..." else "转录示例音频",
            fontSize = 16.sp
        )
    }
}
