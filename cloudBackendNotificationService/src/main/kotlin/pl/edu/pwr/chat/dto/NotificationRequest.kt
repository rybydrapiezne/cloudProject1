package pl.edu.pwr.chat.dto
data class NotificationRequest(
    val target: String,  // email lub numer telefonu
    val message: String
)