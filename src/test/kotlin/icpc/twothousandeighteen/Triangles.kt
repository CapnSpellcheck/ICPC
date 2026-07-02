package icpc.twothousandeighteen

import util.IntervalTreeMap
import util.addAll
import kotlin.test.Test
import kotlin.test.assertEquals

class TrianglesTest {
   @Test fun testSample1Preprocessed() {
      val horizontalSegmentsInfo = arrayOf(
         listOf(IntRange(1, 2),), // row 0
         listOf(IntRange(1, 1),), // row 1
         listOf(IntRange(1, 3),), // row 2
         listOf(IntRange(0, 3),), // row 3
      )

      val forwardSlashSegmentsInfo = Array<IntervalTreeMap<Unit>>(5) { IntervalTreeMap() }
      forwardSlashSegmentsInfo[1].addAll(listOf(IntRange(0, 2)))
      forwardSlashSegmentsInfo[2].addAll(listOf(IntRange(0, 2)))
      forwardSlashSegmentsInfo[4].addAll(listOf(IntRange(2, 2)))

      val backslashSegmentsInfo = Array<IntervalTreeMap<Unit>>(6) { IntervalTreeMap() }
      backslashSegmentsInfo[2].addAll(listOf(IntRange(0, 2)))
      backslashSegmentsInfo[3].addAll(listOf(IntRange(1, 2)))
      backslashSegmentsInfo[4].addAll(listOf(IntRange(0, 2)))

      val result = triangles(horizontalSegmentsInfo, forwardSlashSegmentsInfo, backslashSegmentsInfo)
      assertEquals(12U, result)
   }

}