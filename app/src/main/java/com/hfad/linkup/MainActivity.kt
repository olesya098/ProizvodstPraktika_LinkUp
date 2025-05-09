package com.hfad.linkup

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.hfad.linkup.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var database: DatabaseReference
    private val auth = Firebase.auth
    private lateinit var chatName: String
    private val messagesList = mutableListOf<Pair<String, Message>>() // Список для хранения id и сообщений

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chatName = intent.getStringExtra("CHAT_NAME") ?: "Общий чат"
        database = Firebase.database.reference.child("messages").child(chatName.replace(" ", "_"))

        setupNavigationButtons()

        binding.bSend.setOnClickListener {
            val messageText = binding.edMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                binding.edMessage.setText("")
            }
        }

        listenForMessages()
    }

    private fun sendMessage(messageText: String) {
        val user = auth.currentUser
        if (user != null) {
            val message = Message(
                text = messageText,
                author = user.displayName ?: "Anonymous",
                timestamp = System.currentTimeMillis()
            )
            database.push().setValue(message)
        }
    }

    private fun listenForMessages() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messagesList.clear()
                binding.rcView.text = ""
                for (messageSnapshot in snapshot.children) {
                    val messageId = messageSnapshot.key ?: continue
                    val message = messageSnapshot.getValue(Message::class.java)
                    message?.let {
                        val timeString = formatTimestamp(it.timestamp)
                        val displayText = "\n${it.author} ($timeString):\n${it.text}\n\n"
                        binding.rcView.append(displayText)

                        // Добавляем кнопку удаления для каждого сообщения
                        binding.rcView.append("[Del]"+"\n")
                        binding.rcView.setOnClickListener { view ->
                            showDeleteConfirmation(messageId, message)
                        }

                        messagesList.add(Pair(messageId, message))
                    }
                }

                // Scroll to bottom
                binding.rcView.post {
                    (binding.rcView.parent as? ScrollView)?.fullScroll(ScrollView.FOCUS_DOWN)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Обработка ошибки
            }
        })
    }

    private fun showDeleteConfirmation(messageId: String, message: Message) {
        AlertDialog.Builder(this)
            .setTitle("Удаление сообщения")
            .setMessage("Вы уверены, что хотите удалить это сообщение?")
            .setPositiveButton("Да") { _, _ ->
                deleteMessage(messageId)
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    private fun deleteMessage(messageId: String) {
        val currentUser = auth.currentUser

        // Проверяем, что текущий пользователь - автор сообщения
        database.child(messageId).get().addOnSuccessListener { snapshot ->
            val message = snapshot.getValue(Message::class.java)
            if (message?.author == currentUser?.displayName) {
                // Удаляем сообщение из базы данных
                database.child(messageId).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Сообщение удалено", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Не удалось удалить сообщение", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Вы можете удалять только свои сообщения", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Остальные методы остаются без изменений

    private fun setupNavigationButtons() {
        // Находим кнопки навигации
        val bProfile = findViewById<ImageButton>(R.id.bProfile)
        val bChats = findViewById<ImageButton>(R.id.bChats)
        val bSettings = findViewById<ImageButton>(R.id.bSettings)

        // Обработчик для кнопки профиля
        bProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Обработчик для кнопки чатов (текущая страница)
        bChats.setOnClickListener {
            // Ничего не делаем, так как уже находимся на странице чатов
        }

        // Обработчик для кнопки настроек
        bSettings.setOnClickListener {
            // Здесь можно добавить переход на страницу настроек
            // startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Установка выделения для текущей страницы
        bChats.isSelected = true
    }




    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}