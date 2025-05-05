package pl.edu.pwr.chat.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import pl.edu.pwr.chat.service.ChatServiceImpl
import java.io.InputStream

@RestController
@RequestMapping("/profile")
class ProfileController @Autowired constructor(
    private val chatService: ChatServiceImpl
) {
    @PostMapping("/upload")
    fun uploadProfilePicture(
        @RequestParam("username") username: String,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<String> {
        val result = chatService.uploadProfilePicture(username, file.bytes)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{username}/image")
    fun getProfilePicture(@PathVariable username: String): ResponseEntity<Any> {
        return chatService.getProfilePicture(username)
    }
    @PostMapping("/{username}/status")
    fun updateUserStatus(
            @PathVariable username: String,
            @RequestBody statusRequest: Map<String, String>
    ): ResponseEntity<String> {
        val status = statusRequest["status"] ?: throw IllegalArgumentException("Status is required")
        chatService.updateUserStatus(username, status)
        return ResponseEntity.ok("Status updated")
    }

    @GetMapping("/{username}/status")
    fun getUserStatus(@PathVariable username: String): ResponseEntity<String> {
        val status = chatService.getUserStatus(username)
        return ResponseEntity.ok(status)
    }

    @GetMapping("/online-users")
    fun getOnlineUsers(): ResponseEntity<List<String>> {
        val onlineUsers = chatService.getOnlineUsers()
        return ResponseEntity.ok(onlineUsers)
    }
}