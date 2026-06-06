package com.example.data

enum class TemperatureUnit(val code: String, val symbol: String, val displayName: String) {
    CELSIUS("C", "℃", "Celsius (℃)"),
    FAHRENHEIT("F", "℉", "Fahrenheit (℉)"),
    KELVIN("K", "K", "Kelvin (K)");

    companion object {
        fun fromCode(code: String): TemperatureUnit {
            return values().find { it.code == code } ?: CELSIUS
        }
    }
}
