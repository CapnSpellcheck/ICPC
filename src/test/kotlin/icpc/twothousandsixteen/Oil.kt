package icpc.twothousandsixteen

import util.Interval
import util.StringOutputStream
import java.io.StringBufferInputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class OilTest {
   @Test fun testSample1() {
      val deposits = listOf(
         Deposit(20, Interval(100, 180)),
         Deposit(30, Interval(30, 60)),
         Deposit(40, Interval(70, 110)),
         Deposit(50, Interval(10, 40)),
         Deposit(70, Interval(0, 80)),
      )
      assertEquals(200, maximalSumOfDepositWidthsHitByAnyLineFromSurface(deposits))
   }

   @Test fun testSample2() {
      val deposits = listOf(
         Deposit(10, Interval(50, 60)),
         Deposit(10, Interval(0, 25)),
      )
      assertEquals(25, maximalSumOfDepositWidthsHitByAnyLineFromSurface(deposits))
   }

   @Test fun testSample1IO() {
      val input = """
         5
         100 180 20
         30 60 30
         70 110 40
         10 40 50
         0 80 70
         """.trimIndent()
      val sos = StringOutputStream()
      maximalSumOfDepositWidthsHitByAnyLineFromSurfaceIO(StringBufferInputStream(input), sos)
      assertEquals("200", sos.toString())
   }

   @Test fun testSample2IO() {
      val input = """
         3
         50 60 10
         -42 -42 20
         25 0 10
      """.trimIndent()
      val sos = StringOutputStream()
      maximalSumOfDepositWidthsHitByAnyLineFromSurfaceIO(StringBufferInputStream(input), sos)
      assertEquals("25", sos.toString())
   }
}