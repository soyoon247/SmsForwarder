package com.soyoon.smsforwarder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import com.soyoon.smsforwarder.model.ForwardLog
import com.soyoon.smsforwarder.repository.SettingsRepository
import com.soyoon.smsforwarder.util.PhoneNumberUtils

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
        private const val MAX_FORWARDS_PER_MINUTE = 10
        private const val DUPLICATE_WINDOW_MS = 5 * 60 * 1000L

        private val recentMessages = LinkedHashMap<String, Long>()
        private val forwardTimestamps = mutableListOf<Long>()
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val repo = SettingsRepository(context)
        if (!repo.isForwardingEnabled()) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].originatingAddress ?: return
        val body = messages.joinToString("") { it.messageBody ?: "" }
        val timestamp = messages[0].timestampMillis

        Log.d(TAG, "SMS received from: $sender")

        val normalizedSender = PhoneNumberUtils.normalize(sender)

        if (isDuplicate(normalizedSender, body, timestamp)) {
            Log.d(TAG, "Duplicate message, skipping")
            return
        }

        if (isRateLimited()) {
            Log.w(TAG, "Rate limit exceeded, skipping")
            return
        }

        val rules = repo.getForwardRules()
        for (rule in rules) {
            if (!rule.enabled) continue

            val normalizedRuleSender = PhoneNumberUtils.normalize(rule.senderNumber)
            if (normalizedSender != normalizedRuleSender) continue

            if (rule.keyword.isNotEmpty() && !body.contains(rule.keyword)) continue

            val normalizedForwardTo = PhoneNumberUtils.normalize(rule.forwardTo)
            if (normalizedSender == normalizedForwardTo) {
                Log.w(TAG, "Loop detected: sender == forwardTo ($normalizedSender)")
                continue
            }

            val success = forwardSms(rule.forwardTo, body)
            recordForwardTimestamp()

            repo.addForwardLog(
                ForwardLog(
                    timestamp = System.currentTimeMillis(),
                    senderNumber = sender,
                    messageBody = body,
                    forwardedTo = rule.forwardTo,
                    success = success
                )
            )

            Log.d(TAG, "Forwarded to ${rule.forwardTo}, success=$success")
        }
    }

    private fun forwardSms(destination: String, body: String): Boolean {
        return try {
            val smsManager = SmsManager.getDefault()
            if (body.length > 160) {
                val parts = smsManager.divideMessage(body)
                smsManager.sendMultipartTextMessage(destination, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(destination, null, body, null, null)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to forward SMS", e)
            false
        }
    }

    private fun isDuplicate(sender: String, body: String, timestamp: Long): Boolean {
        val now = System.currentTimeMillis()
        recentMessages.entries.removeAll { now - it.value > DUPLICATE_WINDOW_MS }

        val hash = "$sender|$body|$timestamp".hashCode().toString()
        if (recentMessages.containsKey(hash)) return true

        recentMessages[hash] = now
        return false
    }

    private fun isRateLimited(): Boolean {
        val now = System.currentTimeMillis()
        forwardTimestamps.removeAll { now - it > 60_000 }
        return forwardTimestamps.size >= MAX_FORWARDS_PER_MINUTE
    }

    private fun recordForwardTimestamp() {
        forwardTimestamps.add(System.currentTimeMillis())
    }
}
