package com.reftgres.taihelper.ui.reset

class ForgotPasswordStatesealed open class ForgotPasswordState {
    object Loading : ForgotPasswordState()
    object Success : ForgotPasswordState()
    data class Error(val message: String) : ForgotPasswordState()
}