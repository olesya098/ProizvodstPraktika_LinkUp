package com.hfad.linkup
data class Message(
    val id: String = "", // Добавляем уникальный идентификатор сообщения
    val text: String = "",
    val author: String = "",
    val timestamp: Long = System.currentTimeMillis()
)