package com.fsscrm.ui.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fsscrm.network.RetrofitClient
import com.fsscrm.ui.common.UniversalHeader
import com.fsscrm.ui.theme.PrimaryIndigo
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAgentScreen(userId: Int, onBack: () -> Unit) {
    var inputText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Welcome message
    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            messages.add(ChatMessage("Hello! I am your FSS CRM AI Assistant. How can I help you today?", false))
        }
    }

    // Auto-scroll to bottom
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val onSend = {
        val query = inputText.trim()
        if (query.isNotEmpty() && !isLoading) {
            messages.add(ChatMessage(query, true))
            inputText = ""
            isLoading = true
            scope.launch {
                try {
                    val response = RetrofitClient.apiService.askAi(mapOf(
                        "user_id" to userId.toString(),
                        "query" to query
                    ))
                    if (response.isSuccessful) {
                        val body = response.body()
                        val aiReply = body?.asJsonObject?.get("answer")?.asString 
                            ?: body?.asJsonObject?.get("message")?.asString
                            ?: "I'm sorry, I couldn't process that request."
                        messages.add(ChatMessage(aiReply, false))
                    } else {
                        messages.add(ChatMessage("Error: Could not connect to AI service.", false))
                    }
                } catch (e: Exception) {
                    messages.add(ChatMessage("Network Error: ${e.message}", false))
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            UniversalHeader(
                title = "AI Assistant",
                onBackClick = onBack,
                actions = {
                    Icon(
                          Icons.Default.AutoAwesome,
                        null,
                        tint = Color.White,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8FAFC))
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                messages.forEach { msg ->
                    item {
                        ChatBubble(msg)
                    }
                }
                if (isLoading) {
                    item {
                        TypingIndicator()
                    }
                }
            }

            // Input Area
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { newValue: String -> inputText = newValue },
                        placeholder = { Text("Ask me anything...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            onSend()
                            keyboardController?.hide()
                        })
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { onSend() },
                        enabled = !isLoading && inputText.isNotBlank(),
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank()) PrimaryIndigo else Color.LightGray)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (message.isUser) PrimaryIndigo else Color.White
    val textColor = if (message.isUser) Color.White else Color.Black
    val shape = if (message.isUser) {
        RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!message.isUser) {
                Icon(
                    Icons.Default.SmartToy,
                    null,
                    tint = PrimaryIndigo,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(bottom = 4.dp, end = 4.dp)
                )
            }
            Surface(
                color = bgColor,
                shape = shape,
                shadowElevation = 1.dp,
                border = if (!message.isUser) BorderStroke(1.dp, Color(0xFFE2E8F0)) else null
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = textColor,
                    fontSize = 14.sp
                )
            }
            if (message.isUser) {
                Icon(
                    Icons.Default.Person,
                    null,
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(bottom = 4.dp, start = 4.dp)
                )
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier.padding(start = 28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("AI is thinking...", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(8.dp))
        LinearProgressIndicator(
            modifier = Modifier.width(40.dp),
            color = PrimaryIndigo,
            trackColor = PrimaryIndigo.copy(alpha = 0.2f)
        )
    }
}
