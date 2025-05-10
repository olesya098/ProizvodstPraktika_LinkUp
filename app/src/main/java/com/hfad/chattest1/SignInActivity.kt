package com.hfad.chattest1

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.hfad.chattest1.databinding.ActivitySignInBinding
class SignInActivity : AppCompatActivity() {
    // Объявление необходимых свойств
    lateinit var launcher: ActivityResultLauncher<Intent>  // Лаунчер для запуска авторизации через Google
    lateinit var auth: FirebaseAuth                       // Объект аутентификации Firebase
    lateinit var binding: ActivitySignInBinding           // Объект привязки к разметке

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in)

        // Настройка привязки к разметке
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализация Firebase Auth
        auth = Firebase.auth

        // Настройка лаунчера для обработки результата входа через Google
        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if(account != null){
                    firebaseAuthWithGoogle(account.idToken!!)
                }
            } catch (e: ApiException){
                Log.d("MyLog","Api exception")
            }
        }

        // Настройка кнопки входа
        binding.bSignIn.setOnClickListener {
            signInWithGoogle()
        }

        // Проверка состояния аутентификации
        checkAuthState()
    }

    // Получение клиента Google Sign In
    private fun getClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(this, gso)
    }

    // Запуск процесса входа через Google
    private fun signInWithGoogle(){
        val signInClient = getClient()
        launcher.launch(signInClient.signInIntent)
    }

    // Аутентификация в Firebase с помощью токена Google
    private fun firebaseAuthWithGoogle(idToken: String){
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener {
            if(it.isSuccessful){
                Log.d("MyLog","Google signIn done")
                checkAuthState()
            } else {
                Log.d("MyLog","Google signIn error")
            }
        }
    }

    // Проверка состояния аутентификации и переход к профилю при успешном входе
    private fun checkAuthState(){
        if(auth.currentUser != null){
            val i = Intent(this, ProfileActivity::class.java)
            startActivity(i)
        }
    }
}