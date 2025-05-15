package com.hfad.chattest1
//Данные профиля
data class UserProfile(
    val firstName: String = "",
    val lastName: String = "",
    val bio: String = "",
    val phone: String = "",
    val location: String = "",
    val email: String = "",
    val displayName: String = ""
) {
    fun isValid(): Boolean {
        return firstName.isNotBlank() && phone.isNotBlank() && location.isNotBlank()
    }
}