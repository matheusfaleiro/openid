package com.adein.oauthreference.presenter

import android.util.Log
import com.adein.oauthreference.data.AuthInteractor
import com.adein.oauthreference.data.AuthRepository
import com.adein.oauthreference.domain.AuthState
import com.adein.oauthreference.view.MainView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

// Login presenter that is used when not logged in
// Subscribes to intents from the view and hands off to the Interactor for business logic
// Subscribes to state changes from the model/Interactor and updates the UI
class MainPresenter(private val authInteractor: AuthInteractor) {

    private lateinit var compositeDisposable: CompositeDisposable
    private val disposableOnDestroy = CompositeDisposable()

    private lateinit var view: MainView
    private val authRepository = AuthRepository

    private var initialLoadComplete = false

    // Initial Presenter setup
    // Store the view, subscribe to initial observables
    fun create(view: MainView) {
        this.view = view
        disposableOnDestroy.add(observeAuthorizationResultIntent())
    }

    // Bind to the newly created View
    // Store the view, initial state setup, subscribe to UI observables
    fun bind(view: MainView) {
        this.view = view
        compositeDisposable = CompositeDisposable()
        compositeDisposable.add(observeLoginIntent())
        loadOnBind()
        // Render current state
        view.render(authRepository.state)
    }

    // Setup initial state - load any previously stored (Auth)State
    private fun loadOnBind() {
        if (!initialLoadComplete) {
            Log.d(TAG, "Load previous state")
            view.render(AuthState.LoadingState)
            authRepository.state = AuthState.LoadingState
            authInteractor.loadPreviousState()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    view.render(it)
                    authRepository.state = it
                }
            initialLoadComplete = true
        }
    }

    // Unbinds the Presenter from the View
    // Disposes of compositeDisposables
    fun unbind() {
        if (!compositeDisposable.isDisposed) {
            compositeDisposable.dispose()
        }
    }

    // Prepare for destruction!
    // Disposes of disposableOnDestroy
    fun destroy() {
        if (!disposableOnDestroy.isDisposed) {
            disposableOnDestroy.dispose()
        }
    }

    // Subscribe to the login button observable, and pass events to the Interactor
    // Render the new (Auth)State from the Interactor
    private fun observeLoginIntent() = view.loginIntent()
        .subscribeOn(AndroidSchedulers.mainThread())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext {
            view.render(AuthState.LoadingState)
            authRepository.state = AuthState.LoadingState
        }
        .observeOn(Schedulers.io())
        .flatMap {
            Log.d(TAG, "Login pressed")
            authInteractor.login()
        }
        .observeOn(AndroidSchedulers.mainThread())
        .onErrorReturn { AuthState.ErrorState(it) }
        .subscribe {
            view.render(it)
            authRepository.state = it
        }

    // Subscribe to the result of the login activity (custom tab) and pass results to the Interactor
    // Render the new (Auth)State from the Interactor
    private fun observeAuthorizationResultIntent() = view.loginResponseIntent()
        .observeOn(AndroidSchedulers.mainThread())
        .flatMap { result ->
            Log.d(TAG, "Handle login response: $result")
            authInteractor.handleLogin(result)
                .onErrorReturn { AuthState.ErrorState(it) }
        }
        .doOnNext {
            Log.d(TAG, "Handle login response next: $it")
        }
        .onErrorReturn { AuthState.ErrorState(it) }
        .subscribe {
            view.render(it)
            authRepository.state = it
        }

    companion object {
        private const val TAG = "MainPresenter"
    }
}