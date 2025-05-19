package pl.edu.pwr.chat.controller

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pl.edu.pwr.chat.dto.NotificationRequest
import pl.edu.pwr.chat.service.NotificationService

@RestController
@RequestMapping("/notifications")
class   NotificationController(
    private val notificationService: NotificationService
) {
    @PostMapping("/email")
    fun sendEmailNotification(@RequestBody request: NotificationRequest) {
        notificationService.sendEmailNotification(request._target, request._message)
    }

    @PostMapping("/sms")
    fun sendSmsNotification(@RequestBody request: NotificationRequest) {
        notificationService.sendSmsNotification(request._target, request._message)
    }
}