package com.dabataxi.Auth.Customer

data class Customer(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val photoUrl: String? = null,
    val gender: String = "",
    val password: String = ""
)

