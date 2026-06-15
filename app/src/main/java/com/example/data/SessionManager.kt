package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.data.models.User

data class ControleDraft(
    val equipmentId: Int,
    val step1Ok: Boolean,
    val step2Ok: Boolean,
    val step3Ok: Boolean,
    val isConforme: Boolean,
    val notes: String,
    val signaturePath: String
)

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "inventaire_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NOM = "user_nom"
        private const val KEY_USER_PRENOM = "user_prenom"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_API_BASE_URL = "api_base_url"
        private const val DEFAULT_API_URL = "https://example.com/inventaire/" // Mock default
    }

    fun loginUser(user: User, sessionId: String?) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putInt(KEY_USER_ID, user.id)
            putString(KEY_USER_NOM, user.nom)
            putString(KEY_USER_PRENOM, user.prenom)
            putString(KEY_USER_EMAIL, user.email)
            putString(KEY_USER_ROLE, user.role)
            putString(KEY_SESSION_ID, sessionId)
            apply()
        }
    }

    fun logoutUser() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_USER_ID)
            remove(KEY_USER_NOM)
            remove(KEY_USER_PRENOM)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_ROLE)
            remove(KEY_SESSION_ID)
            apply()
        }
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUser(): User? {
        if (!isLoggedIn()) return null
        val id = prefs.getInt(KEY_USER_ID, -1)
        val nom = prefs.getString(KEY_USER_NOM, "") ?: ""
        val prenom = prefs.getString(KEY_USER_PRENOM, "") ?: ""
        val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        val role = prefs.getString(KEY_USER_ROLE, "") ?: ""
        if (id == -1) return null
        return User(id, nom, prenom, email, role)
    }

    fun getSessionId(): String? {
        return prefs.getString(KEY_SESSION_ID, null)
    }

    fun getApiBaseUrl(): String {
        var url = prefs.getString(KEY_API_BASE_URL, DEFAULT_API_URL) ?: DEFAULT_API_URL
        if (!url.endsWith("/")) {
            url += "/"
        }
        return url
    }

    fun setApiBaseUrl(url: String) {
        val cleanUrl = if (url.endsWith("/")) url else "$url/"
        prefs.edit().putString(KEY_API_BASE_URL, cleanUrl).apply()
    }

    fun saveControleDraft(equipmentId: Int, step1Ok: Boolean, step2Ok: Boolean, step3Ok: Boolean, isConforme: Boolean, notes: String, signaturePath: String) {
        prefs.edit().apply {
            putInt("draft_eq_id", equipmentId)
            putBoolean("draft_step1_$equipmentId", step1Ok)
            putBoolean("draft_step2_$equipmentId", step2Ok)
            putBoolean("draft_step3_$equipmentId", step3Ok)
            putBoolean("draft_conforme_$equipmentId", isConforme)
            putString("draft_notes_$equipmentId", notes)
            putString("draft_signature_$equipmentId", signaturePath)
            apply()
        }
    }

    fun getControleDraft(equipmentId: Int): ControleDraft? {
        val savedEqId = prefs.getInt("draft_eq_id", -1)
        if (savedEqId != equipmentId) return null
        return ControleDraft(
            equipmentId = equipmentId,
            step1Ok = prefs.getBoolean("draft_step1_$equipmentId", false),
            step2Ok = prefs.getBoolean("draft_step2_$equipmentId", false),
            step3Ok = prefs.getBoolean("draft_step3_$equipmentId", false),
            isConforme = prefs.getBoolean("draft_conforme_$equipmentId", true),
            notes = prefs.getString("draft_notes_$equipmentId", "") ?: "",
            signaturePath = prefs.getString("draft_signature_$equipmentId", "") ?: ""
        )
    }

    fun clearControleDraft(equipmentId: Int) {
        prefs.edit().apply {
            if (prefs.getInt("draft_eq_id", -1) == equipmentId) {
                remove("draft_eq_id")
            }
            remove("draft_step1_$equipmentId")
            remove("draft_step2_$equipmentId")
            remove("draft_step3_$equipmentId")
            remove("draft_conforme_$equipmentId")
            remove("draft_notes_$equipmentId")
            remove("draft_signature_$equipmentId")
            apply()
        }
    }
}
