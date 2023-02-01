package com.adein.oauthreference.view

import com.adein.oauthreference.domain.AuthState
import io.reactivex.rxjava3.core.Observable

// Abstraction for the Authorized view
interface AuthorizedView {
    fun render(state: AuthState)
    fun useTokenIntent(): Observable<Unit>
    fun logoutIntent(): Observable<Unit>
    fun logoutResponseIntent(): Observable<Int>
}