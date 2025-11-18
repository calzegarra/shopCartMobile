package com.example.shopcart.auth

import java.io.Serializable

data class Role(
    val id: Int? = null,
    val description: String? = null
) : Serializable

data class User(
    val id: Int? = null,
    val name: String? = null,
    val lastname: String? = null,
    val dni: String? = null,
    val address: String? = null,
    val email: String? = null,
    val username: String? = null,
    val password: String? = null,
    val role: Role? = null,
    val avatar: String? = null
) : Serializable

data class ResponseData(
    val data: User? = null,
    val status: Boolean? = null,
    val message: String? = null,
    val code: Int? = null
) : Serializable

data class AuthRequest(
    val username: String,
    val password: String
)

object ApiConfig {
    // 10.0.2.2 apunta al host cuando se usa el emulador de Android.
    const val BASE_URL = "http://10.0.2.2:8080"
    const val LOGIN_PATH = "/api/auth/profile"
    const val LOGIN_URL = BASE_URL + LOGIN_PATH
    const val CATALOG_PATH = "/api/videogame/findCatalog"
    const val CATALOG_URL = BASE_URL + CATALOG_PATH
    const val USER_UPDATE_PATH = "/api/user/update"
    const val USER_UPDATE_URL = BASE_URL + USER_UPDATE_PATH
    const val GAME_DETAIL_PATH = "/api/videogame/findById/"
    const val GAME_DETAIL_URL = BASE_URL + GAME_DETAIL_PATH
}
