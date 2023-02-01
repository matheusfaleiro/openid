package com.adein.oauthreference.view

import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar

// Convenience function to show a Snackbar
fun View.snack(message: String, length: Int = Snackbar.LENGTH_LONG) {
    val snack = Snackbar.make(this, message, length)
    snack.show()
}

// Convenience function to show a Toast
fun View.toast(message: String, length: Int = Toast.LENGTH_LONG) {
    Toast.makeText(context, message, length).show()
}