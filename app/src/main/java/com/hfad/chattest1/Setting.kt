package com.hfad.chattest1

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.graphics.drawable.DrawableCompat.applyTheme
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.hfad.chattest1.databinding.ActivitySettingBinding

class Setting : AppCompatActivity() {
    // Binding для ActivitySetting
    private lateinit var binding: ActivitySettingBinding
    // Ссылка на базу данных Firebase
    private lateinit var database: DatabaseReference
    // Ссылка на черный список в базе данных
    private lateinit var blackListRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Включаем edge-to-edge отображение (на весь экран)
        enableEdgeToEdge()

        // Инициализация binding
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Устанавливаем цвет статус бара
        window.statusBarColor = Color.parseColor("#5B9693")
        // Устанавливаем светлый текст в статус баре
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        setContentView(binding.root)

        // Инициализация ссылки на базу данных пользователей
        database = Firebase.database.reference.child("users")
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            // Инициализация ссылки на черный список текущего пользователя
            blackListRef = Firebase.database.reference
                .child("blacklists")
                .child(currentUser.uid)
        }

        // Настройка всех компонентов интерфейса
        setupViews()
        setupSideNavigation()
        setupExpandableCards()
        setupNavigationUserInfo()
        loadNavigationProfilePhoto()
        setupMenuButton()
        setupBlackList()

        // Обработка системных отступов (insets)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /**
     * Настройка функционала черного списка
     */
    private fun setupBlackList() {
        val currentUser = Firebase.auth.currentUser ?: return

        // Обновляем ссылку на черный список текущего пользователя
        blackListRef = Firebase.database.reference
            .child("BlackList")
            .child(currentUser.uid)

        // Обработчик клика по заголовку черного списка
        binding.BlackList.setOnClickListener {
            binding.BlackListContent.visibility = if (binding.BlackListContent.visibility == View.VISIBLE) {
                View.GONE
            } else {
                loadBlockedUsers()
                View.VISIBLE
            }
        }
    }

    /**
     * Загрузка заблокированных пользователей из базы данных
     */
    private fun loadBlockedUsers() {
        val blockedUsersLayout = binding.BlackListContent
        blockedUsersLayout.removeAllViews()

        val currentUser = Firebase.auth.currentUser?.email?.replace(".", "_") ?: return
        val blacklistRef = Firebase.database.reference.child("BlackList").child(currentUser)

        // Слушатель изменений в черном списке
        blacklistRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                blockedUsersLayout.removeAllViews()

                // Если черный список пуст, показываем сообщение
                if (!snapshot.exists()) {
                    addEmptyListView(blockedUsersLayout)
                    return
                }

                // Перебираем все заблокированные чаты
                for (chatSnapshot in snapshot.children) {
                    val chatName = chatSnapshot.child("chatName").getValue(String::class.java)
                    val chatId = chatSnapshot.child("chatId").getValue(String::class.java)

                    if (chatName != null && chatId != null) {
                        // Создаем view для заблокированного чата
                        val chatView = TextView(this@Setting).apply {
                            text = "Чат: $chatName"
                            textSize = 16f
                            setTextColor(resources.getColor(android.R.color.black, theme))
                            setPadding(16, 8, 16, 8)
                            background = ContextCompat.getDrawable(context, R.drawable.ripple_effect)

                            // Добавляем слушатель долгого нажатия для разблокировки
                            setOnLongClickListener {
                                showUnblockDialog(chatId, chatName)
                                true
                            }
                        }
                        blockedUsersLayout.addView(chatView)
                    }
                }

                // Если после загрузки список пуст, показываем сообщение
                if (blockedUsersLayout.childCount == 0) {
                    addEmptyListView(blockedUsersLayout)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showError("Ошибка загрузки черного списка: ${error.message}")
            }
        })
    }

    /**
     * Показывает диалог подтверждения разблокировки чата
     */
    private fun showUnblockDialog(chatId: String, chatName: String) {
        AlertDialog.Builder(this)
            .setTitle("Разблокировать чат")
            .setMessage("Вы уверены, что хотите разблокировать чат \"$chatName\"?")
            .setPositiveButton("Разблокировать") { _, _ ->
                unblockChat(chatId)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    /**
     * Разблокирует указанный чат
     */
    private fun unblockChat(chatId: String) {
        val currentUser = Firebase.auth.currentUser?.email?.replace(".", "_") ?: return

        // Удаляем чат из черного списка
        Firebase.database.reference
            .child("BlackList")
            .child(currentUser)
            .child(chatId)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Чат успешно разблокирован",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    "Ошибка при разблокировке: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    /**
     * Добавляет сообщение о пустом черном списке
     */
    private fun addEmptyListView(container: LinearLayout) {
        val emptyView = TextView(this).apply {
            text = "Черный список пуст"
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.black, theme))
            setPadding(16, 8, 16, 8)
        }
        container.addView(emptyView)
    }

    /**
     * Показывает сообщение об ошибке
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Настраивает основные view
     */
    private fun setupViews() {
        // Все view теперь доступны через binding
        with(binding) {
            // Показываем карточки профиля, информации и помощи
            profileCard.visibility = View.VISIBLE
            aboutCard.visibility = View.VISIBLE
            helpCard.visibility = View.VISIBLE
        }
    }

    /**
     * Настраивает кнопку меню и боковую панель навигации
     */
    private fun setupMenuButton() {
        binding.apply {
            // Обработчик клика по кнопке меню
            menuButton.setOnClickListener {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    drawerLayout.openDrawer(GravityCompat.START)
                }
            }

            // Слушатель состояния боковой панели
            drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
                override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
                override fun onDrawerOpened(drawerView: View) {}
                override fun onDrawerClosed(drawerView: View) {}
                override fun onDrawerStateChanged(newState: Int) {}
            })
        }
    }

    /**
     * Загружает фото профиля для навигационного меню
     */
    private fun loadNavigationProfilePhoto() {
        val currentUser = Firebase.auth.currentUser ?: return
        val userEmail = currentUser.email?.replace(".", "_") ?: return

        // Получаем фото профиля из базы данных
        database.child(userEmail).child("profilePhoto")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val base64Image = snapshot.getValue(String::class.java)
                    if (!base64Image.isNullOrEmpty()) {
                        try {
                            // Декодируем base64 в изображение
                            val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            binding.navigationView.findViewById<ShapeableImageView>(R.id.navProfileImage)
                                .setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            Toast.makeText(this@Setting,
                                "Ошибка загрузки фото профиля",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@Setting,
                        "Ошибка загрузки фото: ${error.message}",
                        Toast.LENGTH_SHORT).show()
                }
            })
    }

    /**
     * Настраивает информацию о пользователе в навигационном меню
     */
    private fun setupNavigationUserInfo() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val navigationView = binding.navigationView
        val navUserName = navigationView.findViewById<TextView>(R.id.navUserName)
        val navUserEmail = navigationView.findViewById<TextView>(R.id.navUserEmail)

        // Устанавливаем email пользователя
        navUserEmail.text = currentUser?.email ?: "email не указан"

        val userEmail = currentUser?.email?.replace(".", "_") ?: return
        // Получаем данные профиля пользователя
        database.child(userEmail).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val profile = snapshot.getValue(UserProfile::class.java)
                if (profile != null) {
                    // Формируем полное имя пользователя
                    val fullName = if (profile.firstName.isNotEmpty() && profile.lastName.isNotEmpty()) {
                        "${profile.firstName} ${profile.lastName}"
                    } else {
                        profile.displayName.ifEmpty { "Пользователь" }
                    }
                    navUserName.text = fullName
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Обработка ошибки
            }
        })
    }

    /**
     * Настраивает боковую панель навигации
     */
    private fun setupSideNavigation() {
        binding.navigationView.apply {
            val profileContainer = findViewById<LinearLayout>(R.id.profileContainer)
            val chatsContainer = findViewById<LinearLayout>(R.id.chatsContainer)
            val settingsContainer = findViewById<LinearLayout>(R.id.settingsContainer)
            val signOutContainer = findViewById<LinearLayout>(R.id.signOutContainer)

            // Выделяем текущий раздел (Настройки)
            settingsContainer.isSelected = true

            // Обработчик клика по разделу чатов
            chatsContainer.setOnClickListener {
                clearSelection()
                chatsContainer.isSelected = true
                startActivity(Intent(this@Setting, Chats::class.java))
                finish()
            }

            // Обработчик клика по разделу профиля
            profileContainer.setOnClickListener {
                clearSelection()
                settingsContainer.isSelected = true
                startActivity(Intent(this@Setting, ProfileActivity::class.java))
                finish()
            }

            // Обработчик выхода из аккаунта
            signOutContainer.setOnClickListener {
                signOut()
            }
        }
    }

    /**
     * Выход из аккаунта
     */
    private fun signOut() {
        // Выход из Firebase
        FirebaseAuth.getInstance().signOut()

        // Настройка Google SignIn
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        // Выход из Google аккаунта
        GoogleSignIn.getClient(this, gso).signOut().addOnCompleteListener {
            val intent = Intent(this, SignInActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    /**
     * Снимает выделение со всех пунктов навигационного меню
     */
    private fun clearSelection() {
        binding.navigationView.apply {
            findViewById<LinearLayout>(R.id.profileContainer).isSelected = false
            findViewById<LinearLayout>(R.id.chatsContainer).isSelected = false
            findViewById<LinearLayout>(R.id.settingsContainer).isSelected = false
        }
    }

    /**
     * Настраивает раскрывающиеся карточки
     */
    private fun setupExpandableCards() {
        binding.apply {
            // Обработчик клика по заголовку "О приложении"
            aboutHeader.setOnClickListener {
                aboutContent.visibility = if (aboutContent.visibility == View.VISIBLE) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
            }

            // Обработчик клика по заголовку "Помощь"
            helpHeader.setOnClickListener {
                helpContent.visibility = if (helpContent.visibility == View.VISIBLE) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
            }
        }
    }
}