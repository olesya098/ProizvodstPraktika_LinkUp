package com.hfad.linkup


import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.hfad.linkup.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Получаем текущего пользователя
        val currentUser = Firebase.auth.currentUser

        // Устанавливаем имя пользователя
        binding.tvUserName.text = currentUser?.displayName ?: "Пользователь"

        // Устанавливаем email
        binding.tvUserEmail.text = currentUser?.email ?: "email не указан"

        // Обработка нижней панели навигации
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        // Обработчик для кнопки профиля (текущая страница)
        binding.bottomNavigation.bProfile.setOnClickListener {
            // Текущая страница - профиль, ничего не делаем
        }

        // Обработчик для кнопки чатов
        binding.bottomNavigation.bChats.setOnClickListener {
            val intent = Intent(this, Chats::class.java)
            startActivity(intent)
            finish() // Закрываем текущую активность
        }

        // Обработчик для кнопки настроек
        binding.bottomNavigation.bSettings.setOnClickListener {
            // Здесь позже можно реализовать переход на страницу настроек
            // Пока что оставим заглушку
        }
        binding.bottomNavigation.bProfile .isSelected = true
    }
}
