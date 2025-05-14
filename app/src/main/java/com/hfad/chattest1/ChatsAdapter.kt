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

// Класс данных, представляющий модель чата
@IgnoreExtraProperties  // Игнорирует лишние поля при десериализации из Firebase
data class Chat(
    @PropertyName("id") val id: String = "",               // Уникальный идентификатор чата
    @PropertyName("name") val name: String = "",           // Название чата
    @PropertyName("lastMessage") val lastMessage: String = "",  // Последнее сообщение в чате
    @PropertyName("time") val time: String = "",           // Время последнего сообщения
    @PropertyName("inviteCode") val inviteCode: String = "",  // Код для приглашения в чат
    @PropertyName("adminId") val adminId: String = "",     // ID администратора чата
    @PropertyName("members") val members: List<String> = emptyList(),  // Список участников
    @PropertyName("categoryId") val categoryId: String = ""  // ID категории чата
)

// Адаптер для отображения списка чатов в RecyclerView
class ChatsAdapter(
    private var chats: List<Chat>,  // Список чатов для отображения
    private val onItemClick: (Chat) -> Unit,         // Обработчик клика по элементу
    private val onDeleteClick: (Chat) -> Unit,       // Обработчик удаления чата
    private val onInviteClick: (Chat) -> Unit,       // Обработчик приглашения в чат
    private val onEditNameClick: (Chat) -> Unit,     // Обработчик редактирования названия
    private val onLeaveClick: (Chat) -> Unit,        // Обработчик выхода из чата
    private val onAddToBlacklistClick: (Chat) -> Unit  // Обработчик добавления в черный список
) : RecyclerView.Adapter<ChatsAdapter.ChatViewHolder>() {

    // ViewHolder для элементов списка чатов
    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chatName: TextView = itemView.findViewById(R.id.chatName)  // Название чата
        private val menuButton: ImageView = itemView.findViewById(R.id.menuButton)  // Кнопка меню
        private val leaveButton: ImageView = itemView.findViewById(R.id.leaveButton)  // Кнопка выхода
        private val blacklistButton: ImageView = itemView.findViewById(R.id.blacklistButton)  // Кнопка ЧС

        // Привязка данных чата к элементу списка
        fun bind(chat: Chat) {
            chatName.text = chat.name  // Установка названия чата

            val currentUser = FirebaseAuth.getInstance().currentUser  // Текущий пользователь
            val isAdmin = currentUser?.uid == chat.adminId  // Проверка, является ли админом

            // Настройка видимости кнопок в зависимости от роли пользователя
            menuButton.visibility = if (isAdmin) View.VISIBLE else View.GONE  // Меню только для админа
            leaveButton.visibility = if (!isAdmin) View.VISIBLE else View.GONE  // Выход только для участников
            blacklistButton.visibility = if (!isAdmin) View.VISIBLE else View.GONE  // ЧС только для участников

            // Обработчики нажатий на элементы
            itemView.setOnClickListener { onItemClick(chat) }  // Клик по всему элементу

            leaveButton.setOnClickListener {
                if (!isAdmin && chat.members.contains(currentUser?.uid)) {
                    onLeaveClick(chat)  // Обработчик выхода из чата
                }
            }

            blacklistButton.setOnClickListener {
                if (!isAdmin && chat.members.contains(currentUser?.uid)) {
                    onAddToBlacklistClick(chat)  // Обработчик добавления в ЧС
                }
            }

            menuButton.setOnClickListener { view ->
                if (isAdmin) {
                    showPopupMenu(view, chat)  // Показ меню для администратора
                }
            }
        }

        // Показ всплывающего меню с действиями для администратора
        private fun showPopupMenu(view: View, chat: Chat) {
            PopupMenu(view.context, view).apply {
                menuInflater.inflate(R.menu.chat_item_menu, menu)  // Загрузка меню из ресурсов

                // Настройка видимости пунктов меню
                menu.apply {
                    findItem(R.id.action_delete)?.isVisible = true          // Удаление чата
                    findItem(R.id.action_edit_name)?.isVisible = true      // Редактирование названия
                    findItem(R.id.action_invite)?.isVisible = true         // Приглашение
                    findItem(R.id.action_leave)?.isVisible = false          // Скрыть выход (для админа)
                    findItem(R.id.action_add_to_blacklist)?.isVisible = true  // Добавление в ЧС
                }

                // Обработчик выбора пункта меню
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
                show()  // Показ меню
            }
        }
    }

    // Создание нового ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.chat_item, parent, false)
        return ChatViewHolder(view)
    }

    // Привязка данных к существующему ViewHolder
    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chats[position])
    }

    // Получение количества элементов
    override fun getItemCount() = chats.size

    // Обновление списка чатов
    fun updateList(newList: List<Chat>) {
        chats = newList
        notifyDataSetChanged()  // Уведомление об изменении данных
    }
}