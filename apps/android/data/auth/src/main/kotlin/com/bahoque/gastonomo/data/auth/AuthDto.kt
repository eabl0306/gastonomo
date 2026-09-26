package com.bahoque.gastonomo.data.auth

import com.bahoque.gastonomo.core.domain.auth.Session
import com.bahoque.gastonomo.core.domain.user.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SessionDto(
    @SerialName("accessToken") val accessToken: String,
    @SerialName("expiresIn") val expiresIn: Long,
    @SerialName("refreshToken") val refreshToken: String,
    @SerialName("user") val user: UserDto,
) {
    fun toDomain(): Session = Session(
        accessToken = accessToken,
        expiresInSeconds = expiresIn,
        refreshToken = refreshToken,
        user = user.toDomain(),
    )
}

@Serializable
internal data class UserDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("email") val email: String,
) {
    fun toDomain(): User = User(id = id, name = name, email = email)
}

@Serializable
internal data class RegisterRequestDto(
    @SerialName("name") val name: String,
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
    @SerialName("acceptedTerms") val acceptedTerms: Boolean,
)

@Serializable
internal data class LoginRequestDto(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
)

@Serializable
internal data class LogoutRequestDto(@SerialName("refreshToken") val refreshToken: String)

@Serializable
internal data class ForgotPasswordRequestDto(@SerialName("email") val email: String)

@Serializable
internal data class ChangePasswordRequestDto(
    @SerialName("currentPassword") val currentPassword: String,
    @SerialName("newPassword") val newPassword: String,
    @SerialName("refreshToken") val refreshToken: String?,
)
