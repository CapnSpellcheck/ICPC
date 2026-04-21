package icpc.twothousandeighteen

import org.junit.jupiter.api.Test
import util.StringOutputStream
import java.io.StringBufferInputStream
import kotlin.random.Random
import kotlin.test.assertEquals

class CommaSprinklerTest {
   @Test fun testSample1() {
      val input = "please sit spot. sit spot, sit. spot here now here.\n"
      val expected = "please, sit spot. sit spot, sit. spot, here now, here.\n"
      val sos = StringOutputStream()
      DrSprinkler().apply(input, sos)
      assertEquals(expected, sos.toString(), "Sample 1")
   }

   @Test fun testSample1IO() {
      val input = "please sit spot. sit spot, sit. spot here now here.\n"
      val expected = "please, sit spot. sit spot, sit. spot, here now, here.\n"
      val sos = StringOutputStream()
      applyDrSprinklerIO(StringBufferInputStream(input), sos)
      assertEquals(expected, sos.toString(), "Sample 1 IO")
   }

   @Test fun testSample2() {
      val input = "one, two. one tree. four tree. four four. five four. six five.\n"
      val expected = "one, two. one, tree. four, tree. four, four. five, four. six five.\n"
      val sos = StringOutputStream()
      DrSprinkler().apply(input, sos)
      assertEquals(expected, sos.toString(), "Sample 1")
   }

   @Test fun testSample2IO() {
      val input = "one, two. one tree. four tree. four four. five four. six five.\n"
      val expected = "one, two. one, tree. four, tree. four, four. five, four. six five.\n"
      val sos = StringOutputStream()
      applyDrSprinklerIO(StringBufferInputStream(input), sos)
      assertEquals(expected, sos.toString(), "Sample 1")
   }

   // this one helped me locate a ConcurrentModificationException, even though I don't test the output!
   @Test fun testRandom() {
      val leftLimit = 97 // letter 'a'
      val rightLimit = 122 // letter 'z'
      val words = List(1000) {
         val sb = StringBuilder()
         for (i in 0..< 10) {
            val randomLimitedInt = leftLimit + (Random.nextFloat() * (rightLimit - leftLimit + 1)).toInt()
            sb.append(randomLimitedInt.toChar())
         }
         sb.toString()
      }
      val buf = StringBuilder(1000000)
      while (buf.length < buf.capacity() - 10) {
         buf.append(words.random())
         if (Random.nextFloat() < 0.05) {
            buf.append(',')
         } else if (Random.nextFloat() < 0.1) {
            buf.append('.')
         }
         buf.append(' ')
      }
      buf.append("end.")

      val sos = StringOutputStream()
      applyDrSprinklerIO(StringBufferInputStream(buf.toString()), sos)
   }
}