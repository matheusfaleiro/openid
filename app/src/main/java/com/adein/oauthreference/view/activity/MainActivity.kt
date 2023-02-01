package com.adein.oauthreference.view.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.adein.oauthreference.R
import com.adein.oauthreference.data.AuthInteractor
import com.adein.oauthreference.data.AuthInteractor.Companion.RC_OAUTH_LOGIN
import com.adein.oauthreference.data.model.AuthorizationResult
import com.adein.oauthreference.databinding.ActivityMainBinding
import com.adein.oauthreference.domain.AuthState
import com.adein.oauthreference.presenter.MainPresenter
import com.adein.oauthreference.view.MainView
import com.adein.oauthreference.view.snack
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse

class MainActivity : AppCompatActivity(), MainView {
    private lateinit var binding: ActivityMainBinding
    private lateinit var presenter: MainPresenter

    // Observable to publish the result of the login activity (custom tab with login URI)
    private val loginResponseSubject: PublishSubject<AuthorizationResult> = PublishSubject.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContentView(R.layout.activity_main)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        // Create and setup the Presenter with the Interactor
        presenter = MainPresenter(AuthInteractor(this))
        presenter.create(this)
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
        // Binds the Presenter to the newly created View
        presenter.bind(this)
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
        // Unbinds the Presenter from the View
        presenter.unbind()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        // Prepare presenter for destruction
        presenter.destroy()
        super.onDestroy()
    }

    // Map the given (Auth)State to various UI states
    override fun render(state: AuthState) {
        Log.d(TAG, "render: $state")
        when (state) {
            is AuthState.LoadingState -> renderLoadingState()
            is AuthState.UnauthorizedState -> renderUnauthorizedState()
            is AuthState.AuthorizedState -> renderAuthorizedState()
            is AuthState.LoginFailedState -> renderLoginFailedState(state.message)
            is AuthState.LogoutFailedState -> renderLogoutFailedState(state.message)
            is AuthState.ErrorState -> renderErrorState(state.error)
        }
    }

    private fun renderLoadingState() {
        binding.progressBar.visibility = View.VISIBLE
        binding.buttonLogin.visibility = View.GONE
    }

    private fun renderUnauthorizedState() {
        binding.progressBar.visibility = View.GONE
        binding.buttonLogin.visibility = View.VISIBLE
    }

    private fun renderAuthorizedState() {
        binding.progressBar.visibility = View.GONE
        binding.buttonLogin.visibility = View.GONE
        showAuthorizedActivity()
    }

    private fun showAuthorizedActivity() =
        startActivity(Intent(this, AuthorizedActivity::class.java))

    private fun renderLoginFailedState(message: String?) {
        binding.progressBar.visibility = View.GONE
        binding.buttonLogin.visibility = View.VISIBLE
        binding.mainLayout.snack("Login failed: $message")
    }

    private fun renderLogoutFailedState(message: String?) {
        binding.progressBar.visibility = View.GONE
        binding.buttonLogin.visibility = View.VISIBLE
        binding.mainLayout.snack("Logout failed: $message")
    }

    private fun renderErrorState(ex: Throwable?) {
        binding.progressBar.visibility = View.GONE
        binding.buttonLogin.visibility = View.VISIBLE
        binding.mainLayout.snack("Error: ${ex?.localizedMessage ?: ex.toString()}")
    }

    // Returns an observable that emits every time the login button is pressed
    override fun loginIntent(): Observable<Unit> {
        return Observable.create { e ->
            binding.buttonLogin.setOnClickListener { view ->
                e.setCancellable {
                    view.setOnClickListener(null)
                }
                e.onNext(Unit)
            }
        }
    }

    // Returns an observable that emits the (Authorization)Result from the custom tab login activity
    // This allows the presenter to subscribe to the result of the login custom tab
    override fun loginResponseIntent(): Observable<AuthorizationResult> {
        return loginResponseSubject
            .subscribeOn(AndroidSchedulers.mainThread())
    }

    // Processes the results from the login custom (chrome) tab
    @Deprecated("Deprecated")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // For OAuth login result code
        if (requestCode == RC_OAUTH_LOGIN) {
            // Extract the data from the (Android) Intent, and emit the result
            if (data != null) {
                val authorizationResponse = AuthorizationResponse.fromIntent(data)
                val authorizationException = AuthorizationException.fromIntent(data)
                if (authorizationResponse != null || authorizationException != null) {
                    val authResult =
                        AuthorizationResult(authorizationResponse, authorizationException)
                    Log.d(TAG, "Authorization result: $authResult")
                    loginResponseSubject.onNext(authResult)
                }
            } else {
                Log.d(TAG, "No data in login launcher")
                loginResponseSubject.onNext(AuthorizationResult(null, null))
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}