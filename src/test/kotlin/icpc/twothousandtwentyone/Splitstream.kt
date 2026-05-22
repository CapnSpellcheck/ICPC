package icpc.twothousandtwentyone

import icpc.twothousandtwentyone.Splitstream.*
import icpc.twothousandtwentyone.Splitstream.Companion.NONEXISTENT
import kotlin.test.*

@OptIn(ExperimentalUnsignedTypes::class)
class SplitstreamTest {
   @Test fun testSample1() {
      val stream = Splitstream(
         200,
         listOf(Node('S', 1, 2, 3), Node('M', 3, 2, 4)),
      )

      assertEquals(100U, stream.query(4, 99U))
      assertEquals(99U,  stream.query(4, 100U))
   }

   @Test fun testSample2() {
      val stream = Splitstream(
         100,
         listOf(
            Node('S', 1, 4, 2),
            Node('S', 2, 3, 5),
            Node('M', 3, 4, 6),
         ),
      )

      assertEquals(47U, stream.query(6, 48U))
      assertEquals(98U, stream.query(6, 49U))
      assertEquals(49U, stream.query(6, 50U))
      assertEquals(51U, stream.query(6, 51U))
      assertEquals(53U, stream.query(6, 52U))
      assertEquals(100U, stream.query(5, 25U))
   }

   @Test fun testSample3() {
      val stream = Splitstream(
         2,
         listOf(
            Node('S', 1, 2, 3),
            Node('S', 3, 4, 5),
            Node('M', 5, 2, 6),
         ),
      )

      assertEquals(2U, stream.query(3, 1U))
      assertTrue(stream.query(5, 1U).isNonexistent)
      assertTrue(stream.query(6, 2U).isNonexistent)
   }

   @Test fun testOneNode() {
      listOf(99, 99999, 999999999).forEach { n ->
         val stream = Splitstream(n, listOf(Node('S', 1, 2, 3)))
         assertEquals(1U, stream.query(2, 1U))
         assertEquals(n.toUInt(), stream.query(2, n.toUInt() / 2U + 1U))
         assertEquals(n.toUInt() - 1U, stream.query(3, n.toUInt() / 2U))
         assertEquals(NONEXISTENT, stream.query(3, n.toUInt() / 2U + 1U))
      }
      
      val stream = Splitstream(1, listOf(Node('S', 1, 2, 3)))
      assertEquals(1U, stream.query(2, 1U))
      assertEquals(NONEXISTENT, stream.query(3, 1U))
   }

   @Test fun test() {
      val stream = Splitstream(21, listOf(
         Node('S', 1, 2, 3),
         Node('M', 4, 2, 6),
         Node('S', 3, 4, 5),
         Node('M', 5, 6, 7),
         Node('S', 7, 8, 9),
         Node('S', 8, 10, 11),
         Node('S', 9, 12, 13),
         Node('M', 13, 10, 15),
         Node('M', 11, 12, 14)
      ))
//      for (i in 1 .. 50) {
//         assertEquals((2*i - 1).toUInt(), stream.query(2, i.toUInt())))
//         assertEquals(NONEXISTENT, stream.query(2, (50 + i).toUInt())))
//         assertEquals((2*i).toUInt(), stream.query(3, i.toUInt())))
//         assertEquals(NONEXISTENT, stream.query(3, (50 + i).toUInt())))
//      }
//      for (i in 1 .. 25) {
//         assertEquals((4*i - 2).toUInt(), stream.query(4, i.toUInt())))
//         assertEquals(NONEXISTENT, stream.query(4, (25 + i).toUInt())))
//         assertEquals((4*i).toUInt(), stream.query(5, i.toUInt())))
//         assertEquals(NONEXISTENT, stream.query(5, (25 + i).toUInt())))
//      }
//      assertEquals(2U, stream.query(6, 1U)))
//      assertEquals(1U, stream.query(6, 2U)))
//      assertEquals(6U, stream.query(6, 3U)))
//      assertEquals(3U, stream.query(6, 4U)))
//      assertEquals(10U, stream.query(6, 5U)))
//      assertEquals(5U, stream.query(6, 6U)))
//      assertEquals(30U, stream.query(6, 15U)))
//      assertEquals(98U, stream.query(6, 49U)))
//      assertEquals(49U, stream.query(6, 50U)))
//      assertEquals(51U, stream.query(6, 51U)))
//      assertEquals(99U, stream.query(6, 75U)))
//      assertEquals(NONEXISTENT, stream.query(6, 76U)))
//      assertEquals(NONEXISTENT, stream.query(6, 77U)))
//
//      assertEquals(4U, stream.query(7, 1U)))
//      assertEquals(2U, stream.query(7, 2U)))
//      assertEquals(8U, stream.query(7, 3U)))
//      assertEquals(1U, stream.query(7, 4U)))
//      assertEquals(12U, stream.query(7, 5U)))
//      assertEquals(6U, stream.query(7, 6U)))
//      assertEquals(16U, stream.query(7, 7U)))
//      assertEquals(3U, stream.query(7, 8U)))
//      assertEquals(NONEXISTENT, stream.query(7, 101U)))
//      assertEquals(NONEXISTENT, stream.query(7, 102U)))
//      for (i in 1 .. 25) {
//         assertEquals(4U*i.toUInt(), stream.query(8, i.toUInt())))
//         assertEquals(stream.query(7, 2U*i.toUInt())), stream.query(9, i.toUInt())))
//         assertEquals(stream.query(7, 2U*(25U + i.toUInt()))), stream.query(9, 25U + i.toUInt())))
//      }
//      for (i in 1 .. 12) {
//         assertEquals(8U*i.toUInt() - 4U, stream.query(10, i.toUInt())))
//         assertEquals(8U*i.toUInt(), stream.query(11, i.toUInt())))
//      }

      val answers7 = listOf(4, 2, 8, 1, 12, 6, 16, 3, 20, 10, 5, 14, 7, 18, 9, 11, 13, 15, 17, 19, 21)
      answers7.forEachIndexed { index, answer ->
         assertEquals(answer.toUInt(), stream.query(7, index.toUInt() + 1U))
      }
      val answers8 = listOf(4, 8, 12, 16, 20, 5, 7, 9, 13, 17, 21)
      answers8.forEachIndexed { index, answer ->
         assertEquals(answer.toUInt(), stream.query(8, index.toUInt() + 1U))
      }
      val answers9 = listOf(2, 1, 6, 3, 10, 14, 18, 11, 15, 19)
      answers9.forEachIndexed { index, answer ->
         assertEquals(answer.toUInt(), stream.query(9, index.toUInt() + 1U))
      }
      val answers10 = listOf(4, 12, 20, 7, 13, 21)
      answers10.forEachIndexed { index, answer ->
         assertEquals(answer.toUInt(), stream.query(10, index.toUInt() + 1U))
      }
      val answers11 = listOf(8, 16, 5, 9, 17)
      answers11.forEachIndexed { index, answer ->
         assertEquals(answer.toUInt(), stream.query(11, index.toUInt() + 1U))
      }
      val answers12 = listOf(2, 6, 10, 18, 15)
      answers12.forEachIndexed { index, answer ->
         assertEquals(answer.toUInt(), stream.query(12, index.toUInt() + 1U))
      }
      val answers13 = listOf(1, 3, 14, 11, 19)
      answers13.forEachIndexed { index, answer ->
         assertEquals(answer.toUInt(), stream.query(13, index.toUInt() + 1U))
      }
      assertEquals(21U, stream.query(7, 21U))
      assertEquals(NONEXISTENT, stream.query(8, 12U))
      assertEquals(NONEXISTENT, stream.query(9, 11U))
      assertEquals(NONEXISTENT, stream.query(10, 7U))
      assertEquals(NONEXISTENT, stream.query(11, 6U))

      val answers14 = listOf(8, 2, 16, 6, 5, 10, 9, 18, 17, 15)
      answers14.forEachIndexed { index, answer ->
         assertEquals(answer.toUInt(), stream.query(14, index.toUInt() + 1U))
      }
      val answers15 = listOf(1, 4, 3, 12, 14, 20, 11, 7, 19, 13, 21)
      answers15.forEachIndexed { index, answer ->
         assertEquals(answer.toUInt(), stream.query(15, index.toUInt() + 1U))
      }

      assertEquals(NONEXISTENT, stream.query(14, 11U))
      assertEquals(NONEXISTENT, stream.query(15, 12U))
   }

   @Test fun testDeepSplit() {
      // Have to test left-child splits, as they have large frequencies > 2^32, but still have one output.
      val splits = (1 .. 50).map { i -> Node('S', 2*i - 1, 2*i + 1, 2*i) }
      val stream = Splitstream(1000000000, splits)
      for (line in 79 downTo 65 step 2) {
         assertEquals(1U, stream.query(line, 1U), "line $line")
      }
      // test the 32nd split which is line 63 in, 64 and 65 out. The first output line, 64, should have 2 ^ 32 - 1
   }

   @Test fun testPerformance() {

   }
}