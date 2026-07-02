package icpc.twothousandeighteen

import util.IntervalTreeMap
import util.gte
import util.insert
import kotlin.math.min

class SlashIndexConverter(numRows: Int) {
   val lastRow = numRows - 1

   /**
    * The 0th column of the 0th row has no forward slash line.
    */
   inline fun forwardSlashIndex(row: Int, column: Int): Int = if (row == 0) column - 1 else column + (row - 1)/2

   /**
    * Interestingly, the 0th column of the last row may have a backslash line, if it's an odd row.
    */
   inline fun backslashIndex(row: Int, column: Int): Int = column + (lastRow - row)/2
}

fun triangles(
   rowSegmentsInfo: Array<List<IntRange>>,
   forwardSlashSegmentsInfo: Array<IntervalTreeMap<Unit>>,
   backslashSegmentsInfo: Array<IntervalTreeMap<Unit>>,
): UInt {
   val converter = SlashIndexConverter(rowSegmentsInfo.size)
   val lastRow = rowSegmentsInfo.lastIndex
   var triangleCount = 0U
   var row = 0
   while (row < rowSegmentsInfo.size) {
      val rowSegments = rowSegmentsInfo[row]
      for (segmentInterval in rowSegments) {
         for (start in segmentInterval) {
            // the line segment along the row starts at start and has length = end - start + 1
            val forwardSlashSegmentsUp = forwardSlashSegmentsInfo[converter.forwardSlashIndex(row, start)]
            val backslashSegmentsDown = backslashSegmentsInfo[converter.backslashIndex(row, start)]

            // pointing up triangles: tip is above this row
            // min(..) to clip tip row to bounds, i.e. row 0
            for (length in 1 .. min(row, segmentInterval.last + 1 - start)) {
               val tipRow = row - length
               val backslashSegmentsUp = backslashSegmentsInfo[converter.backslashIndex(row, start + length)]
               val forwardSlashSegment = forwardSlashSegmentsUp.getContaining(tipRow)
               val backslashSegment = backslashSegmentsUp.getContaining(tipRow)
               if (forwardSlashSegment?.last.gte(row - 1) && backslashSegment?.last.gte(row - 1)) {
                  println("Up triangle at row=$row, start=$start, size=$length")
                  triangleCount += 1U
               }
            }

            // pointing down triangles: tip is below this row
            // min(..) to clip tip row to bounds, i.e. last row
            for (length in 1 .. min(lastRow - row, segmentInterval.last + 1 - start)) {
               val tipRowMinus1 = row + length - 1
               val forwardSlashSegmentsDown = forwardSlashSegmentsInfo[converter.forwardSlashIndex(row, start + length)]
               val forwardSlashSegment = forwardSlashSegmentsDown.getContaining(row)
               val backslashSegment = backslashSegmentsDown.getContaining(row)
               if (forwardSlashSegment?.last.gte(tipRowMinus1) && backslashSegment?.last.gte(tipRowMinus1)){
                  println("Down triangle at row=$row, start=$start, size=$length")
                  triangleCount += 1U
               }
            }
         }
      }
      row += 1
   }
   return triangleCount
}
