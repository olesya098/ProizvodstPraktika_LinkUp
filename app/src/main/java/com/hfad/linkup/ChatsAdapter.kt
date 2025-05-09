package com.hfad.linkup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Chat(
    val name: String = "",
    val lastMessage: String = "",
    val time: String = "",
    val icon: Int = R.drawable.baseline_mark_unread_chat_alt_24,
    val inviteCode: String = "", // Добавляем код приглашения
    val adminId: String = "", // ID создателя чата
    val members: List<String> = emptyList() // Список ID участников
)

class ChatsAdapter(
    private val chats: List<Chat>,
    private val onItemClick: (Chat) -> Unit
) : RecyclerView.Adapter<ChatsAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chatIcon: ImageView = itemView.findViewById(R.id.chatIcon)
        private val chatName: TextView = itemView.findViewById(R.id.chatName)
        private val lastMessage: TextView = itemView.findViewById(R.id.lastMessage)
        private val chatTime: TextView = itemView.findViewById(R.id.chatTime)

        fun bind(chat: Chat) {
            chatIcon.setImageResource(chat.icon)
            chatName.text = chat.name
            lastMessage.text = chat.lastMessage
            chatTime.text = chat.time

            itemView.setOnClickListener { onItemClick(chat) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.chat_item, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chats[position])
    }

    override fun getItemCount() = chats.size
}