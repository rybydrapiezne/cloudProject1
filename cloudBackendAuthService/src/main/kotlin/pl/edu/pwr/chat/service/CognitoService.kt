package pl.edu.pwr.chat.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import pl.edu.pwr.chat.dto.AuthResponse
import pl.edu.pwr.chat.dto.LoginRequest
import pl.edu.pwr.chat.dto.RegisterRequest
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.*

@Service class CognitoService( private val cognitoClient: CognitoIdentityProviderClient, @Value("\${aws.cognito.userPoolId}") private val userPoolId: String, @Value("\${aws.cognito.clientId}") private val clientId: String ) {

    fun registerUser(request: RegisterRequest) {
        val signUpRequest = SignUpRequest.builder()
            .clientId(clientId)
            .username(request.username)
            .password(request.password)
            .userAttributes(
                AttributeType.builder().name("email").value(request.email).build()
            )
            .build()

        cognitoClient.signUp(signUpRequest)
    }

    fun loginUser(request: LoginRequest): AuthResponse {
        val authRequest = InitiateAuthRequest.builder()
            .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
            .clientId(clientId)
            .authParameters(
                mapOf(
                    "USERNAME" to request.username,
                    "PASSWORD" to request.password
                )
            )
            .build()

        val authResponse = cognitoClient.initiateAuth(authRequest)
        val authResult = authResponse.authenticationResult()

        return AuthResponse(
            accessToken = authResult.accessToken(),
            idToken = authResult.idToken(),
            refreshToken = authResult.refreshToken(),
            expiresIn = authResult.expiresIn()
        )
    }

}