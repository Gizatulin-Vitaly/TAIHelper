package com.reftgres.taihelper.model

data class User(
    val name: String = "",
    val lastName: String = "",
    val position: String = "",
    val email: String = "",
    val status: String = "3" // По умолчанию
)
