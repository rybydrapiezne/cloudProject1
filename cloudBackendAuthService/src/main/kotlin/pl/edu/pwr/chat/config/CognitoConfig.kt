package pl.edu.pwr.chat.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.regions.Region
@Configuration
class CognitoConfig{
    @Value("\${aws.region}")
    private lateinit var region: String

    @Bean
    fun cognitoClient(): CognitoIdentityProviderClient {
        return CognitoIdentityProviderClient.builder()
            .region(Region.of(region))
            .credentialsProvider(ProfileCredentialsProvider.create())
            .build()
    }

    @Value("\${aws.cognito.userPoolId}")
    private lateinit var userPoolId: String

    @Bean
    fun reactiveJwtDecoder(): ReactiveJwtDecoder {
        val issuerUri = "https://cognito-idp.${region}.amazonaws.com/${userPoolId}"
        val jwkSetUri = "$issuerUri/.well-known/jwks.json"
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build()
    }
}