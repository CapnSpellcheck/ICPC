package icpc.twothousandsixteen

import util.StringOutputStream
import java.io.StringBufferInputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class PCPTest {
   @Test fun testSample1() {
      val tasks = arrayOf(
         PCPTask(3, 70, 3, "C1"),
         PCPTask(1, 50, 2, "C1 L1 C1 U1 C1"),
         PCPTask(2, 1, 1, "C1 L1 C100 U1 C1"),
      )
      val times = finishTimes(tasks, 1)
      assertContentEquals(intArrayOf(106, 107, 71), times)
   }

   @Test fun testSample1IO() {
      val input = """
         3 1
         50 2 5 C1 L1 C1 U1 C1
         1 1 5 C1 L1 C100 U1 C1
         70 3 1 C1
      """.trimIndent()
      val sos = StringOutputStream()

      finishTimesIO(StringBufferInputStream(input), sos)
      assertEquals("106\n107\n71\n", sos.toString())
   }

   @Test fun testSample2() {
      val tasks = arrayOf(
         PCPTask(2, 3, 2, "C1 L2 C1 L3 C1 U3 C1 U2 C1"),
         PCPTask(1, 5, 3, "C1 L1 C1 U1 C1"),
         PCPTask(3, 1, 1, "C1 L3 C3 L2 C1 U2 C1 U3 C1"),
      )
      val times = finishTimes(tasks, 3)
      assertContentEquals(intArrayOf(8, 15, 16), times)
   }

   @Test fun testSample2IO() {
      val input = """
         3 3
         5 3 5 C1 L1 C1 U1 C1
         3 2 9 C1 L2 C1 L3 C1 U3 C1 U2 C1
         1 1 9 C1 L3 C3 L2 C1 U2 C1 U3 C1
      """.trimIndent()
      val sos = StringOutputStream()

      finishTimesIO(StringBufferInputStream(input), sos)
      assertEquals("8\n15\n16\n", sos.toString())
   }
}