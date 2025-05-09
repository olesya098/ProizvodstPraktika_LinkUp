package com.hfad.linkup

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Chats : AppCompatActivity() {
    private lateinit var chatsRecyclerView: RecyclerView
    private lateinit var chatsAdapter: ChatsAdapter
    private val chatsList = mutableListOf<Chat>()
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chats)

        // Инициализация Firebase Database и Authentication
        database = Firebase.database.reference.child("chats")
        auth = FirebaseAuth.getInstance()

        // Находим RecyclerView
        chatsRecyclerView = findViewById(R.id.chatsRecyclerView)
        chatsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Создаем и устанавливаем адаптер
        chatsAdapter = ChatsAdapter(chatsList) { chat ->
            // Обработка клика по чату
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("CHAT_NAME", chat.name)
            startActivity(intent)
        }
        chatsRecyclerView.adapter = chatsAdapter

        // Добавляем кнопки создания и присоединения к чату
        val bCreateChat: ImageButton = findViewById(R.id.bCreateChat)
        val bJoinChat: ImageButton = findViewById(R.id.bJoinChat)

        bCreateChat.setOnClickListener {
            showCreateChatDialog()
        }

        bJoinChat.setOnClickListener {
            showJoinChatDialog()
        }

        setupNavigationButtons()
        loadChats()
    }

    private fun showCreateChatDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_chat, null)
        val editTextChatName = dialogView.findViewById<EditText>(R.id.editTextChatName)

        AlertDialog.Builder(this)
            .setTitle("Создать новый чат")
            .setView(dialogView)
            .setPositiveButton("Создать") { _, _ ->
                val chatName = editTextChatName.text.toString().trim()
                if (chatName.isNotEmpty()) {
                    createNewChat(chatName)
                } else {
                    Toast.makeText(this, "Введите название чата", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun createNewChat(chatName: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Генерируем уникальный код приглашения
            val inviteCode = generateInviteCode()

            val newChatRef = database.push()
            val newChat = Chat(
                name = chatName,
                lastMessage = "Новый чат",
                time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                inviteCode = inviteCode,
                adminId = currentUser.uid,
                members = listOf(currentUser.uid)
            )

            newChatRef.setValue(newChat)
                .addOnSuccessListener {
                    showInviteCodeDialog(inviteCode)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Ошибка создания чата", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun generateInviteCode(): String {
        return (100000..999999).random().toString()
    }

    private fun showInviteCodeDialog(inviteCode: String) {
        AlertDialog.Builder(this)
            .setTitle("Код приглашения")
            .setMessage("Код для приглашения в чат: $inviteCode\n\nПоделитесь этим кодом с друзьями.")
            .setPositiveButton("Поделиться") { _, _ ->
                shareInviteCode(inviteCode)
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun shareInviteCode(inviteCode: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Присоединяйся к чату! Код приглашения: $inviteCode")
        }
        startActivity(Intent.createChooser(shareIntent, "Поделиться кодом"))
    }

    private fun showJoinChatDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_join_chat, null)
        val editTextInviteCode = dialogView.findViewById<EditText>(R.id.editTextInviteCode)

        // Дополнительные настройки для EditText
        editTextInviteCode.filters = arrayOf(
            InputFilter.LengthFilter(6),
            InputFilter { source, _, _, dest, _, _ ->
                // Разрешаем только цифры
                val regex = Regex("[0-9]*")
                if (source.toString().matches(regex)) source else ""
            }
        )

        AlertDialog.Builder(this)
            .setTitle("Войти в чат")
            .setView(dialogView)
            .setPositiveButton("Войти") { _, _ ->
                val inviteCode = editTextInviteCode.text.toString().trim()
                if (inviteCode.isNotEmpty() && inviteCode.length == 6) {
                    joinChatByInviteCode(inviteCode)
                } else {
                    Toast.makeText(this, "Введите корректный код приглашения (6 цифр)", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun joinChatByInviteCode(inviteCode: String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            database.orderByChild("inviteCode").equalTo(inviteCode)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            for (chatSnapshot in snapshot.children) {
                                val chat = chatSnapshot.getValue(Chat::class.java)
                                chat?.let {
                                    // Проверяем, что пользователь еще не в чате
                                    if (!it.members.contains(currentUser.uid)) {
                                        val updatedMembers = it.members.toMutableList().apply {
                                            add(currentUser.uid)
                                        }
                                        chatSnapshot.ref.child("members").setValue(updatedMembers)
                                            .addOnSuccessListener {
                                                Toast.makeText(this@Chats,
                                                    "Вы успешно присоединились к чату ${chat.name}",
                                                    Toast.LENGTH_SHORT).show()

                                                // Переход в чат после присоединения
                                                val intent = Intent(this@Chats, MainActivity::class.java)
                                                intent.putExtra("CHAT_NAME", chat.name)
                                                startActivity(intent)
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(this@Chats,
                                                    "Не удалось присоединиться к чату",
                                                    Toast.LENGTH_SHORT).show()
                                            }
                                    } else {
                                        Toast.makeText(this@Chats,
                                            "Вы уже состоите в этом чате",
                                            Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(this@Chats,
                                "Чат с таким кодом не найден",
                                Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@Chats,
                            "Ошибка: ${error.message}",
                            Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun loadChats() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            database.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    chatsList.clear()
                    for (childSnapshot in snapshot.children) {
                        val chat = childSnapshot.getValue(Chat::class.java)
                        // Показываем только чаты, где пользователь является членом
                        chat?.let {
                            if (it.members.contains(currentUser.uid)) {
                                chatsList.add(it)
                            }
                        }
                    }
                    chatsAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@Chats, "Ошибка загрузки чатов", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun setupNavigationButtons() {
        val bProfile = findViewById<ImageButton>(R.id.bProfile)
        val bChats = findViewById<ImageButton>(R.id.bChats)
        val bSettings = findViewById<ImageButton>(R.id.bSettings)

        bProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        bChats.setOnClickListener {
            // Текущая страница
        }

        bSettings.setOnClickListener {
            // Здесь позже можно реализовать переход на страницу настроек
        }

        bChats.isSelected = true
    }
}