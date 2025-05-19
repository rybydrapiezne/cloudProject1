package pl.edu.pwr.chat

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient

@SpringBootApplication(exclude = [
	org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration::class,
	org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration::class
])
@EnableDiscoveryClient
class AuthService

fun main(args: Array<String>) {
	runApplication<AuthService>(*args)
}
