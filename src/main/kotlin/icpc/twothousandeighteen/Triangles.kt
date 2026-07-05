package icpc.twothousandeighteen

import util.*
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.min

class SlashIndexConverter(numRows: Int) {
   val backslashAdj = numRows - 1 + if (numRows.and(1) == 1) 1 else 0

   inline fun forwardSlashIndex(row: Int, column: Int): Int = column + (row + 1)/2
   inline fun backslashIndex(row: Int, column: Int): Int = column + (backslashAdj - row)/2
}

private fun compare(range: IntRange, int: Int): Int {
   if (int < range.first)
      return 1
   if (int > range.last)
      return -1
   return 0
}

abstract class StaggeredVertexGrid(val numRows: Int, val verticesPerRow: Int) {
   abstract fun horizontalSegmentFrom(row: Int, startColumn: Int): Boolean
   abstract fun backslashSegmentBelow(row: Int, column: Int): Boolean
   abstract fun forwardSlashSegmentBelow(row: Int, column: Int): Boolean
}

class TextStaggeredVertexGrid(private val text: Array<DefaultString>, numRows: Int)
   : StaggeredVertexGrid(numRows, 1 + text[0].lastIndexOf('x')/4)
{
   override fun horizontalSegmentFrom(row: Int, startColumn: Int): Boolean {
      val shift = if (row.and(1) == 0) 2 else 4
      return text[2*row][4*startColumn + shift] == '-'
   }

   override fun backslashSegmentBelow(row: Int, column: Int): Boolean {
      val shift = if (row.and(1) == 0) 1 else 3
      return text[2*row + 1][4*column + shift] == '\\'
   }

   override fun forwardSlashSegmentBelow(row: Int, column: Int): Boolean {
      val shift = if (row.and(1) == 0) -1 else 1
      return text[2*row + 1][4*column + shift] == '/'
   }
}

private class SegmentInfoBuilder(baseOffset: Int, val outList: MutableList<IntRange>) {
   private var cur = baseOffset
   private var start = cur
   private var isOn = false

   inline fun next(segmentIsOn: Boolean) {
      if (segmentIsOn)
         on()
      else
         off()
   }

   fun on() {
      if (!isOn) {
         start = cur
         isOn = true
      }
      cur += 1
   }

   fun off() {
      finish()
      cur += 1
      isOn = false
   }

   fun finish() {
      if (isOn) {
         outList.add(IntRange(start, cur - 1))
         isOn = false
      }
   }
}

fun buildHorizontalSegmentsInfo(grid: StaggeredVertexGrid): Array<out List<IntRange>> {
   val rowSegmentsInfo = Array<MutableList<IntRange>>(grid.numRows) { ArrayList() }
   for (row in 0 ..< grid.numRows) {
      val rowSegments = rowSegmentsInfo[row]
      val builder = SegmentInfoBuilder(0, rowSegments)
      for (offset in 0 .. grid.verticesPerRow - 2) {
         builder.next(grid.horizontalSegmentFrom(row, offset))
      }
      builder.finish()
   }
   return rowSegmentsInfo
}

fun buildBackslashSegmentsInfo(grid: StaggeredVertexGrid, converter: SlashIndexConverter): Array<List<IntRange>> {
   // Get the highest index of backslash line from the converter
   val lastColumn = grid.verticesPerRow - 1
   val backslashSegmentsInfo = Array(1 + converter.backslashIndex(0, lastColumn)) {
      mutableListOf<IntRange>()
   }
   // we'll build the ones that hit the 0th row first
   var baseColumn = 0
   for (i in converter.backslashIndex(0, 0) .. converter.backslashIndex(0, lastColumn)) {
      val segments = backslashSegmentsInfo[i]
      val builder = SegmentInfoBuilder(0, segments)
      var row = 0
      var column = baseColumn
      var rowEven = false
      while (row < grid.numRows - 1 && column < grid.verticesPerRow) {
         builder.next(grid.backslashSegmentBelow(row, column))
         row += 1
         if (rowEven)
            column += 1
         rowEven = !rowEven
      }
      builder.finish()
      baseColumn += 1
   }

   // build the ones that don't hit 0th row
   var baseRow = 2
   for (i in converter.backslashIndex(0, 0) - 1 downTo 0) {
      val segments = backslashSegmentsInfo[i]
      val builder = SegmentInfoBuilder(baseRow, segments)
      var row = baseRow
      var column = 0
      var rowEven = false
      while (row < grid.numRows - 1 && column < grid.verticesPerRow) {
         builder.next(grid.backslashSegmentBelow(row, column))
         row += 1
         if (rowEven)
            column += 1
         rowEven = !rowEven
      }
      builder.finish()
      baseRow += 2
   }
   @Suppress("UNCHECKED_CAST")
   return backslashSegmentsInfo as Array<List<IntRange>>
}

fun buildForwardSlashSegmentsInfo(grid: StaggeredVertexGrid, converter: SlashIndexConverter): Array<List<IntRange>> {
   val lastColumn = grid.verticesPerRow - 1
   val forwardSlashSegmentsInfo = Array(1 + converter.forwardSlashIndex(grid.numRows - 1, lastColumn)) {
      mutableListOf<IntRange>()
   }
   for (i in 1 .. converter.forwardSlashIndex(grid.numRows - 2, lastColumn)) {
      val segments = forwardSlashSegmentsInfo[i]
      var row = 0
      if (i > lastColumn)
         row = 2*(i - lastColumn) - 1
      val builder = SegmentInfoBuilder(row, segments)
      var column = min(i, lastColumn)
      var rowOdd = row.and(1) == 0
      while (row < grid.numRows - 1) {
         if (rowOdd && column == 0)
            break
         builder.next(grid.forwardSlashSegmentBelow(row, column))
         row += 1
         if (rowOdd)
            column -= 1
         rowOdd = !rowOdd
      }
      builder.finish()
   }
   @Suppress("UNCHECKED_CAST")
   return forwardSlashSegmentsInfo as Array<List<IntRange>>
}

fun List<IntRange>.rangeContaining(int: Int): IntRange? {
   val index = this.binarySearch { segment ->
      compare(segment, int)
   }
   return if (index >= 0) this[index] else null
}

fun triangles(
   rowSegmentsInfo: Array<out List<IntRange>>,
   forwardSlashSegmentsInfo: Array<out List<IntRange>>,
   backslashSegmentsInfo: Array<out List<IntRange>>,
): Long {
   val converter = SlashIndexConverter(rowSegmentsInfo.size)
   val lastRow = rowSegmentsInfo.lastIndex
   var triangleCount = 0L
   var row = 0
   while (row < rowSegmentsInfo.size) {
      val rowSegments = rowSegmentsInfo[row]
      for (segmentInterval in rowSegments) {
         val backslashSegmentUpCache = Array<IntRange?>(segmentInterval.width) { i ->
            val backslashSegments = backslashSegmentsInfo[converter.backslashIndex(row, segmentInterval.first + i + 1)]
            backslashSegments.rangeContaining(row - 1)
         }
         val forwardSlashSegmentDownCache = Array<IntRange?>(segmentInterval.width) { i ->
            val forwardSlashSegments = forwardSlashSegmentsInfo[converter.forwardSlashIndex(row, segmentInterval.first + i + 1)]
            forwardSlashSegments.rangeContaining(row)
         }

         for (start in segmentInterval) {
            // the line segment along the row starts at start and has length = end - start + 1
            val forwardSlashSegmentsUp = forwardSlashSegmentsInfo[converter.forwardSlashIndex(row, start)]
            val forwardSlashSegmentUp = forwardSlashSegmentsUp.rangeContaining(row - 1)
//            println("row=$row start=$start forwardSlashSegmentUp=$forwardSlashSegmentUp")

            // pointing up triangles: tip is above this row
            // min(..) to clip tip row to bounds, i.e. row 0
            for (length in 1 .. min(row, segmentInterval.last + 1 - start)) {
               val tipRow = row - length
               val backslashSegment = backslashSegmentUpCache[start + length - segmentInterval.first - 1]
//               println("row=$row start=$start length=$length UP")
               if (forwardSlashSegmentUp?.contains(tipRow..< row) == true && backslashSegment?.contains(tipRow..< row) == true) {
//                  println("Up triangle at row=$row, start=$start, size=$length")
                  triangleCount += 1
               }
            }

            // pointing down triangles: tip is below this row
            // min(..) to clip tip row to bounds, i.e. last row
            val backslashSegmentsDown = backslashSegmentsInfo[converter.backslashIndex(row, start)]
            val backslashSegmentDown = backslashSegmentsDown.rangeContaining(row)
//            println("row=$row start=$start backslashSegmentDown=$backslashSegmentDown")

            for (length in 1 .. min(lastRow - row, segmentInterval.last + 1 - start)) {
               val tipRow = row + length
               val forwardSlashSegment = forwardSlashSegmentDownCache[start + length - segmentInterval.first - 1]
//               println("row=$row start=$start length=$length DOWN backslashSegmentDown=$backslashSegmentDown forwardSlashSegment=$forwardSlashSegment")

               if (forwardSlashSegment?.contains(row ..< tipRow) == true && backslashSegmentDown?.contains(row ..< tipRow) == true) {
//                  println("Down triangle at row=$row, start=$start, size=$length")
                  triangleCount += 1
               }
            }
         }
      }
      row += 1
   }
   return triangleCount
}

fun triangles(grid: StaggeredVertexGrid): Long {
   val converter = SlashIndexConverter(grid.numRows)
   val rowSegmentsInfo = buildHorizontalSegmentsInfo(grid)
   val backslashSegmentsInfo = buildBackslashSegmentsInfo(grid, converter)
   val forwardSlashSegmentsInfo = buildForwardSlashSegmentsInfo(grid, converter)
   return triangles(rowSegmentsInfo, forwardSlashSegmentsInfo, backslashSegmentsInfo)
}

fun trianglesIO(inputStream: InputStream, outputStream: OutputStream) {
   val count: Long
   inputStream.bufferedReader().use { reader ->
      val nums = reader.readLine().split(" ")
      val rows = nums[0].toInt()
      val text = Array(2*rows - 1) { _ ->
         DefaultString(reader.readLine())
      }
      val grid = TextStaggeredVertexGrid(text, rows)
      count = triangles(grid)
   }
   outputStream.writer().use { it.write(count.toString()) }
}

fun main() {
   trianglesIO(System.`in`, System.out)
}