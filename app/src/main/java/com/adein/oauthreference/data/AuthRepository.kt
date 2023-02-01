package com.adein.oauthreference.data

import com.adein.oauthreference.domain.AuthState

// Very simple repository (singleton) of the authentication state
// This persists the (Auth)State outside of the View/Presenter/etc
object AuthRepository {
    @Volatile
    var state: AuthState = AuthState.UnauthorizedState
}