package pl.edu.pwr.chat.dto
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest( @field:NotBlank(message = "Username is required") val username: String,

                            @field:NotBlank(message = "Password is required")
                            @field:Size(min = 8, message = "Password must be at least 8 characters")
                            val password: String,

                            @field:NotBlank(message = "Email is required")
                            @field:Email(message = "Invalid email format")
                            val email: String)