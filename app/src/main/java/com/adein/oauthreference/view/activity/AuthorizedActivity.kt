package com.adein.oauthreference.view.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.adein.oauthreference.R
import com.adein.oauthreference.data.AuthInteractor
import com.adein.oauthreference.databinding.ActivityAuthorizedBinding
import com.adein.oauthreference.domain.AuthState
import com.adein.oauthreference.presenter.AuthorizedPresenter
import com.adein.oauthreference.view.AuthorizedView
import com.adein.oauthreference.view.snack
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject


class AuthorizedActivity : AppCompatActivity(), AuthorizedView {
    private lateinit var binding: ActivityAuthorizedBinding
    private lateinit var presenter: AuthorizedPresenter

    // Observable to publish the result of the logout activity (custom tab with logout URI)
    private val logoutResponseSubject: PublishSubject<Int> = PublishSubject.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authorized)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_authorized)

        // Create and setup the Presenter with the Interactor
        presenter = AuthorizedPresenter(AuthInteractor(this))
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
        binding.buttonUseToken.visibility = View.GONE
        binding.buttonLogout.visibility = View.GONE
    }

    private fun renderAuthorizedState() {
        binding.progressBar.visibility = View.GONE
        binding.buttonUseToken.visibility = View.VISIBLE
        binding.buttonLogout.visibility = View.VISIBLE
    }

    private fun renderUnauthorizedState() {
        binding.progressBar.visibility = View.VISIBLE
        binding.buttonUseToken.visibility = View.GONE
        binding.buttonLogout.visibility = View.GONE
        finish()
    }

    private fun renderLoginFailedState(message: String?) {
        binding.progressBar.visibility = View.GONE
        binding.buttonUseToken.visibility = View.VISIBLE
        binding.buttonLogout.visibility = View.VISIBLE
        binding.authorizedLayout.snack("Login failed: $message")
    }

    private fun renderLogoutFailedState(message: String?) {
        binding.progressBar.visibility = View.GONE
        binding.buttonUseToken.visibility = View.VISIBLE
        binding.buttonLogout.visibility = View.VISIBLE
        binding.authorizedLayout.snack("Logout failed: $message")
    }

    private fun renderErrorState(ex: Throwable?) {
        binding.progressBar.visibility = View.GONE
        binding.buttonUseToken.visibility = View.VISIBLE
        binding.buttonLogout.visibility = View.VISIBLE
        binding.authorizedLayout.snack("Error: ${ex?.localizedMessage ?: ex.toString()}")
    }

    // Returns an observable that emits every time the Use Token button is pressed
    override fun useTokenIntent(): Observable<Unit> {
        return Observable.create { e ->
            binding.buttonUseToken.setOnClickListener { view ->
                e.setCancellable {
                    view.setOnClickListener(null)
                }
                e.onNext(Unit)
            }
        }
    }

    // Returns an observable that emits every time the Logout button is pressed
    override fun logoutIntent(): Observable<Unit> {
        return Observable.create { e ->
            binding.buttonLogout.setOnClickListener { view ->
                e.setCancellable {
                    view.setOnClickListener(null)
                }
                e.onNext(Unit)
            }
        }
    }

    // Returns an observable that emits the (Authorization)Result from the custom tab login activity
    // This allows the presenter to subscribe to the result of the login custom tab
    override fun logoutResponseIntent(): Observable<Int> {
        return logoutResponseSubject
            .subscribeOn(AndroidSchedulers.mainThread())
    }

    // Processes the results from the login custom (chrome) tab
    @Deprecated("Deprecated")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // For OAuth logout result code
        if (requestCode == AuthInteractor.RC_OAUTH_LOGOUT) {
            // Emit the result code
            logoutResponseSubject.onNext(resultCode)
        }
    }

    companion object {
        private const val TAG = "AuthorizedActivity"
    }
}