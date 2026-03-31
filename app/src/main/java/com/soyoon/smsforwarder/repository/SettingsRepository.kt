package com.soyoon.smsforwarder.repository

import android.content.Context
import com.soyoon.smsforwarder.model.ForwardLog
import com.soyoon.smsforwarder.model.ForwardRule
import org.json.JSONArray
import org.json.JSONObject

class SettingsRepository(context: Context) {

    companion object {
        private const val PREFS_NAME = "sms_forwarder_prefs"
        private const val KEY_FORWARDING_ENABLED = "forwarding_enabled"
        private const val KEY_FORWARD_RULES = "forward_rules"
        private const val KEY_FORWARD_LOGS = "forward_logs"
        const val MAX_LOG_COUNT = 100
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isForwardingEnabled(): Boolean {
        return prefs.getBoolean(KEY_FORWARDING_ENABLED, false)
    }

    fun setForwardingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FORWARDING_ENABLED, enabled).apply()
    }

    fun getForwardRules(): List<ForwardRule> {
        val json = prefs.getString(KEY_FORWARD_RULES, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                ForwardRule(
                    senderNumber = obj.getString("senderNumber"),
                    keyword = obj.getString("keyword"),
                    forwardTo = obj.getString("forwardTo"),
                    enabled = obj.optBoolean("enabled", true)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveForwardRules(rules: List<ForwardRule>) {
        val array = JSONArray()
        rules.forEach { rule ->
            val obj = JSONObject().apply {
                put("senderNumber", rule.senderNumber)
                put("keyword", rule.keyword)
                put("forwardTo", rule.forwardTo)
                put("enabled", rule.enabled)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_FORWARD_RULES, array.toString()).apply()
    }

    fun getForwardLogs(): List<ForwardLog> {
        val json = prefs.getString(KEY_FORWARD_LOGS, null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                ForwardLog(
                    timestamp = obj.getLong("timestamp"),
                    senderNumber = obj.getString("senderNumber"),
                    messageBody = obj.getString("messageBody"),
                    forwardedTo = obj.getString("forwardedTo"),
                    success = obj.getBoolean("success")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addForwardLog(log: ForwardLog) {
        val logs = getForwardLogs().toMutableList()
        logs.add(log)

        val trimmed = if (logs.size > MAX_LOG_COUNT) {
            logs.sortedBy { it.timestamp }.takeLast(MAX_LOG_COUNT)
        } else {
            logs
        }

        val array = JSONArray()
        trimmed.forEach { entry ->
            val obj = JSONObject().apply {
                put("timestamp", entry.timestamp)
                put("senderNumber", entry.senderNumber)
                put("messageBody", entry.messageBody)
                put("forwardedTo", entry.forwardedTo)
                put("success", entry.success)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_FORWARD_LOGS, array.toString()).apply()
    }

    fun clearForwardLogs() {
        prefs.edit().remove(KEY_FORWARD_LOGS).apply()
    }
}
