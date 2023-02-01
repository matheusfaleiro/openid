package com.adein.oauthreference.data.net

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.adein.oauthreference.BuildConfig
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import net.openid.appauth.*
import org.json.JSONException


object OAuthClient {
    private const val PREF_AUTH_FILE = "auth"
    private const val PREF_KEY_AUTHSTATE_JSON = "stateJson"
    private const val TAG = "OAuthClient"

    private const val scopes = BuildConfig.OAUTH_SCOPES

    private val authEndPoint: Uri by lazy {
        Uri.parse(BuildConfig.OAUTH_AUTHORIZATION_ENDPOINT_URI)
    }

    private val tokenEndPoint: Uri by lazy {
        Uri.parse(BuildConfig.OAUTH_TOKEN_ENDPOINT_URI)
    }

    private val endSessionEndPoint: Uri by lazy {
        Uri.parse(BuildConfig.OAUTH_END_SESSION_URI)
    }

    private val redirectUri: Uri by lazy {
        Uri.parse(BuildConfig.OAUTH_REDIRECT_URI)
    }

    private val serviceConfig: AuthorizationServiceConfiguration by lazy {
        AuthorizationServiceConfiguration(authEndPoint, tokenEndPoint, null, endSessionEndPoint)
    }

    @Volatile
    private var authState: AuthState? = null

    // Start login process by using the library to open the login URI
    fun login(context: Context): Maybe<Intent> {
        return Maybe.create { e ->
            Log.d(TAG, "login")
            // Seed an AuthState instance from configuration (to help with persistence)
            authState = AuthState(serviceConfig)

            // Construct an AuthorizationRequest
            val authRequestBuilder = AuthorizationRequest.Builder(
                serviceConfig,
                BuildConfig.OAUTH_CLIENT_ID,
                ResponseTypeValues.CODE,
                redirectUri
            )
            val authRequest = authRequestBuilder.setScope(scopes).build()

            // Create an Intent to obtain an authorization code
            // This Intent will open a custom (chrome) tab with the login URI and settings
            val authService = AuthorizationService(context)
            val authIntent = authService.getAuthorizationRequestIntent(authRequest)
            e.onSuccess(authIntent)
        }
    }

    // Start logout process by using the library to open the logout URI
    fun logout(context: Context): Maybe<Intent> {
        return Maybe.create { e ->
            Log.d(TAG, "logout")
            authState?.let { state ->
                // Build EndSessionRequest
                val config = state.authorizationServiceConfiguration ?: serviceConfig
                val endSessionRequest = EndSessionRequest.Builder(config)
                    .setIdTokenHint(state.idToken)
                    .setPostLogoutRedirectUri(redirectUri)
                    .build()

                // Create an Intent to request logout
                // This Intent will open a custom (chrome) tab with the logout URI and settings
                val authService = AuthorizationService(context)
                val endSessionIntent = authService.getEndSessionRequestIntent(endSessionRequest)
                e.onSuccess(endSessionIntent)
            } ?: run {
                e.onComplete()
            }
        }
    }

    // Request tokens after successful login (using authorization code)
    fun getToken(context: Context, response: AuthorizationResponse?): Completable {
        return Completable.create { e ->
            Log.d(TAG, "getToken")
            val authService = AuthorizationService(context)
            val tokenRequest =
                response?.createTokenExchangeRequest() ?: authState?.createTokenRefreshRequest()
            if (tokenRequest != null) {
                authService.performTokenRequest(tokenRequest) { resp, ex ->
                    // Update authState with token result
                    authState?.update(resp, ex)
                    if (resp != null) {
                        // exchange succeeded
                        Log.d(TAG, "getToken: success")
                        e.onComplete()
                    } else {
                        // Authorization failed
                        Log.d(TAG, "getToken: error ${ex?.localizedMessage}")
                        if (ex != null) {
                            e.onError(ex)
                        }
                    }
                }
            } else {
                e.onError(Exception("TokenRequest is null"))
            }
        }
    }

    // Use access token to call an API
    fun useToken(context: Context): Completable {
        return Completable.create { e ->
            Log.d(TAG, "useToken")
            val authService = AuthorizationService(context)
            // Check and obtain fresh tokens if needed
            // Then perform the API call
            authState?.performActionWithFreshTokens(authService) { _, _, ex ->
                if (ex != null) {
                    // negotiation for fresh tokens failed
                    Log.d(TAG, "useToken: error ${ex.localizedMessage}")
                    e.onError(ex)
                }

                // Tokens are known to be valid
                // TODO: Call an API using tokens
                Log.d(TAG, "useToken: success")
                e.onComplete()
            }
        }
    }

    // Update authState with results from login
    fun updateAuthStateFromLogin(
        response: AuthorizationResponse?,
        exception: AuthorizationException?
    ) {
        Log.d(TAG, "updateAuthStateFromLogin")
        if (authState != null) {
            authState?.update(response, exception)
        } else {
            authState = AuthState(response, exception)
        }
    }

    // Persist the AuthState as JSON in SharedPreferences
    fun saveAuthState(context: Context): Single<Boolean> {
        return Single.create { e ->
            Log.d(TAG, "saveAuthState")
            if (authState != null) {
                val authPrefs: SharedPreferences =
                    context.getSharedPreferences(PREF_AUTH_FILE, MODE_PRIVATE)
                authPrefs.edit()
                    .putString(PREF_KEY_AUTHSTATE_JSON, authState?.jsonSerializeString())
                    .apply()
                e.onSuccess(true)
            }
            e.onSuccess(false)
        }
    }

    // Read AuthState from SharedPreferences
    fun readAuthState(context: Context): Single<Boolean> {
        return Single.create { e ->
            Log.d(TAG, "readAuthState")
            val authPrefs: SharedPreferences =
                context.getSharedPreferences(PREF_AUTH_FILE, MODE_PRIVATE)
            val stateJson = authPrefs.getString(PREF_KEY_AUTHSTATE_JSON, null)
            try {
                stateJson?.let {
                    authState = AuthState.jsonDeserialize(stateJson)
                    Log.d(TAG, "readAuthState: success")
                    e.onSuccess(true)
                } ?: run {
                    authState = null
                    Log.d(TAG, "readAuthState: fail")
                    e.onSuccess(false)
                }
            } catch (ex: JSONException) {
                authState = null
                Log.d(TAG, "readAuthState: error ${ex.localizedMessage}")
                e.onError(ex)
            }
        }
    }

    // Clear any AuthState from SharedPreferences
    fun clearAuthState(context: Context): Single<Boolean> {
        return Single.create { e ->
            Log.d(TAG, "clearAuthState")
            authState = null
            val authPrefs: SharedPreferences =
                context.getSharedPreferences(PREF_AUTH_FILE, MODE_PRIVATE)
            authPrefs.edit()
                .remove(PREF_KEY_AUTHSTATE_JSON)
                .apply()
            e.onSuccess(true)
        }
    }
}