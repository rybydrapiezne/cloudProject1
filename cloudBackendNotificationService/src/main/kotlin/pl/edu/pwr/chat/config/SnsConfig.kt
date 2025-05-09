package pl.edu.pwr.chat.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient

@Configuration
class SnsConfig(
    @Value("\${aws.region}") private val region: String,
    @Value("\${aws.accessKeyId}")
    private var accessKeyId: String,
    @Value("\${aws.secretAccessKey}")
    private var secretAccessKey: String,
    @Value("\${aws.sessionId}")
    private var sessionId: String

) {
    @Bean
    fun snsClient(): SnsClient {
        val credentials = AwsSessionCredentials.create(accessKeyId, secretAccessKey,sessionId)
        return SnsClient.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build()
    }
}