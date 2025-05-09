package pl.edu.pwr.chat.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.MessageAttributeValue

@Service
class NotificationService(
    private val snsClient: SnsClient,
    @Value("\${aws.sns.topic-arn}") private val topicArn: String
) {
    fun sendEmailNotification(email: String, message: String) {
        snsClient.publish {
            it.topicArn(topicArn)
                .message(message)
                .subject("Nowa wiadomość w czacie")
                .messageAttributes(
                    mapOf(
                        "email" to MessageAttributeValue.builder()
                            .dataType("String")
                            .stringValue(email)
                            .build()
                    )
                )
        }
    }

    fun sendSmsNotification(phoneNumber: String, message: String) {
        snsClient.publish {
            it.phoneNumber(phoneNumber)
                .message(message)
        }
    }
}