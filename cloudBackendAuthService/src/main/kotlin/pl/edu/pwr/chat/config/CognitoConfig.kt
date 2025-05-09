package pl.edu.pwr.chat.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.regions.Region
@Configuration
class CognitoConfig{
    @Value("\${aws.accessKeyId}")
    private lateinit var accessKeyId: String

    @Value("\${aws.secretAccessKey}")
    private lateinit var secretAccessKey: String

    @Value("\${aws.region}")
    private lateinit var region: String

    @Bean
    fun cognitoClient(): CognitoIdentityProviderClient {
        val credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey)
        return CognitoIdentityProviderClient.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build()
    }
    @Value("\${aws.cognito.userPoolId}")
    private lateinit var userPoolId: String

    @Bean
    fun jwtDecoder(): JwtDecoder {
        val issuerUrl = "https://cognito-idp.${region}.amazonaws.com/${userPoolId}"
        return NimbusJwtDecoder.withJwkSetUri("$issuerUrl/.well-known/jwks.json").build()
    }
}