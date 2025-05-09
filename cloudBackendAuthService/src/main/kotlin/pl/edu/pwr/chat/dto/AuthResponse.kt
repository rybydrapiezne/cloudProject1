package pl.edu.pwr.chat.dto

data class AuthResponse( val accessToken: String, val idToken: String, val refreshToken: String?, val expiresIn: Int )