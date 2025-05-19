package pl.edu.pwr.chat.dto

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import java.util.UUID


@DynamoDbBean
data class NotificationRequest(
    var _id: String = "",
    var _target: String = "",
    var _message: String = ""
) {
    @DynamoDbPartitionKey
    fun getId(): String = _id
    fun setId(id: String) { this._id = id }

    fun getTarget(): String = _target
    fun setTarget(target: String) { this._target = target }

    fun getMessage(): String = _message
    fun setMessage(message: String) { this._message = message }

}