package com.soyoon.smsforwarder.model

data class ForwardLog(
    val timestamp: Long,
    val senderNumber: String,
    val messageBody: String,
    val forwardedTo: String,
    val success: Boolean
)
