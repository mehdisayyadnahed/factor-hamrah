package factor.hamrah.app

import factor.hamrah.app.utils.PersianNumberHelper
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testPersianDigitsConversion() {
    val english = "1234567890"
    val persian = PersianNumberHelper.toPersianDigits(english)
    assertEquals("۱۲۳۴۵۶۷۸۹۰", persian)
    assertEquals(english, PersianNumberHelper.toEnglishDigits(persian))
  }

  @Test
  fun testFormatPrice() {
    val formatted = PersianNumberHelper.formatPrice(5000000L, "تومان", true)
    assertEquals("۵,۰۰۰,۰۰۰ تومان", formatted)
  }
}
