package com.exchangecalc.app.model

enum class Currency(
    val code: String,
    val symbol: String,
    val flag: String,
    val decimalPlaces: Int
) {
    USD("USD", "$", "\uD83C\uDDFA\uD83C\uDDF8", 2),
    EUR("EUR", "\u20AC", "\uD83C\uDDEA\uD83C\uDDFA", 2),
    GBP("GBP", "\u00A3", "\uD83C\uDDEC\uD83C\uDDE7", 2),
    JPY("JPY", "\u00A5", "\uD83C\uDDEF\uD83C\uDDF5", 0),
    KRW("KRW", "\u20A9", "\uD83C\uDDF0\uD83C\uDDF7", 0),
    CNY("CNY", "\u00A5", "\uD83C\uDDE8\uD83C\uDDF3", 2),
    TWD("TWD", "NT$", "\uD83C\uDDF9\uD83C\uDDFC", 2),
    HKD("HKD", "HK$", "\uD83C\uDDED\uD83C\uDDF0", 2),
    THB("THB", "\u0E3F", "\uD83C\uDDF9\uD83C\uDDED", 2),
    AUD("AUD", "A$", "\uD83C\uDDE6\uD83C\uDDFA", 2),
    PHP("PHP", "\u20B1", "\uD83C\uDDF5\uD83C\uDDED", 2),
    VND("VND", "\u20AB", "\uD83C\uDDFB\uD83C\uDDF3", 0),
    IDR("IDR", "Rp", "\uD83C\uDDEE\uD83C\uDDE9", 0),
    MYR("MYR", "RM", "\uD83C\uDDF2\uD83C\uDDFE", 2),
    SGD("SGD", "S$", "\uD83C\uDDF8\uD83C\uDDEC", 2),
    CAD("CAD", "C$", "\uD83C\uDDE8\uD83C\uDDE6", 2),
    INR("INR", "\u20B9", "\uD83C\uDDEE\uD83C\uDDF3", 2),
    CHF("CHF", "CHF", "\uD83C\uDDE8\uD83C\uDDED", 2),
    RUB("RUB", "\u20BD", "\uD83C\uDDF7\uD83C\uDDFA", 2);

    companion object {
        fun fromCode(code: String): Currency? = entries.find { it.code == code }
    }
}
