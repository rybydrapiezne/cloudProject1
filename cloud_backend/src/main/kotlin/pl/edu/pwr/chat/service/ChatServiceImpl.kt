package pl.edu.pwr.chat.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.bind.Bindable.mapOf
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import pl.edu.pwr.chat.dto.MessageRequestTO
import pl.edu.pwr.chat.dto.MessageTO
import pl.edu.pwr.chat.dto.MessagesListTO
import pl.edu.pwr.chat.model.ChatMessage
import pl.edu.pwr.chat.model.UserProfile
import pl.edu.pwr.chat.repository.ChatMessageRepository
import pl.edu.pwr.chat.repository.UserProfileRepository
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.InputStream
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class ChatServiceImpl @Autowired constructor(

        private val chatMessageRepository: ChatMessageRepository,
        private val userProfileRepository: UserProfileRepository,
        private val s3Client: S3Client,



) : ChatService {
    private val userStatuses = ConcurrentHashMap<String, String>()

    private val messageReactions = ConcurrentHashMap<String, MutableMap<String, MutableSet<String>>>()
    private val bucketName = System.getenv("bucket_name")
    override fun getAllEvents(username: String): MessagesListTO {
        val messages = chatMessageRepository.findAll()
        val messageList = messages.map { msg ->
            MessageTO(
                    id = msg.id,
                    username = msg.username,
                    message = msg.message,
                    timestamp = msg.timestamp
            )
        }
        return MessagesListTO(messages = messageList)
    }

    override fun getNewMessages(username: String, after: LocalDateTime): MessagesListTO {
        val messages = chatMessageRepository.findByTimestampAfter(after)
        val messageList = messages.map { msg ->
            MessageTO(
                    id = msg.id,
                    username = msg.username,
                    message = msg.message,
                    timestamp = msg.timestamp
            )
        }
        return MessagesListTO(messages = messageList)
    }

    override fun createLiveEvent(messageDTO: MessageRequestTO) {
        val chatMessage = ChatMessage(
            username = messageDTO.username,
            message = messageDTO.message,
            timestamp = LocalDateTime.now()
        )

        chatMessageRepository.save(chatMessage)
    }

    fun getProfilePicture(username: String): ResponseEntity<Any> {
        return try {
            val listRequest = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix("profile-pictures/$username-")
                .build()

            val listResponse = s3Client.listObjectsV2(listRequest)
            val userFiles = listResponse.contents()

            if (userFiles.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No profile pictures found for $username")
            }

                val latestFile = userFiles.maxByOrNull { it.lastModified() }!!
            val getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(latestFile.key())
                .build()

            val inputStream = s3Client.getObject(getRequest)
            ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(inputStream.readAllBytes())
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Error retrieving profile picture: ${e.message}")
        }
    }

    fun uploadProfilePicture(username: String, fileContent: ByteArray): String {
        val listRequest = ListObjectsV2Request.builder()
            .bucket(bucketName)
            .prefix("profile-pictures/$username-")
            .build()

        s3Client.listObjectsV2(listRequest).contents().forEach { file ->
            s3Client.deleteObject { it.bucket(bucketName).key(file.key()) }
        }

        val key = "profile-pictures/$username-${UUID.randomUUID()}.jpg"
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("image/jpeg")
                .build(),
            RequestBody.fromBytes(fileContent)
        )

        return "https://$bucketName.s3.amazonaws.com/$key"
    }
    override fun updateUserStatus(username: String, status: String) {
        userStatuses[username] = status
    }

    override fun getUserStatus(username: String): String {
        return userStatuses[username] ?: "offline"
    }

    override fun getOnlineUsers(): List<String> {
        return userStatuses.filter { it.value == "online" }.keys.toList()
    }

    override fun addReaction(messageId: String, username: String, reaction: String) {
        val reactions = messageReactions.getOrPut(messageId) { ConcurrentHashMap() }
        val users = reactions.getOrPut(reaction) { ConcurrentHashMap.newKeySet() }
        users.add(username)
    }

    override fun removeReaction(messageId: String, username: String, reaction: String) {
        messageReactions[messageId]?.get(reaction)?.remove(username)
    }

    override fun getMessageReactions(messageId: String): Map<String, Any> {
        return messageReactions[messageId]?.mapValues {
            mapOf(
                    "count" to it.value.size,
                    "users" to it.value.toList()
            )
        } ?: emptyMap()
    }

}
