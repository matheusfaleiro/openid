package com.adein.oauthreference.presenter

import android.util.Log
import com.adein.oauthreference.data.AuthInteractor
import com.adein.oauthreference.data.AuthRepository
import com.adein.oauthreference.domain.AuthState
import com.adein.oauthreference.view.AuthorizedView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers

// Authorized presenter that is used when authorized/logged in
// Subscribes to intents from the view and hands off to the Interactor for business logic
// Subscribes to state changes from the model/Interactor and updates the UI
class AuthorizedPresenter(private val authInteractor: AuthInteractor) {

    private val compositeDisposable = CompositeDisposable()
    private val disposableOnDestroy = CompositeDisposable()

    private lateinit var view: AuthorizedView
    private val authRepository = AuthRepository

    // Initial Presenter setup
    // Store the view, subscribe to initial observables
    fun create(view: AuthorizedView) {
        this.view = view
        view.render(AuthState.LoadingState)
        disposableOnDestroy.add(observeLogoutResponseIntent())
    }

    // Bind to the newly created View
    // Store the view, subscribe to UI observables, render current (Auth)State
    fun bind(view: AuthorizedView) {
        this.view = view
        compositeDisposable.add(observeUseTokenIntent())
        compositeDisposable.add(observeLogoutIntent())
        view.render(authRepository.state)
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

    // Subscribe to the Use Token button observable, and pass events to the Interactor
    // Render the new (Auth)State from the Interactor
    private fun observeUseTokenIntent(): Disposable = view.useTokenIntent()
        .subscribeOn(AndroidSchedulers.mainThread())
        .observeOn(Schedulers.io())
        .flatMap {
            Log.d(TAG, "Use token pressed")
            authInteractor.useTokens()
        }
        .observeOn(AndroidSchedulers.mainThread())
        .onErrorReturn { AuthState.ErrorState(it) }
        .subscribe {
            view.render(it)
            authRepository.state = it
        }

    // Subscribe to the Logout button observable, and pass events to the Interactor
    // Render the new (Auth)State from the Interactor
    private fun observeLogoutIntent() = view.logoutIntent()
        .subscribeOn(AndroidSchedulers.mainThread())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext {
            view.render(AuthState.LoadingState)
            authRepository.state = AuthState.LoadingState
        }
        .observeOn(Schedulers.io())
        .flatMap {
            Log.d(TAG, "Logout pressed")
            authInteractor.logout()
        }
        .observeOn(AndroidSchedulers.mainThread())
        .onErrorReturn { AuthState.ErrorState(it) }
        .subscribe {
            view.render(it)
            authRepository.state = it
        }

    // Subscribe to the result of the logout activity (custom tab) and pass results to the Interactor
    // Render the new (Auth)State from the Interactor
    private fun observeLogoutResponseIntent() = view.logoutResponseIntent()
        .observeOn(AndroidSchedulers.mainThread())
        .flatMap { result ->
            Log.d(TAG, "Handle logout response: $result")
            authInteractor.handleLogout(result)
                .onErrorReturn { AuthState.ErrorState(it) }
        }
        .onErrorReturn { AuthState.ErrorState(it) }
        .subscribe {
            view.render(it)
            authRepository.state = it
        }

    companion object {
        private const val TAG = "AuthorizedPresenter"
    }
}