package pl.edu.pwr.chat.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.edu.pwr.chat.dto.NotificationRequest
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import java.util.UUID

@Service
class NotificationService(
    private val snsClient: SnsClient,
    private val enhancedClient: DynamoDbEnhancedClient,
    @Value("\${aws.sns.topic-arn}") private val topicArn: String
) {
    private val tableName = "notifications"
    private val table = enhancedClient.table(tableName, TableSchema.fromBean(NotificationRequest::class.java))
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
        val notification = NotificationRequest().apply {
            _id = UUID.randomUUID().toString()
            _target = email
            this._message = message
        }
        table.putItem(notification)
    }

    fun sendSmsNotification(phoneNumber: String, message: String) {
        snsClient.publish {
            it.phoneNumber(phoneNumber)
                .message(message)
        }
        val notification = NotificationRequest().apply {
            _id = UUID.randomUUID().toString()
            _target = phoneNumber
            this._message = message
        }

        table.putItem(notification)
    }
}