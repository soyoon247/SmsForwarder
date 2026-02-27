package com.soyoon.smsforwarder.model

data class ForwardRule(
    val senderNumber: String,
    val keyword: String,
    val forwardTo: String,
    val enabled: Boolean = true
)
