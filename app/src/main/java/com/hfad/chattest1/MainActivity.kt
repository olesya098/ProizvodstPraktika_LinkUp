package com.hfad.chattest1

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.transition.Transition
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat.setBackground
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.hfad.chattest1.databinding.ActivityMainBinding
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
data class ChatBackground(
    val imageUrl: String = "",
    val timestamp: Long = 0
)
//Основная активность чата, отвечающая за отображение и управление сообщениями.
//Включает функционал отправки, редактирования, удаления сообщений,
//управления фоном чата и списком участников.
class MainActivity : AppCompatActivity() {
    // Привязка к View элементам
    private lateinit var binding: ActivityMainBinding

    // Firebase компоненты
    private lateinit var database: DatabaseReference
    private lateinit var participantsRef: DatabaseReference
    private lateinit var backgroundRef: DatabaseReference
    private val auth = Firebase.auth

    // Данные чата
    private lateinit var chatName: String
    private val messagesList = mutableListOf<Pair<String, Message>>()
    private val participants = mutableListOf<String>()
    private var isAdmin = false

    // Утилитные функции для конвертации dp в px
    private fun Float.dpToPx(): Float = this * resources.displayMetrics.density
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    //Инициализация активности
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        // Настройка статус бара
        window.statusBarColor = Color.parseColor("#5B9693")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        setContentView(binding.root)

        // Получение имени чата из интента
        chatName = intent.getStringExtra("CHAT_NAME") ?: return finish()

        // Инициализация компонентов
        setupUI()
        initializeDatabase()
        setupListeners()
        addCurrentUserToParticipants()
        checkAdminStatus()
        listenForBackgroundChanges()
    }
    //Настройка пользовательского интерфейса
    private fun setupUI() {
        setupToolbar()
        setupParticipantsButton()
        setupBackgroundButton()
    }

    //Настройка верхней панели (тулбара)
    private fun setupToolbar() {
        // Настройка кнопки "назад"
        findViewById<ImageButton>(R.id.backButton)?.setOnClickListener {
            finish()
        }
        // Установка названия чата
        findViewById<TextView>(R.id.chatTitleText)?.text = chatName
    }

    //Настройка кнопки просмотра списка участников
    private fun setupParticipantsButton() {
        findViewById<ImageButton>(R.id.participantsButton)?.setOnClickListener {
            showParticipantsList()
        }
    }

    //Настройка кнопки изменения фона чата
    private fun setupBackgroundButton() {
        val backgroundButton = ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(48.dpToPx(), 48.dpToPx())
            setImageResource(R.drawable.wallpaper_24)
            background = AppCompatResources.getDrawable(context, R.drawable.button_bac)
            this.setBackgroundColor(ContextCompat.getColor(context, R.color.bar))
            contentDescription = "Изменить фон"
            setOnClickListener {
                showBackgroundOptionsDialog()
            }
        }

        findViewById<LinearLayout>(R.id.toolbarLayout)?.addView(backgroundButton)
    }

    //Инициализация Firebase компонентов и слушателей
    private fun initializeDatabase() {
        val sanitizedChatName = chatName.replace(" ", "_")

        // Инициализация ссылок на различные узлы базы данных
        database = Firebase.database.reference
            .child("messages")
            .child(sanitizedChatName)

        participantsRef = Firebase.database.reference
            .child("chats")
            .child(sanitizedChatName)
            .child("participants")

        backgroundRef = Firebase.database.reference
            .child("chats")
            .child(sanitizedChatName)
            .child("background")

        // Загрузка данных
        loadChatTitle()
        listenForMessages()
        listenForParticipants()
    }

    // Показывает диалог с опциями изменения фона
    private fun showBackgroundOptionsDialog() {
        val options = arrayOf("Установить фон", "Удалить фон")
        AlertDialog.Builder(this)
            .setTitle("Фон чата")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showBackgroundInputDialog()
                    1 -> removeBackground()
                }
            }
            .show()
    }
    //Показывает диалог для ввода URL изображения фона
    private fun showBackgroundInputDialog() {
        val editText = EditText(this).apply {
            hint = "Введите URL изображения"
            setPadding(32, 16, 32, 16)
        }

        AlertDialog.Builder(this)
            .setTitle("Установить фон чата")
            .setView(editText)
            .setPositiveButton("Установить") { _, _ ->
                val imageUrl = editText.text.toString().trim()
                if (imageUrl.isNotEmpty()) {
                    setBackground(imageUrl)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    //Устанавливает новый фон чата
    private fun setBackground(imageUrl: String) {
        if (!isFinishing && !isDestroyed) {
            val background = ChatBackground(imageUrl, System.currentTimeMillis())
            backgroundRef.setValue(background)
                .addOnSuccessListener {
                    if (!isFinishing && !isDestroyed) {
                        showToast("Фон чата обновлен")
                    }
                }
                .addOnFailureListener {
                    if (!isFinishing && !isDestroyed) {
                        showToast("Ошибка обновления фона")
                    }
                }
        }
    }

    //Удаляет текущий фон чата
    private fun removeBackground() {
        if (!isFinishing && !isDestroyed) {
            backgroundRef.removeValue()
                .addOnSuccessListener {
                    if (!isFinishing && !isDestroyed) {
                        showToast("Фон чата удален")
                    }
                }
                .addOnFailureListener {
                    if (!isFinishing && !isDestroyed) {
                        showToast("Ошибка удаления фона")
                    }
                }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        // Очищаем все слушатели
        backgroundRef.removeEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    //Слушатель изменений фона чата
    private fun listenForBackgroundChanges() {
        val backgroundListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isFinishing && !isDestroyed) {
                    val background = snapshot.getValue(ChatBackground::class.java)
                    updateBackgroundImage(background?.imageUrl)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isFinishing && !isDestroyed) {
                    showToast("Ошибка загрузки фона: ${error.message}")
                }
            }
        }

        backgroundRef.addValueEventListener(backgroundListener)
    }

    //Обновляет фоновое изображение чата
    private fun updateBackgroundImage(imageUrl: String?) {
        val scrollView = binding.rcView.parent as? ScrollView
        if (imageUrl.isNullOrEmpty()) {
            scrollView?.setBackgroundColor(Color.TRANSPARENT)
            return
        }

        try {
            Glide.with(this)
                .load(imageUrl)
                .error(android.R.color.transparent)
                .into(object : SimpleTarget<Drawable>() {
                    override fun onResourceReady(
                        resource: Drawable,
                        transition: com.bumptech.glide.request.transition.Transition<in Drawable>?
                    ) {
                        try {
                            if (!isFinishing && !isDestroyed) {
                                scrollView?.background = resource
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            scrollView?.setBackgroundColor(Color.TRANSPARENT)
                        }
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
            scrollView?.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    //Настройка слушателей событий
    private fun setupListeners() {
        // Слушатель кнопки отправки сообщения
        binding.bSend.setOnClickListener {
            val messageText = binding.edMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                binding.edMessage.setText("")

            }
        }
        // В функции setupListeners() добавьте:
        binding.bSendImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            selectImageLauncher.launch(intent)
        }

    }
    //Загружает название чата из базы данных
    private fun loadChatTitle() {
        Firebase.database.reference.child("chats")
            .orderByChild("name")
            .equalTo(chatName)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.firstOrNull()?.getValue(Chat::class.java)?.let { chat ->
                        findViewById<TextView>(R.id.chatTitleText)?.text = chat.name
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("Ошибка загрузки названия чата: ${error.message}")
                }
            })
    }
    //Добавляет текущего пользователя в список участников
    private fun addCurrentUserToParticipants() {
        auth.currentUser?.let { user ->
            val userEmail = user.email ?: return
            participantsRef.child(userEmail.replace(".", "_"))
                .setValue(true)
        }
    }
    //Слушатель изменений в списке участников
    private fun listenForParticipants() {
        participantsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                participants.clear()
                snapshot.children.forEach { participantSnapshot ->
                    val email = participantSnapshot.key?.replace("_", ".")
                    email?.let { participants.add(it) }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Ошибка загрузки участников: ${error.message}")
            }
        })
    }
    // Показывает диалог со списком участников чата
    private fun showParticipantsList() {
        if (participants.isEmpty()) {
            showToast("Список участников пуст")
            return
        }

        val participantsArray = participants.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Участники чата")
            .setItems(participantsArray, null)
            .setPositiveButton("OK", null)
            .show()
    }

    //Отправляет новое сообщение в чат
    private fun sendMessage(messageText: String) {
        auth.currentUser?.let { user ->
            val message = Message(
                text = messageText,
                author = user.displayName ?: "Anonymous",
                timestamp = System.currentTimeMillis()
            )
            database.push().setValue(message)
                .addOnFailureListener { e ->
                    showToast("Ошибка отправки сообщения: ${e.message}")
                }

            user.email?.let { email ->
                participantsRef.child(email.replace(".", "_"))
                    .setValue(true)
            }
        } ?: showToast("Необходимо авторизоваться")
    }

    //Слушатель новых сообщений
    private fun listenForMessages() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messagesList.clear()
                snapshot.children.forEach { messageSnapshot ->
                    val messageId = messageSnapshot.key ?: return@forEach
                    messageSnapshot.getValue(Message::class.java)?.let { message ->
                        messagesList.add(Pair(messageId, message))
                    }
                }
                displayMessages()
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Ошибка загрузки сообщений: ${error.message}")
            }
        })
    }
    private fun displayMessages() {
        binding.rcView.removeAllViews()
        val maxMessageWidth = (resources.displayMetrics.widthPixels * 0.7).toInt()

        messagesList.forEach { (messageId, message) ->
            val isCurrentUser = message.author == auth.currentUser?.displayName
            val timeString = formatTimestamp(message.timestamp)

            // Создание контейнера для сообщения
            val messageContainer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(8.dpToPx(), 4.dpToPx(), 8.dpToPx(), 4.dpToPx())
                }
            }

            // Добавляем кнопку редактирования слева
            if (isCurrentUser) {
                val editButton = ImageView(this).apply {
                    setImageResource(R.drawable.edit_note_24)
                    layoutParams = LinearLayout.LayoutParams(24.dpToPx(), 24.dpToPx()).apply {
                        gravity = Gravity.BOTTOM
                        marginEnd = 8.dpToPx()
                    }
                    setOnClickListener { view ->
                        showMessageOptions(view, messageId, message)
                    }
                }
                messageContainer.addView(editButton)
            }

            val messageContentContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    weight = 1f
                    gravity = if (isCurrentUser) Gravity.END else Gravity.START
                }
            }

            val messageContent = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = createMessageBackground(isCurrentUser)
                setPadding(12.dpToPx(), 8.dpToPx(), 12.dpToPx(), 8.dpToPx())
            }

            // Добавляем имя автора
            val authorText = TextView(this).apply {
                text = message.author
                setTextColor(if (isCurrentUser) Color.BLACK else Color.WHITE)
                textSize = 14f
            }
            messageContent.addView(authorText)

            // Если есть изображение, добавляем его
            if (message.imageData.isNotEmpty()) {
                val imageView = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        maxMessageWidth,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        adjustViewBounds = true
                    }
                }
                convertBase64ToBitmap(message.imageData)?.let { bitmap ->
                    imageView.setImageBitmap(bitmap)
                }
                messageContent.addView(imageView)
            }

            // Если есть текст, добавляем его
            if (message.text.isNotEmpty()) {
                val messageText = TextView(this).apply {
                    text = message.text
                    setTextColor(if (isCurrentUser) Color.BLACK else Color.WHITE)
                    textSize = 16f
                    maxWidth = maxMessageWidth
                }
                messageContent.addView(messageText)
            }

            // Добавляем время
            val timeText = TextView(this).apply {
                text = timeString
                setTextColor(if (isCurrentUser) Color.BLACK else Color.WHITE)
                textSize = 12f
                gravity = Gravity.END
            }
            messageContent.addView(timeText)

            messageContentContainer.addView(messageContent)
            messageContainer.addView(messageContentContainer)

            if (!isCurrentUser) {
                messageContainer.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                })
            }

            binding.rcView.addView(messageContainer)
        }

        scrollToBottom()
    }


    //Создает фон для сообщения с закругленными углами
    private fun createMessageBackground(isUser: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16f.dpToPx()

            if (isUser) {
                // Светло-серый цвет для сообщений пользователя
                setColor(Color.parseColor("#d6d6d6"))

                cornerRadii = floatArrayOf(
                    16f.dpToPx(), 16f.dpToPx(),   // Верхний левый
                    16f.dpToPx(), 16f.dpToPx(),   // Верхний правый
                    16f.dpToPx(), 16f.dpToPx(),   // Нижний правый
                    0f, 0f                        // Нижний левый (острый угол)
                )

            } else {
                // Более насыщенный оттенок бирюзового для сообщений собеседника
                setColor(Color.parseColor("#82ADAB"))

                cornerRadii = floatArrayOf(
                    16f.dpToPx(), 16f.dpToPx(),   // Верхний левый
                    16f.dpToPx(), 16f.dpToPx(),   // Верхний правый
                    0f, 0f,                       // Нижний правый (острый угол)
                    16f.dpToPx(), 16f.dpToPx()    // Нижний левый
                )
            }
        }
    }

    private fun showMessageOptions(view: View, messageId: String, message: Message) {
        // Проверяем, является ли текущий пользователь автором сообщения
        if (message.author != auth.currentUser?.displayName) return

        // Создаем и настраиваем всплывающее меню
        PopupMenu(this, view).apply {
            menu.apply {
                add(Menu.NONE, 1, Menu.NONE, "Редактировать")
                add(Menu.NONE, 2, Menu.NONE, "Копировать")
                add(Menu.NONE, 3, Menu.NONE, "Удалить")
            }
            // Обработчик выбора пункта меню
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> showEditMessageDialog(messageId, message)
                    2 -> copyMessageToClipboard(message.text)
                    3 -> showDeleteConfirmation(messageId)
                }
                true
            }
            show()
        }
    }

    //Показывает диалог редактирования сообщения
    private fun showEditMessageDialog(messageId: String, message: Message) {
        // Создаем поле ввода с текстом текущего сообщения
        val editText = EditText(this).apply {
            setText(message.text)
            setSelection(text.length)// Устанавливаем курсор в конец текста
        }

        // Создаем и показываем диалог редактирования
        AlertDialog.Builder(this)
            .setTitle("Редактировать сообщение")
            .setView(editText)
            .setPositiveButton("Сохранить") { _, _ ->
                val newText = editText.text.toString().trim()
                if (newText.isNotEmpty()) {
                    updateMessage(messageId, newText)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    //Обновляет текст сообщения в базе данных
    private fun updateMessage(messageId: String, newText: String) {
        database.child(messageId).child("text").setValue(newText)
            .addOnSuccessListener { showToast("Сообщение обновлено") }
            .addOnFailureListener { showToast("Ошибка обновления сообщения") }
    }

    //Копирует текст сообщения в буфер обмена
    private fun copyMessageToClipboard(text: String) {
        (getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.let { clipboard ->
            val clip = ClipData.newPlainText("message", text)
            clipboard.setPrimaryClip(clip)
            showToast("Текст скопирован")
        }
    }

    //Показывает диалог подтверждения удаления сообщения
    private fun showDeleteConfirmation(messageId: String) {
        AlertDialog.Builder(this)
            .setTitle("Удаление сообщения")
            .setMessage("Вы уверены, что хотите удалить это сообщение?")
            .setPositiveButton("Да") { _, _ -> deleteMessage(messageId) }
            .setNegativeButton("Нет", null)
            .show()
    }

    //Удаляет сообщение из базы данных
    private fun deleteMessage(messageId: String) {
        database.child(messageId).removeValue()
            .addOnSuccessListener { showToast("Сообщение удалено") }
            .addOnFailureListener { showToast("Ошибка удаления сообщения") }
    }

    //Форматирует временную метку в строку времени
    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    //Прокручивает список сообщений в самый низ
    private fun scrollToBottom() {
        binding.rcView.post {
            (binding.rcView.parent as? ScrollView)?.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    //Показывает краткое уведомление пользователю
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    //Проверяет статус администратора для текущего пользователя
    private fun checkAdminStatus() {
        Firebase.database.reference.child("chats")
            .orderByChild("name")
            .equalTo(chatName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentUser = auth.currentUser
                    // Проверяем, является ли текущий пользователь администратором чата
                    snapshot.children.firstOrNull()?.getValue(Chat::class.java)?.let { chat ->
                        isAdmin = chat.adminId == currentUser?.uid
                        setupChatTitleClickability()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("Ошибка проверки прав администратора: ${error.message}")
                }
            })
    }

    //Настраивает возможность редактирования названия чата для администратора
    private fun setupChatTitleClickability() {
        val chatTitleText = findViewById<TextView>(R.id.chatTitleText)
        if (isAdmin) {
            // Добавляем обработчик нажатия для администратора
            chatTitleText.setOnClickListener {
                showRenameChatDialog()
            }
            // Добавляем иконку редактирования рядом с названием
            chatTitleText.setCompoundDrawablesWithIntrinsicBounds(
                0, 0, R.drawable.border_color, 0
            )
            chatTitleText.compoundDrawablePadding = 8
        }
    }

    //Показывает диалог изменения названия чата
    private fun showRenameChatDialog() {
        val editText = EditText(this).apply {
            setText(chatName)
            setSelection(text.length)
            setPadding(32, 16, 32, 16)
        }

        AlertDialog.Builder(this)
            .setTitle("Изменить название чата")
            .setView(editText)
            .setPositiveButton("Сохранить") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && newName != chatName) {
                    updateChatName(newName)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    //Обновляет название чата в базе данных
    private fun updateChatName(newName: String) {
        // Обновляем название в узле chats
        Firebase.database.reference.child("chats")
            .orderByChild("name")
            .equalTo(chatName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (chatSnapshot in snapshot.children) {
                        chatSnapshot.ref.child("name").setValue(newName)
                            .addOnSuccessListener {
                                // Обновляем сообщения
                                val oldMessagesPath = chatName.replace(" ", "_")
                                val newMessagesPath = newName.replace(" ", "_")

                                val messagesRef = Firebase.database.reference
                                    .child("messages")

                                messagesRef.child(oldMessagesPath)
                                    .get()
                                    .addOnSuccessListener { dataSnapshot ->
                                        if (dataSnapshot.exists()) {
                                            messagesRef.child(newMessagesPath)
                                                .setValue(dataSnapshot.value)
                                                .addOnSuccessListener {
                                                    messagesRef.child(oldMessagesPath)
                                                        .removeValue()
                                                        .addOnSuccessListener {
                                                            chatName = newName
                                                            findViewById<TextView>(R.id.chatTitleText)?.text = newName
                                                            showToast("Название чата обновлено")

                                                            // Обновляем ссылки на базу данных
                                                            initializeDatabase()
                                                        }
                                                }
                                        }
                                    }
                            }
                            .addOnFailureListener {
                                showToast("Ошибка обновления названия чата")
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    showToast("Ошибка: ${error.message}")
                }
            })
    }
    // Добавьте эти функции в класс MainActivity

    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    val resizedBitmap = resizeBitmap(bitmap, 800) // максимальная ширина 800px
                    val base64Image = convertBitmapToBase64(resizedBitmap)

                    // Создаем сообщение с изображением
                    auth.currentUser?.let { user ->
                        val message = Message(
                            text = "", // пустой текст для сообщения с изображением
                            author = user.displayName ?: "Anonymous",
                            timestamp = System.currentTimeMillis(),
                            imageData = base64Image // добавляем изображение в формате Base64
                        )

                        database.push().setValue(message)
                            .addOnFailureListener { e ->
                                showToast("Ошибка отправки изображения: ${e.message}")
                            }
                    }
                } catch (e: IOException) {
                    showToast("Ошибка загрузки изображения")
                }
            }
        }
    }

    // Функция для изменения размера изображения
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val newWidth = maxWidth
        val newHeight = (maxWidth / ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    // Функция для конвертации Bitmap в Base64
    private fun convertBitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val byteArray = outputStream.toByteArray()
        return android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
    }

    // Функция для конвертации Base64 обратно в Bitmap
    private fun convertBase64ToBitmap(base64String: String): Bitmap? {
        return try {
            val imageBytes = android.util.Base64.decode(base64String, android.util.Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}


