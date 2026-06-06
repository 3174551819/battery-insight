package com.example.data

enum class AppLanguage(val code: String, val displayName: String) {
    SIMPLIFIED_CHINESE("zh", "简体中文"),
    TRADITIONAL_CHINESE("zh-TW", "繁體中文"),
    ENGLISH("en", "English"),
    JAPANESE("ja", "日本語"),
    GERMAN("de", "Deutsch"),
    KOREAN("ko", "한국어"),
    FRENCH("fr", "Français"),
    SPANISH("es", "Español");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return values().find { it.code == code } ?: SIMPLIFIED_CHINESE
        }
    }
}
