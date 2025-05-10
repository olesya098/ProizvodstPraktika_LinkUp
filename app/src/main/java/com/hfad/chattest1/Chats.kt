package com.hfad.chattest1
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseException
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ServerValue
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
    private val filteredChatsList = mutableListOf<Chat>()
    private lateinit var database: DatabaseReference
    private lateinit var databaseim: DatabaseReference
    private lateinit var blacklistRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var searchEditText: EditText
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chats)

        val backgroundResId = intent.getIntExtra("background_theme", R.drawable.photo1)
        findViewById<View>(android.R.id.content).setBackgroundResource(backgroundResId)


        database = Firebase.database.reference.child("chats")
        auth = FirebaseAuth.getInstance()
        databaseim = Firebase.database.reference.child("users")
        blacklistRef = Firebase.database.reference.child("BlackList")
        drawerLayout = findViewById(R.id.drawerLayout)//вся страничка

        setupToolbar()
        setupSearch()
        setupRecyclerView()
        setupSideNavigation()
        loadChats()
        setupNavigationUserInfo()
        loadNavigationProfilePhoto()
    }

    private fun setupToolbar() {

        // Находим элемент Toolbar по его идентификатору из ресурсов
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        // Устанавливаем найденный Toolbar в качестве ActionBar для активности( основной панели управления)
        setSupportActionBar(toolbar)


        // Настройка кнопки меню
        // Находим кнопку меню по её идентификатору и устанавливаем обработчик нажатия
        findViewById<ImageButton>(R.id.menuButton).setOnClickListener {

            // Открываем боковое меню (Navigation Drawer) при нажатии на кнопку
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Настройка кнопки поиска
        findViewById<ImageButton>(R.id.searchButton).setOnClickListener {

            // Вызываем функцию для переключения состояния поиска
            toggleSearch()
        }
    }


    private fun setupSearch() {
        // Инициализация поля для ввода текста поиска
        searchEditText = findViewById(R.id.searchEditText)

        // Добавление слушателя для отслеживания изменений текста
        searchEditText.addTextChangedListener(object : TextWatcher {
            // Метод вызывается перед изменением текста
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            // Метод вызывается во время изменения текста
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            // Метод вызывается после изменения текста
            override fun afterTextChanged(s: Editable?) {
                // Фильтрация чатов на основе введенного текста
                filterChats(s.toString())
            }
        })
    }

    private fun toggleSearch() {
        // Получение ссылки на поле ввода поиска и заголовок тулбара
        val searchEditText = findViewById<EditText>(R.id.searchEditText)
        val toolbarTitle = findViewById<TextView>(R.id.toolbarTitle)

        // Проверка видимости поля ввода поиска
        if (searchEditText.visibility == View.VISIBLE) {// - View.VISIBLE — элемент видим и занимают место в макете.
            // Скрываем поле ввода поиска
            searchEditText.visibility = View.GONE
            toolbarTitle.visibility = View.VISIBLE
            searchEditText.setText("") // Очищаем текст в поле ввода

            // Скрываем клавиатуру
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)

            // Сбрасываем фильтр чатов
            filterChats("")
        } else {
            // Показываем поле ввода поиска
            searchEditText.visibility = View.VISIBLE
            toolbarTitle.visibility = View.GONE
            searchEditText.requestFocus() // Устанавливаем фокус на поле ввода

            // Показываем клавиатуру
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun filterChats(query: String) {
        // Очищаем список отфильтрованных чатов
        filteredChatsList.clear()

        // Если запрос пустой, добавляем все чаты в отфильтрованный список
        if (query.isEmpty()) {
            filteredChatsList.addAll(chatsList)
        } else {
            // Фильтруем чаты по имени, сравнивая с введенным текстом
            filteredChatsList.addAll(chatsList.filter {
                it.name.toLowerCase(Locale.getDefault())
                    .contains(query.toLowerCase(Locale.getDefault()))
            })//
        }//
//
        // Обновляем адаптер списка чатов с отфильтрованными данными
        chatsAdapter.updateList(filteredChatsList)//
    }

    private fun loadNavigationProfilePhoto() {
        // Получаем текущего пользователя из Firebase
        val currentUser = Firebase.auth.currentUser ?: return
        // Заменяем точки в email на подчеркивания для использования в базе данных
        val userEmail = currentUser.email?.replace(".", "_") ?: return
        // Находим элемент для отображения изображения профиля
        val navProfileImage = findViewById<ShapeableImageView>(R.id.navProfileImage)

        // Подписываемся на изменения в базе данных по пути, соответствующему email пользователя
        databaseim.child(userEmail).child("profilePhoto")
            .addValueEventListener(object : ValueEventListener {
                // Метод вызывается при изменении данных в базе
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Получаем строку с изображением в формате Base64
                    val base64Image = snapshot.getValue(String::class.java)
                    // Проверяем, что строка не пустая
                    if (!base64Image.isNullOrEmpty()) {
                        try {
                            // Декодируем строку Base64 в массив байтов
                            val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                            // Конвертируем массив байтов в объект Bitmap
                            val bitmap =
                                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            // Устанавливаем полученное изображение в элемент интерфейса
                            navProfileImage.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            // Обрабатываем возможные ошибки при загрузке изображения
                            Toast.makeText(
                                this@Chats,
                                "Ошибка загрузки фото профиля",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                // Метод вызывается при ошибке доступа к базе данных
                override fun onCancelled(error: DatabaseError) {
                    // Показываем сообщение об ошибке
                    Toast.makeText(
                        this@Chats,
                        "Ошибка загрузки фото: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }


    private fun setupNavigationUserInfo() {
        // Получаем текущего пользователя из Firebase Authentication
        val currentUser = auth.currentUser
        // Находим элементы TextView для отображения имени и email пользователя
        val navUserName = findViewById<TextView>(R.id.navUserName)
        val navUserEmail = findViewById<TextView>(R.id.navUserEmail)

        // Устанавливаем email пользователя, если он есть, иначе выводим сообщение
        navUserEmail.text = currentUser?.email ?: "email не указан"

        // Получаем email пользователя и заменяем точки на подчеркивания для использования в базе данных
        val userEmail = currentUser?.email?.replace(".", "_") ?: return
        // Ссылаемся на узел "users" в базе данных Firebase
        val database = Firebase.database.reference.child("users")

        // Добавляем слушатель для получения данных о пользователе
        database.child(userEmail).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Получаем профиль пользователя из снимка базы данных
                val profile = snapshot.getValue(UserProfile::class.java)
                if (profile != null) {
                    // Формируем полное имя пользователя, если оно указано
                    val fullName =
                        if (profile.firstName.isNotEmpty() && profile.lastName.isNotEmpty()) {
                            "${profile.firstName} ${profile.lastName}"
                        } else {
                            // Если имя и фамилия не указаны, используем отображаемое имя или "Пользователь"
                            profile.displayName.ifEmpty { "Пользователь" }
                        }
                    // Устанавливаем полное имя в TextView
                    navUserName.text = fullName
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Обрабатываем ошибку, если загрузка профиля была отменена
                Toast.makeText(
                    this@Chats,
                    "Ошибка загрузки профиля: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun setupRecyclerView() {
        chatsRecyclerView = findViewById(R.id.chatsRecyclerView)
        chatsRecyclerView.layoutManager = LinearLayoutManager(this)

        chatsAdapter = ChatsAdapter(
            chatsList,
            { chat -> openChat(chat) },
            { chat -> showDeleteChatConfirmation(chat) },
            { chat -> showInviteDialog(chat) },
            { chat -> showEditNameDialog(chat) },
            { chat -> showLeaveChatConfirmation(chat) },
            { chat -> showAddToBlacklistConfirmation(chat) } // Добавляем новый обработчик
        )
        chatsRecyclerView.adapter = chatsAdapter
    }

    private fun showAddToBlacklistConfirmation(chat: Chat) {
        AlertDialog.Builder(this)
            .setTitle("Добавить в черный список")
            .setMessage("Вы уверены, что хотите добавить чат \"${chat.name}\" в черный список? Чат будет скрыт из вашего списка.")
            .setPositiveButton("Да") { _, _ ->
                addChatToBlacklist(chat)
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    // Метод для добавления чата в черный список
    private fun addChatToBlacklist(chat: Chat) {
        val currentUser = auth.currentUser?.email?.replace(".", "_") ?: return

        // Создаем объект для черного списка с названием чата
        val blacklistEntry = mapOf(
            "chatId" to chat.id,
            "chatName" to chat.name,
            "addedAt" to ServerValue.TIMESTAMP
        )

        // Добавляем запись в черный список
        blacklistRef.child(currentUser).child(chat.id).setValue(blacklistEntry)
            .addOnSuccessListener {
                Toast.makeText(this, "Чат добавлен в черный список", Toast.LENGTH_SHORT).show()
                loadChats() // Обновляем список чатов
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка при добавлении в черный список: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupSideNavigation() {
        // Находим NavigationView, который содержит боковое меню
        val navigationView = findViewById<NavigationView>(R.id.navigationView)

        // Находим все контейнеры для различных пунктов меню
        val profileContainer = navigationView.findViewById<LinearLayout>(R.id.profileContainer)
        val chatsContainer = navigationView.findViewById<LinearLayout>(R.id.chatsContainer)
        val createChatContainer =
            navigationView.findViewById<LinearLayout>(R.id.createChatContainer)
        val joinChatContainer = navigationView.findViewById<LinearLayout>(R.id.joinChatContainer)
        val settingsContainer = navigationView.findViewById<LinearLayout>(R.id.settingsContainer)
        val signOutContainer = navigationView.findViewById<LinearLayout>(R.id.signOutContainer)

        // Устанавливаем начальное выделение на контейнере чатов
        chatsContainer.isSelected = true

        // Настраиваем обработчики нажатий для каждого контейнера
        profileContainer.setOnClickListener {
            clearSelection(navigationView) // Сбрасываем выделение других пунктов
            profileContainer.isSelected = true // Выделяем текущий пункт
            startActivity(Intent(this, ProfileActivity::class.java)) // Переход к экрану профиля
            finish() // Закрываем текущую активность
        }

        createChatContainer.setOnClickListener {
            clearSelection(navigationView) // Сбрасываем выделение других пунктов
            createChatContainer.isSelected = true // Выделяем текущий пункт
            showCreateChatDialog() // Показываем диалог для создания чата
        }

        joinChatContainer.setOnClickListener {
            clearSelection(navigationView) // Сбрасываем выделение других пунктов
            joinChatContainer.isSelected = true // Выделяем текущий пункт
            showJoinChatDialog() // Показываем диалог для присоединения к чату
        }

        settingsContainer.setOnClickListener {
            clearSelection(navigationView) // Сбрасываем выделение других пунктов
            settingsContainer.isSelected = true // Выделяем текущий пункт
            startActivity(Intent(this, Setting::class.java)) // Переход к экрану настроек
            finish() // Закрываем текущую активность
        }

        signOutContainer.setOnClickListener {
            val auth = FirebaseAuth.getInstance() // Получаем экземпляр FirebaseAuth
            auth.signOut() // Выход из текущей сессии

            // Настраиваем выход из Google аккаунта
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Запрашиваем ID токен
                .requestEmail() // Запрашиваем email
                .build()

            // Выход из Google аккаунта
            GoogleSignIn.getClient(this, gso).signOut().addOnCompleteListener {
                // Переход на экран входа после выхода
                val intent = Intent(this, SignInActivity::class.java)
                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Очищаем стек активностей
                startActivity(intent) // Запускаем экран входа
                finish() // Закрываем текущую активность
            }
        }
    }

    // Вспомогательный метод для сброса выделения всех пунктов меню
    private fun clearSelection(navigationView: NavigationView) {
        navigationView.findViewById<LinearLayout>(R.id.profileContainer).isSelected = false
        navigationView.findViewById<LinearLayout>(R.id.chatsContainer).isSelected = false
        navigationView.findViewById<LinearLayout>(R.id.createChatContainer).isSelected = false
        navigationView.findViewById<LinearLayout>(R.id.joinChatContainer).isSelected = false
        navigationView.findViewById<LinearLayout>(R.id.settingsContainer).isSelected = false
    }

    //Открывает выбранный чат
    private fun openChat(chat: Chat) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("CHAT_NAME", chat.name)
        }
        startActivity(intent)
    }

    //Показывает диалог редактирования названия чата
    //Доступно только администратору чата
    private fun showEditNameDialog(chat: Chat) {
        val currentUser = auth.currentUser
        // Проверяем, является ли текущий пользователь администратором чата
        if (currentUser?.uid != chat.adminId) {
            Toast.makeText(
                this,
                "Только администратор может изменять название чата",
                Toast.LENGTH_SHORT
            ).show()
            return // Если не администратор, выходим из функции
        }

        // Создаем представление для диалогового окна
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_chat, null)
        val editTextChatName = dialogView.findViewById<EditText>(R.id.editTextChatName)
        // Устанавливаем текущее название чата в поле ввода
        editTextChatName.setText(chat.name)

        // Создаем и показываем диалоговое окно
        AlertDialog.Builder(this)
            .setTitle("Изменить название чата")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val newName = editTextChatName.text.toString().trim() // Получаем новое название
                // Проверяем, что новое название не пустое
                if (newName.isNotEmpty()) {
                    updateChatName(chat, newName) // Обновляем название чата
                } else {
                    Toast.makeText(
                        this,
                        "Название чата не может быть пустым",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Отмена", null) // Кнопка отмены
            .show()
    }

    // Обновляет название чата в базе данных
// Также обновляет путь к сообщениям чата
    private fun updateChatName(chat: Chat, newName: String) {
        // Ищем чат по старому названию
        database.orderByChild("name").equalTo(chat.name)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (chatSnapshot in snapshot.children) {
                        // Обновляем название чата в базе данных
                        chatSnapshot.ref.child("name").setValue(newName)
                            .addOnSuccessListener {
                                // Обновляем путь к сообщениям
                                val oldMessagesPath = chat.name.replace(" ", "_")
                                val newMessagesPath = newName.replace(" ", "_")

                                val messagesRef = Firebase.database.reference
                                    .child("messages")

                                // Копируем сообщения в новый путь
                                messagesRef.child(oldMessagesPath)
                                    .get()
                                    .addOnSuccessListener { dataSnapshot ->
                                        if (dataSnapshot.exists()) {
                                            // Устанавливаем сообщения по новому пути
                                            messagesRef.child(newMessagesPath)
                                                .setValue(dataSnapshot.value)
                                                .addOnSuccessListener {
                                                    // Удаляем старые сообщения
                                                    messagesRef.child(oldMessagesPath)
                                                        .removeValue()
                                                }
                                        }
                                    }

                                Toast.makeText(
                                    this@Chats,
                                    "Название чата изменено",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    this@Chats,
                                    "Не удалось изменить название чата",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@Chats,
                        "Ошибка: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun showCreateChatDialog() {
        // Создаем представление для диалогового окна, используя макет dialog_create_chat
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_chat, null)
        // Находим поле ввода для названия чата
        val editTextChatName = dialogView.findViewById<EditText>(R.id.editTextChatName)

        // Создаем диалоговое окно для создания нового чата
        AlertDialog.Builder(this)
            .setTitle("Создать новый чат") // Заголовок диалога
            .setView(dialogView) // Устанавливаем представление диалога
            .setPositiveButton("Создать") { _, _ -> // Обработчик нажатия на кнопку "Создать"
                val chatName = editTextChatName.text.toString().trim() // Получаем название чата
                if (chatName.isNotEmpty()) { // Проверяем, что название не пустое
                    createNewChat(chatName) // Создаем новый чат
                } else {
                    // Если название пустое, показываем сообщение
                    Toast.makeText(this, "Введите название чата", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null) // Обработчик нажатия на кнопку "Отмена"
            .show() // Показываем диалог
    }

    // Создает новый чат в базе данных.
// Генерирует уникальный код приглашения и сохраняет информацию о чате в Firebase.
    private fun createNewChat(chatName: String) {
        val currentUser = auth.currentUser // Получаем текущего пользователя
        if (currentUser != null) { // Проверяем, что пользователь авторизован
            val inviteCode = generateInviteCode() // Генерируем код приглашения

            // Создаем новый объект чата с необходимыми данными
            val newChat = Chat(
                id = database.push().key ?: "", // Генерируем уникальный ID для чата
                name = chatName, // Устанавливаем название чата
                time = SimpleDateFormat(
                    "HH:mm",
                    Locale.getDefault()
                ).format(Date()), // Устанавливаем текущее время
                inviteCode = inviteCode, // Устанавливаем код приглашения
                adminId = currentUser.uid, // Устанавливаем ID администратора
                members = listOf(currentUser.uid) // Добавляем текущего пользователя в список участников
            )

            // Сохраняем новый чат в базе данных
            newChat.id.let { chatId ->
                database.child(chatId).setValue(newChat) // Сохраняем чат по его ID
                    .addOnSuccessListener {
                        showInviteCodeDialog(inviteCode) // Показываем диалог с кодом приглашения
                    }
                    .addOnFailureListener {
                        // Если произошла ошибка, показываем сообщение
                        Toast.makeText(this, "Ошибка создания чата", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    // Генерирует случайный 6-значный код приглашения.
    private fun generateInviteCode(): String {
        return (100000..999999).random()
            .toString() // Генерируем случайное число от 100000 до 999999
    }

    // Показывает диалоговое окно с кодом приглашения после создания чата.
// Доступно только администратору чата.
    private fun showInviteCodeDialog(inviteCode: String) {
        AlertDialog.Builder(this)
            .setTitle("Код приглашения") // Заголовок диалога
            .setMessage("Код для приглашения в чат: $inviteCode\n\nПоделитесь этим кодом с друзьями.") // Сообщение с кодом
            .setPositiveButton("Поделиться") { _, _ -> // Обработчик нажатия на кнопку "Поделиться"
                shareInviteCode(inviteCode) // Вызываем метод для совместного использования кода
            }
            .setNegativeButton("Закрыть", null) // Обработчик нажатия на кнопку "Закрыть"
            .show() // Показываем диалог
    }

    //Открывает системное окно для отправки кода приглашения.
    private fun showInviteDialog(chat: Chat) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Проверяем, является ли текущий пользователь администратором чата
            if (chat.adminId == currentUser.uid) {
                // Показываем код приглашения
                AlertDialog.Builder(this)
                    .setTitle("Пригласить в чат")
                    .setMessage(
                        "Код приглашения для чата \"${chat.name}\": ${chat.inviteCode}\n\n" +
                                "Поделитесь этим кодом и ссылкой на скачивание приложения с друзьями для приглашения их в чат."
                    )
                    .setPositiveButton("Поделиться") { _, _ ->
                        shareInviteCode(chat.inviteCode)
                    }
                    .setNegativeButton("Закрыть", null)
                    .show()
            } else {
                Toast.makeText(
                    this,
                    "Только администратор чата может приглашать новых участников",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Открывает системное окно для отправки кода приглашения.
    private fun shareInviteCode(inviteCode: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Присоединяйся к чату! Код приглашения: $inviteCode\n\n" +
                        "Скачать приложение: https://github.com/olesya098/LinkUp"
            )
        }
        startActivity(Intent.createChooser(shareIntent, "Поделиться кодом"))
    }

    //Показывает диалоговое окно для присоединения к чату по коду.
//Включает валидацию ввода (только 6 цифр).
    private fun showJoinChatDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_join_chat, null)
        val editTextInviteCode = dialogView.findViewById<EditText>(R.id.editTextInviteCode)

        // Дополнительные настройки для EditText
        // Ограничения ввода: только 6 цифр
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
                    Toast.makeText(
                        this,
                        "Введите корректный код приглашения (6 цифр)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    //Присоединяет пользователя к чату по коду приглашения.
//Проверяет существование чата и отсутствие пользователя в нём.
    private fun joinChatByInviteCode(inviteCode: String) {
        val currentUser = auth.currentUser // Получаем текущего пользователя
        if (currentUser != null) { // Проверяем, что пользователь авторизован
            // Ищем чат по коду приглашения в базе данных
            database.orderByChild("inviteCode").equalTo(inviteCode)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) { // Если чат с таким кодом найден
                            for (chatSnapshot in snapshot.children) {
                                val chat =
                                    chatSnapshot.getValue(Chat::class.java) // Получаем объект чата
                                chat?.let {
                                    // Проверяем, что пользователь еще не в чате
                                    if (!it.members.contains(currentUser.uid)) {
                                        // Обновляем список участников, добавляя текущего пользователя
                                        val updatedMembers = it.members.toMutableList().apply {
                                            add(currentUser.uid)
                                        }
                                        // Сохраняем обновленный список участников в базе данных
                                        chatSnapshot.ref.child("members").setValue(updatedMembers)
                                            .addOnSuccessListener {
                                                // Успешное присоединение к чату
                                                Toast.makeText(
                                                    this@Chats,
                                                    "Вы успешно присоединились к чату ${chat.name}",
                                                    Toast.LENGTH_SHORT
                                                ).show()

                                                // Переход в чат после присоединения
                                                val intent =
                                                    Intent(this@Chats, MainActivity::class.java)
                                                intent.putExtra(
                                                    "CHAT_NAME",
                                                    chat.name
                                                ) // Передаем название чата
                                                startActivity(intent) // Запускаем активность чата
                                            }
                                            .addOnFailureListener {
                                                // Ошибка при присоединении к чату
                                                Toast.makeText(
                                                    this@Chats,
                                                    "Не удалось присоединиться к чату",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    } else {
                                        // Пользователь уже состоит в этом чате
                                        Toast.makeText(
                                            this@Chats,
                                            "Вы уже состоите в этом чате",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        } else {
                            // Чат с таким кодом не найден
                            Toast.makeText(
                                this@Chats,
                                "Чат с таким кодом не найден",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Обработка ошибки при запросе к базе данных
                        Toast.makeText(
                            this@Chats,
                            "Ошибка: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }
    }

    private fun loadChats() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userEmail = currentUser.email?.replace(".", "_") ?: return

            // Сначала получаем черный список пользователя
            blacklistRef.child(userEmail).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(blacklistSnapshot: DataSnapshot) {
                    // Создаем множество ID чатов из черного списка
                    val blacklistedChatIds = blacklistSnapshot.children.mapNotNull {
                        it.child("chatId").getValue(String::class.java)
                    }.toSet()

                    // Теперь загружаем чаты, исключая те, что в черном списке
                    database.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            chatsList.clear()
                            for (childSnapshot in snapshot.children) {
                                try {
                                    val chat = childSnapshot.getValue(Chat::class.java)
                                    chat?.let {
                                        // Добавляем чат только если он не в черном списке
                                        if (it.members.contains(currentUser.uid) &&
                                            !blacklistedChatIds.contains(it.id)) {
                                            chatsList.add(it)
                                        }
                                    }
                                } catch (e: DatabaseException) {
                                    Log.e("ChatError", "Error parsing chat: ${e.message}")
                                    continue
                                }
                            }
                            chatsAdapter.notifyDataSetChanged()
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(
                                this@Chats,
                                "Ошибка загрузки чатов",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@Chats,
                        "Ошибка загрузки черного списка",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }

    // Показывает диалог подтверждения удаления чата.
    private fun showDeleteChatConfirmation(chat: Chat) {
        AlertDialog.Builder(this) // Создаем диалог
            .setTitle("Удаление чата") // Заголовок диалога
            .setMessage("Вы уверены, что хотите удалить чат \"${chat.name}\"?") // Сообщение
            .setPositiveButton("Да") { _, _ -> // Кнопка "Да"
                deleteChat(chat) // Вызываем метод удаления чата
            }
            .setNegativeButton("Нет", null) // Кнопка "Нет"
            .show() // Показываем диалог
    }

    // Удаляет чат из базы данных.
// Доступно только администратору чата.
    private fun deleteChat(chat: Chat) {
        val currentUser = auth.currentUser // Получаем текущего пользователя
        if (currentUser != null) { // Проверяем, что пользователь авторизован
            // Ищем чат по имени в базе данных
            database.orderByChild("name").equalTo(chat.name)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (chatSnapshot in snapshot.children) { // Проходим по всем найденным чатам
                            val chatData =
                                chatSnapshot.getValue(Chat::class.java) // Получаем данные чата
                            // Проверяем, является ли текущий пользователь администратором чата
                            if (chatData?.adminId == currentUser.uid) {
                                // Удаляем чат из базы данных
                                chatSnapshot.ref.removeValue()
                                    .addOnSuccessListener {
                                        // Удаляем сообщения, связанные с чатом
                                        Firebase.database.reference
                                            .child("messages")
                                            .child(chat.name.replace(" ", "_"))
                                            .removeValue()

                                        // Уведомляем пользователя об успешном удалении
                                        Toast.makeText(
                                            this@Chats,
                                            "Чат \"${chat.name}\" удален",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .addOnFailureListener {
                                        // Уведомляем пользователя об ошибке удаления
                                        Toast.makeText(
                                            this@Chats,
                                            "Не удалось удалить чат",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            } else {
                                // Уведомляем, что только администратор может удалить чат
                                Toast.makeText(
                                    this@Chats,
                                    "Удалить чат может только его администратор",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Уведомляем об ошибке при получении данных
                        Toast.makeText(
                            this@Chats,
                            "Ошибка: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }
    }

    // Показывает диалог подтверждения выхода из чата.
// Недоступно для администратора чата.
    private fun showLeaveChatConfirmation(chat: Chat) {
        val currentUser = auth.currentUser // Получаем текущего пользователя
        // Проверяем, является ли текущий пользователь администратором чата
        if (currentUser?.uid == chat.adminId) {
            // Уведомляем, что администратор не может покинуть чат
            Toast.makeText(
                this,
                "Администратор не может покинуть чат. Вы можете удалить его",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Создаем диалог подтверждения выхода из чата
        AlertDialog.Builder(this)
            .setTitle("Выход из чата")
            .setMessage("Вы уверены, что хотите покинуть чат \"${chat.name}\"?")
            .setPositiveButton("Да") { _, _ -> // Если пользователь подтверждает
                leaveChat(chat) // Вызываем метод выхода из чата
            }
            .setNegativeButton("Нет", null) // Если пользователь отменяет
            .show() // Показываем диалог
    }

    // Удаляет пользователя из списка участников чата.
    private fun leaveChat(chat: Chat) {
        // Получаем текущего пользователя
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Запрашиваем чат по его имени из базы данных
            database.orderByChild("name").equalTo(chat.name)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // Проходим по всем найденным чатам
                        for (chatSnapshot in snapshot.children) {
                            // Получаем данные чата
                            val chatData = chatSnapshot.getValue(Chat::class.java)
                            if (chatData != null) {
                                // Удаляем пользователя из списка участников
                                val updatedMembers = chatData.members.toMutableList()
                                updatedMembers.remove(currentUser.uid) // Удаляем ID текущего пользователя

                                // Обновляем список участников в базе данных
                                chatSnapshot.ref.child("members").setValue(updatedMembers)
                                    .addOnSuccessListener {
                                        // Успешное удаление, показываем сообщение
                                        Toast.makeText(
                                            this@Chats,
                                            "Вы вышли из чата \"${chat.name}\"",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .addOnFailureListener {
                                        // Ошибка при удалении, показываем сообщение
                                        Toast.makeText(
                                            this@Chats,
                                            "Не удалось выйти из чата",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Обработка ошибки при запросе данных
                        Toast.makeText(
                            this@Chats,
                            "Ошибка: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }
    }
}
