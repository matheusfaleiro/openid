package com.adein.oauthreference.data.model

import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse

// Holds the result from the login attempt in the OpenID (Chrome) Custom Tab
data class AuthorizationResult(
    val response: AuthorizationResponse?,
    val exception: AuthorizationException?
)
