package pl.edu.pwr.chat.service

import org.springframework.http.ResponseEntity

interface ChatService {
    fun updateUserStatus(username: String, status: String)
    fun getUserStatus(username: String): String
    fun getOnlineUsers(): List<String>
    fun getProfilePicture(username: String): ResponseEntity<Any>
    fun uploadProfilePicture(username: String, fileContent: ByteArray): String
}