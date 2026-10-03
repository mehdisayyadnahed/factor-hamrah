package com.example.utils

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object PersianNumberHelper {

    private val persianDigits = arrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    private val englishDigits = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

    fun toPersianDigits(text: String?): String {
        if (text == null) return ""
        val sb = StringBuilder()
        for (ch in text) {
            if (ch in '0'..'9') {
                sb.append(persianDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toEnglishDigits(text: String?): String {
        if (text == null) return ""
        val sb = StringBuilder()
        for (ch in text) {
            val index = persianDigits.indexOf(ch)
            if (index != -1) {
                sb.append(englishDigits[index])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatPrice(amount: Long, currency: String = "تومان", usePersianDigits: Boolean = true): String {
        val formatter = DecimalFormat("#,###")
        val formatted = formatter.format(amount)
        val text = "$formatted $currency"
        return if (usePersianDigits) toPersianDigits(text) else text
    }

    fun formatPriceNumberOnly(amount: Long, usePersianDigits: Boolean = true): String {
        val formatter = DecimalFormat("#,###")
        val formatted = formatter.format(amount)
        return if (usePersianDigits) toPersianDigits(formatted) else formatted
    }

    fun parseAmount(input: String?): Long {
        if (input.isNullOrBlank()) return 0L
        val clean = toEnglishDigits(input).replace("[^0-9]".toRegex(), "")
        return clean.toLongOrNull() ?: 0L
    }

    fun parseDouble(input: String?): Double {
        if (input.isNullOrBlank()) return 0.0
        val clean = toEnglishDigits(input).replace("[^0-9.]".toRegex(), "")
        return clean.toDoubleOrNull() ?: 0.0
    }

    /**
     * Formats 16-digit card number with hyphens only and no spaces: 1234-5648-1234-5678
     */
    fun formatCardNumber(card: String?): String {
        if (card.isNullOrBlank()) return ""
        val clean = toEnglishDigits(card).replace("-", "").replace(" ", "").trim()
        if (clean.length == 16) {
            return "${clean.substring(0, 4)}-${clean.substring(4, 8)}-${clean.substring(8, 12)}-${clean.substring(12, 16)}"
        }
        return if (clean.length >= 4) clean.chunked(4).joinToString("-") else clean
    }

    // Persian Number To Words Conversion
    private val yekan = arrayOf("", "یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه")
    private val dahgan = arrayOf("", "", "بیست", "سی", "چهل", "پنجاه", "شصت", "هفتاد", "هشتاد", "نود")
    private val dahTaNoozdah = arrayOf("ده", "یازده", "دوازده", "سیزده", "چهارده", "پانزده", "شانزده", "هفده", "هجده", "نوزده")
    private val sadgan = arrayOf("", "یکصد", "دویست", "سیصد", "چهارصد", "پانصد", "ششصد", "هفتصد", "هشتصد", "نهصد")
    private val tabaghat = arrayOf("", "هزار", "میلیون", "میلیارد", "تریلیون")

    fun numberToPersianWords(amount: Long, currency: String = "تومان"): String {
        if (amount == 0L) return "صفر $currency"
        if (amount < 0L) return "منفی " + numberToPersianWords(-amount, currency)

        var num = amount
        val groups = mutableListOf<Int>()
        while (num > 0) {
            groups.add((num % 1000).toInt())
            num /= 1000
        }

        val parts = mutableListOf<String>()
        for (i in groups.indices.reversed()) {
            val groupValue = groups[i]
            if (groupValue != 0) {
                val groupText = convertThreeDigits(groupValue)
                val suffix = tabaghat.getOrElse(i) { "" }
                if (suffix.isNotEmpty()) {
                    parts.add("$groupText $suffix")
                } else {
                    parts.add(groupText)
                }
            }
        }

        val result = parts.joinToString(" و ")
        return if (currency.isNotEmpty()) "$result $currency" else result
    }

    private fun convertThreeDigits(n: Int): String {
        val s = n / 100
        val d = (n % 100) / 10
        val y = n % 10

        val words = mutableListOf<String>()

        if (s > 0) {
            words.add(sadgan[s])
        }

        if (d == 1) {
            words.add(dahTaNoozdah[y])
        } else {
            if (d > 1) {
                words.add(dahgan[d])
            }
            if (y > 0) {
                words.add(yekan[y])
            }
        }

        return words.joinToString(" و ")
    }
}
