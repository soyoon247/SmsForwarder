package com.soyoon.smsforwarder.util

object PhoneNumberUtils {

    fun normalize(number: String): String {
        var result = number.replace(Regex("[\\s\\-()]"), "")

        if (result.startsWith("+82")) {
            result = "0" + result.removePrefix("+82")
        }

        return result
    }
}
