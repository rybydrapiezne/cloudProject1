package pl.edu.pwr.chat

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.sql.DriverManager.println

@SpringBootApplication
class ChatAppApplication

fun main(args: Array<String>) {
	runApplication<ChatAppApplication>(*args)
}
