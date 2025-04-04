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
}