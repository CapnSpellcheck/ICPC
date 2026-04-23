package icpc.twothousandtwelve

import org.junit.jupiter.api.Test
import util.StringOutputStream
import java.io.StringBufferInputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CurvyLittleBottlesTest {
   val FORMAT = "%.2f"

   @Test
   fun testSample1() {
      val expected = VolumeResult(263.89, listOf(0.511, 1.061, 1.661, 2.311, 3.021, 3.831, 4.751, 5.871))
      val actual = volumeMarks(Polynomial(doubleArrayOf(4.0, -.25)), .0, 12.0, 25)
      assertEquals(FORMAT.format(expected.volume), FORMAT.format(actual.volume), "bottle volume")
      assertEquals(expected.markDistances.map { "%.2f".format(it) }, actual.markDistances.map { "%.2f".format(it)}, "volume marks")
   }

   @Test fun testSample2() {
      val expected = VolumeResult(263.89, listOf())
      val actual = volumeMarks(Polynomial(doubleArrayOf(4.0, -.25)), .0, 12.0, 300)
      assertEquals(FORMAT.format(expected.volume), FORMAT.format(actual.volume), "bottle volume")
      assertTrue(actual.markDistances.isEmpty(), "insufficient volume")
   }

   @Test fun testSample4() {
      val expected = VolumeResult(31.42, listOf(3.18, 6.37, 9.55))
      val actual = volumeMarks(Polynomial(doubleArrayOf(1.0)), .0, 10.0, 10)
      assertEquals(FORMAT.format(expected.volume), FORMAT.format(actual.volume), "bottle volume")
      assertEquals(expected.markDistances.map { "%.2f".format(it) }, actual.markDistances.map { "%.2f".format(it)}, "volume marks")
   }

   @Test fun testSample3() {
      val expected = VolumeResult(50.0, listOf(2, 4).map { it.toDouble() })
      val actual = volumeMarks(Polynomial(doubleArrayOf(1.7841241161782)), 5.0, 10.0, 20)
      assertEquals(FORMAT.format(expected.volume), FORMAT.format(actual.volume), "bottle volume")
      assertEquals(expected.markDistances.map { "%.2f".format(it) }, actual.markDistances.map { "%.2f".format(it)}, "volume marks")
   }

   @Test fun testSampleIO() {
      val sis = StringBufferInputStream("""
         1
         4.0 -0.25
         0.0 12.0 25
         1
         4.0 -0.25
         0.0 12.0 300
         0
         1.7841241161782
         5.0 10.0 20
         0
         1.0
         0.0 10.0 10
      """.trimIndent() + "\n")
      val sos = StringOutputStream()

      volumeMarksIO(sis, sos)
      assertEquals("""
         Case 1: 263.89
         0.51 1.06 1.66 2.31 3.02 3.83 4.75 5.87
         Case 2: 263.89
         insufficient volume
         Case 3: 50.00
         2.00 4.00
         Case 4: 31.42
         3.18 6.37 9.55
      """.trimIndent() + "\n", sos.toString())
   }

}
