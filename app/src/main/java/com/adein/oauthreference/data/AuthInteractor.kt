package com.adein.oauthreference.data

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.adein.oauthreference.data.model.AuthorizationResult
import com.adein.oauthreference.domain.AuthState
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers

// Contains Use Cases/Business Logic
// Converts actions and events into new (Auth)State's
class AuthInteractor(
    private val context: Context,
) : AuthorizationInteractor {

    private val loginClient = com.adein.oauthreference.data.net.OAuthClient

    // Load any stored AuthState and emit AuthorizedState on success
    override fun loadPreviousState(): Observable<AuthState> {
        return loginClient.readAuthState(context)
            .subscribeOn(AndroidSchedulers.mainThread())
            .map { success ->
                if (success) {
                    AuthState.AuthorizedState
                } else {
                    AuthState.UnauthorizedState
                }
            }
            .onErrorReturn { AuthState.ErrorState(it) }
            .toObservable()
    }

    // Create the login intent and then start the activity (custom tab to login URI)
    // Emits LoadingState (temporarily)
    override fun login(): Observable<AuthState> {
        return loginClient.login(context)
            .subscribeOn(Schedulers.io())
            .toObservable()
            .doOnNext { intent ->
                (context as? Activity)?.startActivityForResult(intent, RC_OAUTH_LOGIN)
            }
            .map { AuthState.LoadingState }
    }

    // Uses the AuthorizationResult (authorizationCode) from login to obtain (access/refresh) tokens
    // Updates stored authState
    // Emits AuthorizedState on success
    override fun handleLogin(result: AuthorizationResult): Observable<AuthState> {
        loginClient.updateAuthStateFromLogin(result.response, result.exception)
        return if (result.response != null) {
            loginClient.getToken(context, result.response)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .andThen(loginClient.saveAuthState(context))
                .map { success ->
                    if (success) {
                        AuthState.AuthorizedState
                    } else {
                        AuthState.LoginFailedState("Failed to save auth state")
                    }
                }
                .onErrorReturn { AuthState.ErrorState(it) }
                .toObservable()
        } else {
            Observable.just(
                AuthState.LoginFailedState(
                    result.exception?.localizedMessage ?: "Unknown error"
                )
            )
        }
    }

    // Uses the (access) token (and refresh if needed/present) to call an example API endpoint
    override fun useTokens(): Observable<AuthState> {
        return Observable.fromCompletable<AuthState>(loginClient.useToken(context))
            .subscribeOn(Schedulers.io())
            .onErrorReturn { AuthState.ErrorState(it) }
    }

    // Create the logout Intent and then start the Activity (custom tab to logout URI)
    // Emits LoadingState (temporarily)
    override fun logout(): Observable<AuthState> {
        return loginClient.logout(context)
            .subscribeOn(Schedulers.io())
            .toObservable()
            .doOnNext { intent ->
                (context as? Activity)?.startActivityForResult(intent, RC_OAUTH_LOGOUT)
            }
            .map { AuthState.LoadingState }
    }

    // Verifies the result from logout is OK, clears stored AuthState, and emits UnauthorizedState
    override fun handleLogout(resultCode: Int): Observable<AuthState> {
        return if (resultCode == AppCompatActivity.RESULT_OK) {
            loginClient.clearAuthState(context)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .toObservable()
                .map { AuthState.UnauthorizedState }
        } else {
            Observable.just(AuthState.LogoutFailedState("Unknown error"))
        }
    }

    companion object {
        private const val TAG = "AuthInteractor"
        const val RC_OAUTH_LOGIN = 9998
        const val RC_OAUTH_LOGOUT = 9999
    }
}