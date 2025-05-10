
package com.hfad.chattest1

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.hfad.chattest1.databinding.ActivityProfileBinding
import java.io.ByteArrayOutputStream
import java.io.IOException


class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding // Связываем представление с кодом
    private lateinit var database: DatabaseReference // Ссылка на базу данных Firebase
    private var isEditMode = false // Флаг для отслеживания режима редактирования


    // Регистрация для получения результата выбора изображения
    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> // Обработка результата выбора изображения
        if (result.resultCode == Activity.RESULT_OK) { // Проверяем, что результат успешный
            result.data?.data?.let { uri -> // Получаем URI выбранного изображения
                try {
                    // Загружаем изображение из URI
                    val inputStream =
                        contentResolver.openInputStream(uri) // Открываем поток для чтения изображения
                    val bitmap =
                        BitmapFactory.decodeStream(inputStream) // Декодируем поток в Bitmap

                    // Изменяем размер изображения для оптимизации
                    val resizedBitmap = resizeBitmap(bitmap, 500) // максимальная ширина 500px

                    // Конвертируем в Base64
                    val base64Image =
                        convertBitmapToBase64(resizedBitmap) // Преобразуем Bitmap в строку Base64

                    // Сохраняем в Firebase
                    savePhotoToDatabase(base64Image) // Сохраняем изображение в базе данных

                    // Отображаем фото в ImageView
                    binding.profileImage.setImageBitmap(resizedBitmap) // Устанавливаем измененное изображение в ImageView

                } catch (e: IOException) { // Обработка ошибок при загрузке изображения
                    Toast.makeText(this, "Ошибка загрузки изображения", Toast.LENGTH_SHORT)
                        .show() // Показываем сообщение об ошибке
                }
            }
        }
    }

    private fun setupPhotoSelection() {
        binding.fabEditPhoto.setOnClickListener { // Устанавливаем обработчик нажатия на кнопку выбора фото
            val intent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            ) // Создаем интент для выбора изображения
            selectImageLauncher.launch(intent) // Запускаем выбор изображения
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
        val width = bitmap.width // Получаем ширину изображения
        val height = bitmap.height // Получаем высоту изображения

        if (width <= maxWidth) return bitmap // Если ширина меньше максимальной, возвращаем оригинальное изображение

        val ratio = width.toFloat() / height.toFloat() // Вычисляем соотношение сторон
        val newWidth = maxWidth // Устанавливаем новую ширину
        val newHeight =
            (maxWidth / ratio).toInt() // Вычисляем новую высоту с учетом соотношения сторон

        return Bitmap.createScaledBitmap(
            bitmap,
            newWidth,
            newHeight,
            true
        ) // Создаем и возвращаем измененное изображение
    }


    // Конвертирует Bitmap в строку Base64
    private fun convertBitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream() // Создаем поток для записи байтов
        bitmap.compress(
            Bitmap.CompressFormat.JPEG,
            70,
            outputStream
        ) // Сжимаем изображение в JPEG формате
        val byteArray = outputStream.toByteArray() // Получаем массив байтов
        return Base64.encodeToString(
            byteArray,
            Base64.DEFAULT
        ) // Кодируем массив байтов в строку Base64
    }

    // Сохраняет фото в базе данных
    private fun savePhotoToDatabase(base64Image: String) {
        val currentUser = Firebase.auth.currentUser ?: return // Получаем текущего пользователя
        val userEmail = currentUser.email?.replace(".", "_")
            ?: return // Заменяем точки в email для использования в качестве ключа

        // Сохраняем строку Base64 в базе данных
        database.child(userEmail).child("profilePhoto").setValue(base64Image)
            .addOnSuccessListener {
                Toast.makeText(this, "Фото профиля обновлено", Toast.LENGTH_SHORT)
                    .show() // Успешное обновление
            }
            .addOnFailureListener { e -> // Обработка ошибок при сохранении
                Toast.makeText(this, "Ошибка сохранения фото: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    // Загружает фото профиля из базы данных
    private fun loadProfilePhoto() {
        val currentUser = Firebase.auth.currentUser ?: return // Получаем текущего пользователя
        val userEmail = currentUser.email?.replace(".", "_")
            ?: return // Заменяем точки в email для использования в качестве ключа

        // Получаем фото профиля из базы данных
        database.child(userEmail).child("profilePhoto")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val base64Image =
                        snapshot.getValue(String::class.java) // Получаем строку Base64
                    if (!base64Image.isNullOrEmpty()) { // Проверяем, что строка не пустая
                        try {
                            val imageBytes = Base64.decode(
                                base64Image,
                                Base64.DEFAULT
                            ) // Декодируем строку Base64 в байты
                            val bitmap = BitmapFactory.decodeByteArray(
                                imageBytes,
                                0,
                                imageBytes.size
                            ) // Декодируем байты в Bitmap
                            binding.profileImage.setImageBitmap(bitmap) // Устанавливаем изображение в ImageView
                        } catch (e: Exception) { // Обработка ошибок при загрузке изображения
                            Toast.makeText(
                                this@ProfileActivity,
                                "Ошибка загрузки фото профиля",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) { // Обработка ошибок при запросе к базе данных
                    Toast.makeText(
                        this@ProfileActivity,
                        "Ошибка загрузки фото: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // Вызов метода родительского класса для инициализации активности
        binding =
            ActivityProfileBinding.inflate(layoutInflater) // Инициализация привязки представления для доступа к элементам интерфейса
        setContentView(binding.root) // Установка корневого представления для активности

        window.statusBarColor = Color.parseColor("#5B9693")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        setContentView(binding.root)

        // Инициализация ссылки на базу данных Firebase для пользователей
        database = Firebase.database.reference.child("users")
        // Получение текущего пользователя из Firebase Authentication
        val currentUser = Firebase.auth.currentUser

        // Установка текста в TextView с электронной почтой текущего пользователя или сообщение об отсутствии
        binding.tvUserEmail.text = currentUser?.email ?: "email не указан"

        // Вызов методов для загрузки профиля пользователя и настройки интерфейса
        loadUserProfile() // Загрузка профиля пользователя
        setupListeners() // Настройка слушателей событий
        setupSideNavigation() // Настройка боковой навигации
        setEditMode(false) // Установка режима редактирования в false (т.е. отключение редактирования)
        setupNavigationUserInfo() // Настройка информации о пользователе в навигации
        setupPhotoSelection() // Настройка выбора фото
        loadProfilePhoto() // Загрузка фото профиля
        loadNavigationProfilePhoto() // Загрузка фото профиля для боковой навигации
    }

    private fun loadNavigationProfilePhoto() {
        // Получение текущего пользователя, если он не существует, выходим из функции
        val currentUser = Firebase.auth.currentUser ?: return
        // Замена точки в электронной почте на подчеркивание для использования в качестве ключа в базе данных
        val userEmail = currentUser.email?.replace(".", "_") ?: return
        // Получение ссылки на ImageView для отображения фото профиля в боковой навигации
        val navProfileImage =
            binding.navigationView.findViewById<ShapeableImageView>(R.id.navProfileImage)

        // Добавление слушателя для получения фото профиля из базы данных
        database.child(userEmail).child("profilePhoto")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Получение значения фото профиля из базы данных
                    val base64Image = snapshot.getValue(String::class.java)
                    // Проверка, что значение не пустое
                    if (!base64Image.isNullOrEmpty()) {
                        try {
                            // Декодирование строки base64 в массив байтов
                            val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                            // Преобразование массива байтов в Bitmap
                            val bitmap =
                                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            // Установка полученного Bitmap в ImageView
                            navProfileImage.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            // Обработка ошибок при загрузке фото
                            Toast.makeText(
                                this@ProfileActivity,
                                "Ошибка загрузки фото профиля",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Обработка ошибок при обращении к базе данных
                    Toast.makeText(
                        this@ProfileActivity,
                        "Ошибка загрузки фото: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun setupNavigationUserInfo() {
        // Получаем текущего пользователя из Firebase Authentication
        val currentUser = Firebase.auth.currentUser
        // Находим элементы TextView для отображения имени и email пользователя в навигационном меню
        val navUserName = binding.navigationView.findViewById<TextView>(R.id.navUserName)
        val navUserEmail = binding.navigationView.findViewById<TextView>(R.id.navUserEmail)

        // Устанавливаем email пользователя, если он есть, иначе показываем сообщение "email не указан"
        navUserEmail.text = currentUser?.email ?: "email не указан"

        // Получаем email пользователя и заменяем точки на подчеркивания для использования в базе данных
        val userEmail = currentUser?.email?.replace(".", "_") ?: return
        // Добавляем слушатель для получения данных о пользователе из базы данных
        database.child(userEmail).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Получаем профиль пользователя из снимка базы данных
                val profile = snapshot.getValue(UserProfile::class.java)
                if (profile != null) {
                    // Формируем полное имя пользователя, если есть имя и фамилия, иначе используем отображаемое имя или "Пользователь"
                    val fullName =
                        if (profile.firstName.isNotEmpty() && profile.lastName.isNotEmpty()) {
                            "${profile.firstName} ${profile.lastName}"
                        } else {
                            profile.displayName.ifEmpty { "Пользователь" }
                        }
                    // Устанавливаем полное имя в TextView
                    navUserName.text = fullName
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Обрабатываем ошибку загрузки профиля, показывая сообщение пользователю
                Toast.makeText(
                    this@ProfileActivity,
                    "Ошибка загрузки профиля: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun setEditMode(enabled: Boolean) {
        // Устанавливаем режим редактирования
        isEditMode = enabled
        binding.apply {
            // Включаем или отключаем поля ввода в зависимости от режима редактирования
            edFirstName.isEnabled = enabled
            edLastName.isEnabled = enabled
            edBio.isEnabled = enabled
            edPhone.isEnabled = enabled
            edLocation.isEnabled = enabled

            // Показываем или скрываем кнопки "Сохранить" и "Редактировать"
            btnSave.isVisible = enabled
            btnEdit.isVisible = !enabled

            // Устанавливаем цвет текста для полей ввода
            val textColor = Color.parseColor("#6B4F38")
            edFirstName.setTextColor(textColor)
            edLastName.setTextColor(textColor)
            edBio.setTextColor(textColor)
            edPhone.setTextColor(textColor)
            edLocation.setTextColor(textColor)
        }
    }


    private fun loadUserProfile() {
        // Получаем текущего пользователя из Firebase
        val currentUser = Firebase.auth.currentUser ?: return
        // Заменяем точку в email на подчеркивание для использования в базе данных
        val userEmail = currentUser.email?.replace(".", "_") ?: return

        // Добавляем слушатель для получения данных профиля пользователя из базы данных
        database.child(userEmail).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Получаем объект профиля пользователя из снимка базы данных
                val profile = snapshot.getValue(UserProfile::class.java) ?: return

                // Обновляем поля ввода данными профиля
                binding.apply {
                    edFirstName.setText(profile.firstName) // Устанавливаем имя
                    edLastName.setText(profile.lastName) // Устанавливаем фамилию
                    edBio.setText(profile.bio) // Устанавливаем биографию
                    edPhone.setText(profile.phone) // Устанавливаем телефон
                    edLocation.setText(profile.location) // Устанавливаем местоположение

                    // Устанавливаем отображаемое имя пользователя
                    tvUserName.text =
                        if (profile.firstName.isNotEmpty() && profile.lastName.isNotEmpty()) {
                            "${profile.firstName} ${profile.lastName}" // Полное имя
                        } else {
                            profile.displayName.ifEmpty { "Пользователь" } // Имя по умолчанию
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Обработка ошибки при загрузке профиля
                Toast.makeText(
                    this@ProfileActivity,
                    "Ошибка загрузки профиля: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun setupListeners() {
        // Устанавливаем слушатели для кнопок
        binding.apply {
            btnEdit.setOnClickListener {
                setEditMode(true) // Включаем режим редактирования
            }

            btnSave.setOnClickListener {
                if (validateFields()) { // Проверяем корректность введенных данных
                    saveProfile() // Сохраняем профиль
                    setEditMode(false) // Выключаем режим редактирования
                }
            }

            btnSignOut.setOnClickListener {
                signOut() // Выход из аккаунта
            }
        }
    }

    private fun validateFields(): Boolean {
        // Проверяем корректность заполнения полей
        binding.apply {
            var isValid = true // Флаг для проверки валидности

            // Проверка поля 'Имя'
            if (edFirstName.text.toString().trim().isEmpty()) {
                edFirstName.error = "Поле 'Имя' обязательно для заполнения" // Устанавливаем ошибку
                isValid = false // Устанавливаем флаг в false
            }

            // Проверка поля 'Телефон'
            if (edPhone.text.toString().trim().isEmpty()) {
                edPhone.error = "Поле 'Телефон' обязательно для заполнения" // Устанавливаем ошибку
                isValid = false // Устанавливаем флаг в false
            }

            // Проверка поля 'Местоположение'
            if (edLocation.text.toString().trim().isEmpty()) {
                edLocation.error =
                    "Поле 'Местоположение' обязательно для заполнения" // Устанавливаем ошибку
                isValid = false // Устанавливаем флаг в false
            }

            return isValid // Возвращаем результат проверки
        }
    }


    private fun saveProfile() {
        // Получаем текущего пользователя из Firebase
        val currentUser = Firebase.auth.currentUser ?: return
        // Заменяем точку в email на подчеркивание для использования в базе данных
        val userEmail = currentUser.email?.replace(".", "_") ?: return

        // Создаем объект профиля пользователя с данными из полей ввода
        val profile = UserProfile(
            firstName = binding.edFirstName.text.toString().trim(),
            lastName = binding.edLastName.text.toString().trim(),
            bio = binding.edBio.text.toString().trim(),
            phone = binding.edPhone.text.toString().trim(),
            location = binding.edLocation.text.toString().trim(),
            email = currentUser.email ?: "",
            displayName = currentUser.displayName ?: ""
        )

        // Проверяем, валиден ли профиль перед сохранением
        if (profile.isValid()) {
            // Сохраняем профиль в базе данных
            database.child(userEmail).setValue(profile)
                .addOnSuccessListener {
                    // Успешное сохранение, показываем сообщение
                    Toast.makeText(this, "Профиль успешно сохранен", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    // Ошибка при сохранении, показываем сообщение с ошибкой
                    Toast.makeText(
                        this,
                        "Ошибка сохранения профиля: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            // Если профиль не валиден, показываем сообщение об ошибке
            Toast.makeText(this, "Заполните все обязательные поля", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signOut() {
        // Выход из текущей сессии пользователя Firebase
        Firebase.auth.signOut()

        // Настраиваем параметры для выхода из Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        // Выход из Google Sign-In
        GoogleSignIn.getClient(this, gso).signOut().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Если выход успешен, переходим на экран входа
                val intent = Intent(this, SignInActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish() // Закрываем текущую активность
            }
        }
    }

    private fun setupSideNavigation() {
        // Получаем ссылку на NavigationView через binding
        val navigationView = binding.navigationView

        // Используем binding для доступа к кнопке меню и DrawerLayout
        binding.menuButton.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // Находим все контейнеры для навигации
        val profileContainer = navigationView.findViewById<LinearLayout>(R.id.profileContainer)
        val chatsContainer = navigationView.findViewById<LinearLayout>(R.id.chatsContainer)
        val settingsContainer = navigationView.findViewById<LinearLayout>(R.id.settingsContainer)
        val signOutContainer = navigationView.findViewById<LinearLayout>(R.id.signOutContainer)

        // Устанавливаем начальное выделение на контейнер профиля
        profileContainer.isSelected = true

        // Настраиваем обработчик нажатия для контейнера чатов
        chatsContainer.setOnClickListener {
            clearSelection(navigationView)
            chatsContainer.isSelected = true
            startActivity(Intent(this, Chats::class.java))
            finish()
        }

        // Настраиваем обработчик нажатия для контейнера настроек
        settingsContainer.setOnClickListener {
            clearSelection(navigationView)
            settingsContainer.isSelected = true
            startActivity(Intent(this, Setting::class.java))
            finish()
        }

        // Настраиваем обработчик нажатия для контейнера выхода
        signOutContainer.setOnClickListener {
            val auth = FirebaseAuth.getInstance()
            auth.signOut()

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            GoogleSignIn.getClient(this, gso).signOut().addOnCompleteListener {
                val intent = Intent(this, SignInActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    private fun clearSelection(navigationView: NavigationView) {
        // Сбрасываем выделение для всех контейнеров навигации
        navigationView.findViewById<LinearLayout>(R.id.profileContainer).isSelected = false
        navigationView.findViewById<LinearLayout>(R.id.chatsContainer).isSelected = false
        navigationView.findViewById<LinearLayout>(R.id.settingsContainer).isSelected = false
    }
}