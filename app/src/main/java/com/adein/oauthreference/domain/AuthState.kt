package com.adein.oauthreference.domain

// Possible states for authentication
sealed class AuthState {
    object LoadingState : AuthState()
    object UnauthorizedState : AuthState()
    object AuthorizedState : AuthState()
    data class LoginFailedState(val message: String?) : AuthState()
    data class LogoutFailedState(val message: String?) : AuthState()
    data class ErrorState(val error: Throwable?) : AuthState()
}
