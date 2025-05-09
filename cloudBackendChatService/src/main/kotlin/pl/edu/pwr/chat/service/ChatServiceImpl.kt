package pl.edu.pwr.chat.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import pl.edu.pwr.chat.dto.MessageRequestTO
import pl.edu.pwr.chat.dto.MessageTO
import pl.edu.pwr.chat.dto.MessagesListTO
import pl.edu.pwr.chat.model.ChatMessage
import pl.edu.pwr.chat.repository.ChatMessageRepository
import kotlin.collections.*
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Service
class ChatServiceImpl @Autowired constructor(

        private val chatMessageRepository: ChatMessageRepository,



) : ChatService {
    private val userStatuses = ConcurrentHashMap<String, String>()

    private val messageReactions = ConcurrentHashMap<String, MutableMap<String, MutableSet<String>>>()
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
