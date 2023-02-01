package com.adein.oauthreference.view

import com.adein.oauthreference.data.model.AuthorizationResult
import com.adein.oauthreference.domain.AuthState
import io.reactivex.rxjava3.core.Observable

// Abstraction for the Main view
interface MainView {
    fun render(state: AuthState)
    fun loginIntent(): Observable<Unit>
    fun loginResponseIntent(): Observable<AuthorizationResult>
}