package com.reftgres.taihelper.ui.utils

import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.reftgres.taihelper.R

object SnackbarUtils {
    fun showSnackbar(view: View, message: String, isError: Boolean = false, anchorView: View? = null) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).apply {
            anchorView?.let { this.anchorView = it }
            setBackgroundTint(
                view.context.getColor(if (isError) R.color.error_red else R.color.success_green)
            )
            show()
        }
    }
}