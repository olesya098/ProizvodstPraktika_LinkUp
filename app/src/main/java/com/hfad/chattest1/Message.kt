package com.hfad.chattest1
// Класс данных для представления сообщения в чате
data class Message(
    val text: String = "",
    val author: String = "",
    val authorEmail: String = "",
    val timestamp: Long = 0,
    val imageData: String = "",
    val chatId: String = ""
)