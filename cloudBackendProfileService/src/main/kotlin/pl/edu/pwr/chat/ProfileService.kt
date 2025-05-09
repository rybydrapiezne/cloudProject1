package pl.edu.pwr.chat

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ProfileService

fun main(args: Array<String>) {
	runApplication<ProfileService>(*args)
}
