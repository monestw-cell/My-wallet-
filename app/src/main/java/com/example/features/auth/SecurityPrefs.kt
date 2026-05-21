package com.example.features.auth

import android.content.Context
import android.content.SharedPreferences

class SecurityPrefs(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN = "user_pin"
        private const val KEY_LOCK_ENABLED = "lock_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_AUTH_METHOD = "auth_method" // "PIN", "BIOMETRIC", "BOTH"
    }

    fun setPin(pin: String?) {
        prefs.edit().putString(KEY_PIN, pin).apply()
    }

    fun getPin(): String? {
        return prefs.getString(KEY_PIN, null)
    }

    fun setLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply()
    }

    fun isLockEnabled(): Boolean {
        if (getPin().isNullOrEmpty()) return false
        return prefs.getBoolean(KEY_LOCK_ENABLED, false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setAuthMethod(method: String) {
        prefs.edit().putString(KEY_AUTH_METHOD, method).apply()
    }

    fun getAuthMethod(): String {
        return prefs.getString(KEY_AUTH_METHOD, "BOTH") ?: "BOTH"
    }
}
