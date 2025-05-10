package com.hfad.chattest1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

@IgnoreExtraProperties
data class Chat(
    @PropertyName("id") val id: String = "",
    @PropertyName("name") val name: String = "",
    @PropertyName("lastMessage") val lastMessage: String = "",
    @PropertyName("time") val time: String = "",
    @PropertyName("inviteCode") val inviteCode: String = "",
    @PropertyName("adminId") val adminId: String = "",
    @PropertyName("members") val members: List<String> = emptyList(),
    @PropertyName("categoryId") val categoryId: String = ""
)

class ChatsAdapter(
    private var chats: List<Chat>,
    private val onItemClick: (Chat) -> Unit,
    private val onDeleteClick: (Chat) -> Unit,
    private val onInviteClick: (Chat) -> Unit,
    private val onEditNameClick: (Chat) -> Unit,
    private val onLeaveClick: (Chat) -> Unit,
    private val onAddToBlacklistClick: (Chat) -> Unit
) : RecyclerView.Adapter<ChatsAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chatName: TextView = itemView.findViewById(R.id.chatName)
        private val menuButton: ImageView = itemView.findViewById(R.id.menuButton)
        private val leaveButton: ImageView = itemView.findViewById(R.id.leaveButton)
        private val blacklistButton: ImageView = itemView.findViewById(R.id.blacklistButton)

        fun bind(chat: Chat) {
            chatName.text = chat.name

            val currentUser = FirebaseAuth.getInstance().currentUser
            val isAdmin = currentUser?.uid == chat.adminId

            // Настройка видимости кнопок
            menuButton.visibility = if (isAdmin) View.VISIBLE else View.GONE
            leaveButton.visibility = if (!isAdmin) View.VISIBLE else View.GONE
            blacklistButton.visibility = if (!isAdmin) View.VISIBLE else View.GONE

            // Обработчики нажатий
            itemView.setOnClickListener { onItemClick(chat) }
            leaveButton.setOnClickListener {
                if (!isAdmin && chat.members.contains(currentUser?.uid)) {
                    onLeaveClick(chat)
                }
            }
            blacklistButton.setOnClickListener {
                if (!isAdmin && chat.members.contains(currentUser?.uid)) {
                    onAddToBlacklistClick(chat)
                }
            }
            menuButton.setOnClickListener { view ->
                if (isAdmin) {
                    showPopupMenu(view, chat)
                }
            }
        }

        private fun showPopupMenu(view: View, chat: Chat) {
            PopupMenu(view.context, view).apply {
                menuInflater.inflate(R.menu.chat_item_menu, menu)

                menu.apply {
                    findItem(R.id.action_delete)?.isVisible = true
                    findItem(R.id.action_edit_name)?.isVisible = true
                    findItem(R.id.action_invite)?.isVisible = true
                    findItem(R.id.action_leave)?.isVisible = false
                    findItem(R.id.action_add_to_blacklist)?.isVisible = true
                }

                setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_delete -> {
                            onDeleteClick(chat)
                            true
                        }
                        R.id.action_invite -> {
                            onInviteClick(chat)
                            true
                        }
                        R.id.action_edit_name -> {
                            onEditNameClick(chat)
                            true
                        }
                        R.id.action_add_to_blacklist -> {
                            onAddToBlacklistClick(chat)
                            true
                        }
                        else -> false
                    }
                }
                show()
            }
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

    fun updateList(newList: List<Chat>) {
        chats = newList
        notifyDataSetChanged()
    }
}