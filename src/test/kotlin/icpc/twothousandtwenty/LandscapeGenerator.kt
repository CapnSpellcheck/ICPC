package icpc.twothousandtwenty

import util.StringOutputStream
import java.io.StringBufferInputStream
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class LandscapeGeneratorTest {
   val MAX_POINTS = 200000

   @Test fun testNoModification() {
      val input = "1500 0\n"
      val sos = StringOutputStream()
      sos.close()
      generateLandscapeIO(StringBufferInputStream(input), sos)

      val expected = "0\n".repeat(1500)
      assertEquals(expected, sos.toString())
   }

   @Test fun testSample1() {
      val input = """
         22 13
         H 12 13
         D 5 18
         R 13 14
         R 8 16
         H 2 3
         V 10 19
         V 3 13
         R 8 13
         V 3 10
         D 5 18
         V 11 12
         R 1 6
         R 14 19
      """.trimIndent()

      val sos = StringOutputStream()
      generateLandscapeIO(StringBufferInputStream(input), sos)
      sos.close()
      assertEquals("1\n" +
         "2\n" +
         "0\n" +
         "-3\n" +
         "-7\n" +
         "-9\n" +
         "-11\n" +
         "-9\n" +
         "-7\n" +
         "-6\n" +
         "-6\n" +
         "-5\n" +
         "-3\n" +
         "-4\n" +
         "-5\n" +
         "-4\n" +
         "-4\n" +
         "-3\n" +
         "0\n" +
         "0\n0\n0\n", sos.toString(), "sample 1 output")
   }

   private fun randomModificationString(points: Int): String {
      val start = Random.nextInt(1, points + 1)
      val end = Random.nextInt(start, points + 1)
      val type = Random.nextInt(1, 5)
      val char = when (type) {
         1 -> 'R'
         2 -> 'D'
         3 -> 'H'
         4 -> 'V'
         else -> error("")
      }
      return "$char $start $end\n"
   }

   @Test fun performanceTest() {
      var dur = 0L
      repeat(20) {
         val modifications = (1..200000).map { randomModificationString(60000) }
         val sis = StringBufferInputStream(modifications.joinToString("", prefix = "60000 ${modifications.size}\n"))
         val sos = StringOutputStream()
         val start = System.currentTimeMillis()
         generateLandscapeIO(sis, sos)
         dur += System.currentTimeMillis() - start
      }

      println ("----------")
      println("$dur ms")
   }

}
