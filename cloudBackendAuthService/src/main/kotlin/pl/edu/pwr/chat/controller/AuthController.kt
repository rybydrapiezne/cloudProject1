package pl.edu.pwr.chat.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pl.edu.pwr.chat.dto.AuthResponse
import pl.edu.pwr.chat.dto.LoginRequest
import pl.edu.pwr.chat.dto.RegisterRequest
import pl.edu.pwr.chat.service.CognitoService

@RestController @RequestMapping("/api/auth") class AuthController(private val cognitoService: CognitoService) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<String> {
        return try {
            cognitoService.registerUser(request)
            ResponseEntity.ok("User registered successfully")
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.message)
        }
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        return try {
            val response = cognitoService.loginUser(request)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                AuthResponse("", "", null, 0)
            )
        }
    }

}