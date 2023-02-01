package com.adein.oauthreference.data

import com.adein.oauthreference.data.model.AuthorizationResult
import com.adein.oauthreference.domain.AuthState
import io.reactivex.rxjava3.core.Observable

// Abstraction for the Auth interactor
interface AuthorizationInteractor {
    fun loadPreviousState(): Observable<AuthState>
    fun login(): Observable<AuthState>
    fun handleLogin(result: AuthorizationResult): Observable<AuthState>
    fun handleLogout(resultCode: Int): Observable<AuthState>
    fun useTokens(): Observable<AuthState>
    fun logout(): Observable<AuthState>
}