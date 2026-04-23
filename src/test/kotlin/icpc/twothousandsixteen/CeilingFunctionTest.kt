package icpc.twothousandsixteen

import org.junit.jupiter.api.Test
import util.StringOutputStream
import java.io.StringBufferInputStream
import kotlin.test.assertEquals

class CeilingFunctionTest {
   @Test
   fun testSample1() {
      val trees = listOf(
         intArrayOf(2,7,1),
         intArrayOf(3,1,4),
         intArrayOf(1,5,9),
         intArrayOf(2,6,5),
         intArrayOf(9,7,3),
      )
      assertEquals(4, countShapes(trees))
   }

   @Test fun testSample1IO() {
      val sis = StringBufferInputStream("""
         5 3
         2 7 1
         3 1 4
         1 5 9
         2 6 5
         9 7 3
      """.trimIndent())
      val sos = StringOutputStream()

      countShapesIO(sis, sos)
      assertEquals("4", sos.toString())
   }

   @Test fun testRearranged() {
      val trees = listOf(
         intArrayOf(1,5,9),
         intArrayOf(9,7,3),
         intArrayOf(2,6,5),
         intArrayOf(2,7,1),
         intArrayOf(3,1,4),
      )
      assertEquals(4, countShapes(trees))
   }
}