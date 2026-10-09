package com.example.utils

import java.util.Calendar
import java.util.Date

/**
 * Persian (Jalali / Solar Hijri) date calculations and helper methods.
 */
data class JalaliDate(
    val year: Int,
    val month: Int, // 1 to 12
    val day: Int    // 1 to 31
) {
    val monthName: String
        get() = PersianDateHelper.PERSIAN_MONTH_NAMES.getOrElse(month - 1) { "" }

    fun formatFormatted(): String {
        return "%04d/%02d/%02d".format(year, month, day)
    }

    fun formatDescriptive(): String {
        return "$day $monthName $year"
    }
}

object PersianDateHelper {
    val PERSIAN_MONTH_NAMES = listOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    val PERSIAN_WEEK_DAYS = listOf(
        "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه"
    )

    fun getDaysInMonth(year: Int, month: Int): Int {
        return when {
            month in 1..6 -> 31
            month in 7..11 -> 30
            month == 12 -> if (isLeapYear(year)) 30 else 29
            else -> 30
        }
    }

    fun isLeapYear(year: Int): Boolean {
        // Algorithm for Jalali leap year determination
        val a = 0.025
        val b = 266
        var leap = year % 33
        return (leap == 1 || leap == 5 || leap == 9 || leap == 13 || leap == 17 || leap == 22 || leap == 26 || leap == 30)
    }

    fun getTodayJalali(): JalaliDate {
        val cal = Calendar.getInstance()
        return gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun parseJalali(dateStr: String): JalaliDate {
        return try {
            val parts = dateStr.trim().split("/", "-", " ")
            if (parts.size >= 3) {
                JalaliDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            } else {
                getTodayJalali()
            }
        } catch (e: Exception) {
            getTodayJalali()
        }
    }

    /**
     * Converts Gregorian Date to Jalali Date
     */
    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        var gy2 = gy - 1600
        var gm2 = gm - 1
        var gd2 = gd - 1

        var gDayNo = 365 * gy2 + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400)

        for (i in 0 until gm2) {
            gDayNo += gDaysInMonth[i + 1]
        }
        if (gm2 > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) {
            gDayNo++
        }
        gDayNo += gd2

        var jDayNo = gDayNo - 79

        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += ((jDayNo - 1) / 365)
            jDayNo = (jDayNo - 1) % 365
        }

        var jm = 0
        for (i in 0..11) {
            if (jDayNo < jDaysInMonth[i + 1]) {
                jm = i + 1
                break
            }
            jDayNo -= jDaysInMonth[i + 1]
        }
        val jd = jDayNo + 1

        return JalaliDate(jy, jm, jd)
    }

    /**
     * Converts Jalali Date to Gregorian Date
     */
    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
        val jDaysInMonth = intArrayOf(0, 31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)
        val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

        val jy2 = jy - 979
        val jm2 = jm - 1
        val jd2 = jd - 1

        var jDayNo = 365 * jy2 + (jy2 / 33) * 8 + (((jy2 % 33) + 3) / 4)
        for (i in 0 until jm2) {
            jDayNo += jDaysInMonth[i + 1]
        }
        jDayNo += jd2

        var gDayNo = jDayNo + 79

        var gy = 1600 + 400 * (gDayNo / 146097)
        gDayNo %= 146097

        var leap = true
        if (gDayNo >= 36525) {
            gDayNo--
            gy += 100 * (gDayNo / 36524)
            gDayNo %= 36524

            if (gDayNo >= 365) {
                gDayNo++
            } else {
                leap = false
            }
        }

        gy += 4 * (gDayNo / 1461)
        gDayNo %= 1461

        if (gDayNo >= 366) {
            leap = false
            gDayNo--
            gy += gDayNo / 365
            gDayNo %= 365
        }

        var gm = 0
        for (i in 0..11) {
            val days = if (i == 1 && leap) 29 else gDaysInMonth[i + 1]
            if (gDayNo < days) {
                gm = i + 1
                break
            }
            gDayNo -= days
        }
        val gd = gDayNo + 1

        return Triple(gy, gm, gd)
    }

    fun fromTimestamp(timestamp: Long): JalaliDate {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun formatFromTimestamp(timestamp: Long): String {
        return fromTimestamp(timestamp).formatFormatted()
    }
}
