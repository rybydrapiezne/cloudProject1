package pl.edu.pwr.chat.dto

import java.time.LocalDateTime
import java.util.*

data class MessageTO(
        val id: Long,
        val username: String,
        val message: String,
        val timestamp:LocalDateTime
)
